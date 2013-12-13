/*
 * omeis.providers.re.quantum.QuantumStrategy
 *
 *   Copyright 2006-2013 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package omeis.providers.re.quantum;

// Java imports

// Third-party libraries

// Application-internal dependencies
import ome.model.display.QuantumDef;
import ome.model.enums.Family;
import ome.model.enums.PixelsType;
import omeis.providers.re.data.PlaneFactory;

/**
 * Subclasses Work on explicit pixel types. Taking into
 * account the pixel types, transform the pixel intensity value passed to
 * {@link #quantize} by delegating to the configured quantum map. Encapsulate a
 * computation strategy for the quantization process i.e. LUT and Approximation.
 * Implement {@link #onWindowChange} to get notified when the input interval
 * changes.
 * 
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author <br>
 *         Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:a.falconi@dundee.ac.uk"> a.falconi@dundee.ac.uk</a>
 * @version 2.2 <small> (<b>Internal version:</b> $Revision$ $Date:
 *          2005/06/20 14:12:05 $) </small>
 * @since OME2.2
 */
public abstract class QuantumStrategy {

    /**
     * Maximum value (<code>255</code>) allowed for the upper bound of the
     * codomain interval.
     */
    public static final int MAX = 255;

    /**
     * Minimum value (<code>0</code>) allowed for the lower bound of the
     * codomain interval.
     */
    public static final int MIN = 0;

    /**
     * Determines the number of sub-intervals of the [globalMin, globalMax]
     * interval.
     */
    public static final int DECILE = 10;

    /** The maximum size for a lookup table. */
    static final double MAX_SIZE_LUT = 0x10000;
    
    /** The minimum value for the pixels type. */
    private double pixelsTypeMin;
    
    /** The maximum value for the pixels type. */
    private double pixelsTypeMax;
    
    /** Minimum of all minima. */
    private double globalMin;

    /** Maximum of all maxima. */
    private double globalMax;

    /** The original Minimum of all minima. */
    private double originalGlobalMin;

    /** The original Maximum of all maxima. */
    private double originalGlobalMax;
    
    /** The lower limit of the input Interval i.e. pixel intensity interval. */
    private double windowStart;

    /** The upper limit of the input Interval i.e. pixel intensity interval. */
    private double windowEnd;

    /** Identifies a family of maps. */
    private Family family;

    /** Selects a curve in the family. */
    private double curveCoefficient;

    /**
     * Identifies the noise reduction algorithm, value set to <code>true</code>
     * if the noise reduction algorithm is applied, <code>false</code>
     * otherwise.
     */
    private boolean noiseReduction;

    /** Reference to a quantumDef object. */
    protected final QuantumDef qDef;

    /** The type of pixels this strategy is for. */
    protected final PixelsType type;

    /** Reference to the value mapper. */
    protected QuantumMap valueMapper;

    /**
     * Defines the value mapper corresponding to the specified family.
     * 
     * @param family
     *            The family identifying the value mapper.
     */
    private void defineMapper(Family family) {
        String value = family.getValue();
        verifyFamily(value);
        if (value.equals(QuantumFactory.LINEAR)
                || value.equals(QuantumFactory.POLYNOMIAL)) {
            valueMapper = new PolynomialMap();
        } else if (value.equals(QuantumFactory.LOGARITHMIC)) {
            valueMapper = new LogarithmicMap();
        } else if (value.equals(QuantumFactory.EXPONENTIAL)) {
            valueMapper = new ExponentialMap();
        }
    }

    /**
     * Controls if the specified family is supported. The family must be one of
     * the constant defined in {@link QuantumFactory}.
     * 
     * @param value
     *            The family we're checking for validity.
     */
    private static void verifyFamily(String value) {
        if (!value.equals(QuantumFactory.LINEAR)
                && !value.equals(QuantumFactory.LOGARITHMIC)
                && !value.equals(QuantumFactory.EXPONENTIAL)
                && !value.equals(QuantumFactory.POLYNOMIAL)) {
            throw new IllegalArgumentException("Unsupported family type: '"
                    + value + "'");
        }
    }

    /**
     * Controls if the specified interval is valid depending on the pixel type.
     * 
     * The min value and max value could be out of pixel type range b/c of an
     * error occurred during the calculations of the statistics.
     * 
     * @param min
     *            The lower bound of the interval.
     * @param max
     *            The upper bound of the interval.
     */
    private void verifyInterval(double min, double max) {
        boolean b = false;
        if (min <= max) {
            double range = max - min;
            if (PlaneFactory.in(type,
            		new String[] { PlaneFactory.INT8, PlaneFactory.UINT8 })) {
                if (range < 0x100) {
                    b = true;
                }
            } else if (PlaneFactory
                    .in(type, new String[] { PlaneFactory.INT16,
                    		PlaneFactory.UINT16 })) {
                if (range < 0x10000) {
                    b = true;
                }
            } else if (PlaneFactory
                    .in(type, new String[] { PlaneFactory.INT32,
                            PlaneFactory.UINT32 })) {
                if (range < 0x100000000L) {
                    b = true;
                }
            } else if (PlaneFactory
                    .in(type, new String[] { PlaneFactory.FLOAT_TYPE,
                            PlaneFactory.DOUBLE_TYPE })) {
                b = true;
            }
        }
        if (!b) {
            throw new IllegalArgumentException(
            	"Min: " + min + " Max: " + max + " Interval not supported");
        }
    }

    /** 
     * Initializes the minimum and maximum used to build a LUT depending on the
     * pixels type.
     * 
     * @param withRange Pass <code>true</code> to indicate that the range
     *                  has to be taken into account, <code>false</code>
     *                  otherwise.
     */
    private void initPixelsRange(boolean withRange)
    {
        double range;
        String typeAsString = type.getValue();
        if (PlaneFactory.INT8.equals(typeAsString)) {
            pixelsTypeMin = -128;
            pixelsTypeMax = 127;
        } else if (PlaneFactory.UINT8.equals(typeAsString)) {
            pixelsTypeMin = 0;
            pixelsTypeMax = 255;
        } else if (PlaneFactory.INT16.equals(typeAsString)) {
            pixelsTypeMin = -32768;
            pixelsTypeMax = 32767;
        } else if (PlaneFactory.UINT16.equals(typeAsString)) {
            pixelsTypeMin = 0;
            pixelsTypeMax = 65535;
        } else if (PlaneFactory.INT32.equals(typeAsString)) {
            if (withRange) {
                range = globalMax - globalMin;
                if (range < 0x10000) { 
                    pixelsTypeMin = -32768;
                    pixelsTypeMax = 32767;
                }
            } else {
                pixelsTypeMin = -32768;
                pixelsTypeMax = 32767;
            }
        } else if (PlaneFactory.UINT32.equals(typeAsString)) {
            if (withRange) {
                range = globalMax - globalMin;
                if (range < 0x10000) { 
                    pixelsTypeMin = 0;
                    pixelsTypeMax = 65535;
                }
            } else {
                pixelsTypeMin = 0;
                pixelsTypeMax = 65535;
            }

        } else if (PlaneFactory.FLOAT_TYPE.equals(typeAsString) ||
                PlaneFactory.DOUBLE_TYPE.equals(typeAsString)) {
            if (withRange) {
                range = globalMax - globalMin;
                if (range < 0x10000 && globalMin > -1) { 
                    pixelsTypeMin = 0;
                    pixelsTypeMax = 65535;
                }
                if (range < 0x10000 && globalMin < 0) { 
                    pixelsTypeMin = -32768;
                    pixelsTypeMax = 32767;
                }
            } else {
                //b/c we don't know if it is signed or not
                pixelsTypeMin = 0;
                pixelsTypeMax = 32767;
            }
        }
    }

    /**
     * Creates a new instance.
     * 
     * @param qd The {@link QuantumDef} this strategy is for.
     * @param pt The pixels type to handle.
     */
    protected QuantumStrategy(QuantumDef qd, PixelsType pt)
    {
        windowStart = globalMin = 0.0;
        windowEnd = globalMax = 1.0;
        curveCoefficient = 1.0;
        pixelsTypeMax = 0.0;
        pixelsTypeMin = 0.0;
        if (qd == null) {
            throw new NullPointerException("No quantum definition");
        }
        this.qDef = qd;
        if (pt == null) {
            throw new NullPointerException("No pixel type");
        }
        this.type = pt;
        initPixelsRange(false);
    }

    /**
     * Sets the maximum range of the input window.
     * 
     * @param globalMin
     *            The minimum of all minima for a specified stack.
     * @param globalMax
     *            The maximum of all maxima for a specified stack.
     */
    public void setExtent(double globalMin, double globalMax)
    {
        originalGlobalMin = globalMin;
        originalGlobalMax = globalMax;
        initPixelsRange(false);
        if (Double.isInfinite(globalMax) || globalMax > pixelsTypeMax)
            globalMax = pixelsTypeMax;
        if (Double.isInfinite(globalMin) || globalMin < pixelsTypeMin)
            globalMin = pixelsTypeMin;
        verifyInterval(globalMin, globalMax);
        this.globalMin = globalMin;
        this.globalMax = globalMax;
        this.windowStart = globalMin;
        this.windowEnd = globalMax;
        initPixelsRange(true);
    }

    /**
     * Sets the input window interval.
     * 
     * @param start
     *            The lower bound of the interval.
     * @param end
     *            The upper bound of the interval.
     */
    public void setWindow(double start, double end) {
        verifyInterval(start, end);
        if (start < globalMin) start = globalMin;
        if (end < globalMax) end = globalMax;
        windowStart = start;
        windowEnd = end;
        onWindowChange();
    }

    /**
     * Sets the selected family, the curve coefficient and the noise reduction
     * flag.
     * 
     * @param family
     *            The mapping family.
     * @param k
     *            The curve coefficient.
     * @param noiseReduction
     *            The noise reduction flag.
     */
    public void setMapping(Family family, double k, boolean noiseReduction) {
        defineMapper(family);
        this.family = family;
        curveCoefficient = k;
        this.noiseReduction = noiseReduction;
    }

    /**
     * Sets the selected family, the curve coefficient and the noise reduction
     * flag and rebuilds the look-up table.
     * 
     * @param family
     *            The mapping family.
     * @param k
     *            The curve coefficient.
     * @param noiseReduction
     *            The noise reduction flag.
     */
    public void setQuantizationMap(Family family, double k,
            boolean noiseReduction) {
        setMapping(family, k, noiseReduction);
        onWindowChange();
    }

    /**
     * Sets the quantum map.
     * 
     * @param qMap The value to set.
     */
    public void setMap(QuantumMap qMap) {
        valueMapper = qMap;
    }

    /**
     * Returns the mapping family.
     * 
     * @return See above.
     */
    public Family getFamily() {
        return family;
    }

    /**
     * Returns the coefficient identifying a curve within a given family.
     * 
     * @return See above.
     */
    public double getCurveCoefficient() {
        return curveCoefficient;
    }

    /**
     * Returns <code>true</code> if the noise reduction algorithm 
     * is turned on, <code>false</code> if turned off.
     * 
     * @return See above.
     */
    public boolean getNoiseReduction() { return noiseReduction; }

    /**
     * Returns the minimum of all minima.
     * 
     * @return See above.
     */
    public double getGlobalMin() {
        // needed b/c of float value
        double d = globalMin - Math.floor(globalMin);
        if (d != 0) {
            globalMin = Math.floor(globalMin);
        }
        return globalMin;
    }

    /**
     * Returns the maximum of all maxima.
     * 
     * @return See above.
     */
    public double getGlobalMax() { return globalMax; }

    /**
     * Returns the original minimum of all minima.
     * 
     * @return See above.
     */
    public double getOriginalGlobalMin() { return originalGlobalMin; }

    /**
     * Returns the original maximum of all minima.
     * 
     * @return See above.
     */
    public double getOriginalGlobalMax() { return originalGlobalMax; }

    /**
     * Returns the lower bound of the pixels range or <code>0</code>
     * if the value couldn't be set.
     * 
     * @return See above.
     */
    public double getPixelsTypeMin() { return pixelsTypeMin; }
    
    /**
     * Returns the upper bound of the pixels range or <code>0</code>
     * if the value couldn't be set.
     * 
     * @return See above.
     */
    public double getPixelsTypeMax() { return pixelsTypeMax; }

    /**
     * Returns the lower bound of the input interval.
     * 
     * @return See above.
     */
    public double getWindowStart() { return windowStart; }

    /**
     * Returns the upper bound of the input interval.
     * 
     * @return See above.
     */
    public double getWindowEnd() { return windowEnd; }

    /**
     * Notifies when the input interval has changed or the mapping strategy has
     * changed.
     */
    protected abstract void onWindowChange();

    /**
     * Maps a value from [windowStart, windowEnd] to a value in the codomain
     * interval.
     * 
     * @param value
     *            The pixel intensity value.
     * @return The value in the codomain interval i.e. sub-interval of [0,
     *         255].
     * @throws QuantizationException
     *             If the specified value is not in the interval [globalMin,
     *             globalMax].
     */
    public abstract int quantize(double value) throws QuantizationException;

}
