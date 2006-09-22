/*
 * org.openmicroscopy.shoola.env.data.OmeroServiceImpl
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.data;


//Java imports
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import ome.model.IObject;
import ome.model.containers.Category;
import ome.model.containers.CategoryGroup;
import ome.model.containers.CategoryGroupCategoryLink;
import ome.model.containers.CategoryImageLink;
import ome.model.containers.Dataset;
import ome.model.containers.DatasetImageLink;
import ome.model.containers.Project;
import ome.model.containers.ProjectDatasetLink;
import ome.model.core.Image;
import ome.util.builders.PojoOptions;
import org.openmicroscopy.shoola.env.LookupNames;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.util.ModelMapper;
import org.openmicroscopy.shoola.env.data.util.PojoMapper;
import pojos.AnnotationData;
import pojos.CategoryData;
import pojos.CategoryGroupData;
import pojos.DataObject;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.GroupData;
import pojos.ImageData;
import pojos.ProjectData;

/** 
 * Implementation of the {@link OmeroService} I/F.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
class OmeroServiceImpl
    implements OmeroService
{
    
    /** Uses it to gain access to the container's services. */
    private Registry                context;
    
    /** Reference to the entry point to access the <i>OMERO</i> services. */
    private OMEROGateway            gateway;
    
    /**
     * Helper method to return the user's details.
     * 
     * @return See above.
     */
    private ExperimenterData getUserDetails()
    {
        return (ExperimenterData) context.lookup(
                					LookupNames.CURRENT_USER_DETAILS);
    }
    
    /**
     * Sets the root context of the options depending on the specified
     * level and ID.
     * 
     * @param po		The {@link PojoOptions} to handle.
     * @param rootLevel	The level of the root, either {@link GroupData}, 
     *                  {@link ExperimenterData}.
     * @param rootID	The ID of the root if needed.
     */
    private void setRootOptions(PojoOptions po, Class rootLevel, long rootID)
    {
        if (rootLevel.equals(GroupData.class))
            po.grp(new Long(rootID));
        else if (rootLevel.equals(ExperimenterData.class))
            po.exp(new Long(getUserDetails().getId()));
    }
    
    /**
     * Checks if the specified classification algorithm is supported.
     * 
     * @param algorithm The passed index.
     * @return <code>true</code> if the algorithm is supported.
     */
    private boolean checkAlgorithm(int algorithm)
    {
        switch (algorithm) {
            case OmeroService.DECLASSIFICATION:
            case OmeroService.CLASSIFICATION_ME:
            case OmeroService.CLASSIFICATION_NME:    
                return true;
        }
        return false;
    }
    
    /**
     * Creates a new instance.
     * 
     * @param gateway   Reference to the OMERO entry point.
     *                  Mustn't be <code>null</code>.
     * @param registry  Reference to the registry. Mustn't be <code>null</code>.
     */
    OmeroServiceImpl(OMEROGateway gateway, Registry registry)
    {
        if (registry == null)
            throw new IllegalArgumentException("No registry.");
        if (gateway == null)
            throw new IllegalArgumentException("No gateway.");
        context = registry;
        this.gateway = gateway;
    }
    
    /** 
     * Implemented as specified by {@link OmeroService}. 
     * @see OmeroService#loadContainerHierarchy(Class, Set, boolean, Class, 
     * 												long)
     */
    public Set loadContainerHierarchy(Class rootNodeType, Set rootNodeIDs,
                                    boolean withLeaves, Class rootLevel, 
                                    long rootLevelID)
        throws DSOutOfServiceException, DSAccessException 
    {
        PojoOptions po = new PojoOptions();
        po.allCounts();
        setRootOptions(po, rootLevel, rootLevelID);
        if (!withLeaves) po.noLeaves();
        po.countsFor(new Long(getUserDetails().getId()));
        return gateway.loadContainerHierarchy(rootNodeType, rootNodeIDs,
                                            po.map());                              
    }

    /** 
     * Implemented as specified by {@link OmeroService}. 
     * @see OmeroService#findContainerHierarchy(Class, Set, Class, long)
     */
    public Set findContainerHierarchy(Class rootNodeType, Set leavesIDs, 
                                    Class rootLevel, long rootLevelID)
        throws DSOutOfServiceException, DSAccessException
    {
        PojoOptions po = new PojoOptions();
        setRootOptions(po, rootLevel, rootLevelID);
        po.countsFor(new Long(getUserDetails().getId()));
        return gateway.findContainerHierarchy(rootNodeType, leavesIDs,
                        po.map());
    }

    /** 
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#findAnnotations(Class, Set, Set)
     */
    public Map findAnnotations(Class nodeType, Set nodeIDs, Set annotatorIDs)
        throws DSOutOfServiceException, DSAccessException
    {
        PojoOptions po = new PojoOptions();
        po.noCounts();
        po.noLeaves();
        return gateway.findAnnotations(nodeType, nodeIDs, annotatorIDs, 
                                        po.map());
    }

    /** 
     * Implemented as specified by {@link OmeroService}. 
     * @see OmeroService#findCGCPaths(Set, int)
     */
    public Set findCGCPaths(Set imgIDs, int algorithm)
        throws DSOutOfServiceException, DSAccessException
    {
        if (!checkAlgorithm(algorithm)) 
            throw new IllegalArgumentException("Find CGCPaths algorithm not " +
                    "supported.");
        PojoOptions po = new PojoOptions();
        po.noCounts();
        po.exp(new Long(getUserDetails().getId()));
        return gateway.findCGCPaths(imgIDs, algorithm, po.map());
    }
    
    /** 
     * Implemented as specified by {@link OmeroService}. 
     * @see OmeroService#getImages(Class, Set, Class, long)
     */
    public Set getImages(Class nodeType, Set nodeIDs, Class rootLevel, 
            			long rootLevelID)
        throws DSOutOfServiceException, DSAccessException
    {
        PojoOptions po = new PojoOptions();
        po.allCounts();
        setRootOptions(po, rootLevel, rootLevelID);
        po.countsFor(new Long(getUserDetails().getId()));
        return gateway.getContainerImages(nodeType, nodeIDs, po.map());
    }
    
    /** 
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#getUserImages()
     */
    public Set getUserImages()
        throws DSOutOfServiceException, DSAccessException
    {
        PojoOptions po = new PojoOptions();
        po.noCounts();
        po.exp(new Long(getUserDetails().getId()));
        return gateway.getUserImages(po.map());
    }
  
    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#getCollectionCount(Class, String, Set)
     */
    public Map getCollectionCount(Class rootNodeType, String property, Set 
            						rootNodeIDs)
    	throws DSOutOfServiceException, DSAccessException
    {
        PojoOptions po = new PojoOptions();
        po.noCounts();
        if (!(property.equals(IMAGES_PROPERTY)))
            throw new IllegalArgumentException("Property not supported.");
        Map m = gateway.getCollectionCount(rootNodeType, property, rootNodeIDs, 
                po.map());
        return m;
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#createAnnotationFor(DataObject, AnnotationData)
     */
    public DataObject createAnnotationFor(DataObject annotatedObject,
                                        AnnotationData data)
            throws DSOutOfServiceException, DSAccessException
    {
        if (data == null) 
            throw new IllegalArgumentException("No annotation to create.");
        if (annotatedObject == null) 
            throw new IllegalArgumentException("No DataObject to annotate."); 
        if (!(annotatedObject instanceof ImageData) && 
                !(annotatedObject instanceof DatasetData))
            throw new IllegalArgumentException("This method only supports " +
                    "ImageData and DatasetData objects.");
        IObject object = gateway.createObject(
                ModelMapper.createAnnotation(annotatedObject.asIObject(), 
                                            data), (new PojoOptions()).map());
        return PojoMapper.asDataObject(ModelMapper.getAnnotatedObject(object));
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#removeAnnotationFrom(DataObject, AnnotationData)
     */
    public DataObject removeAnnotationFrom(DataObject annotatedObject, 
                                            AnnotationData data)
            throws DSOutOfServiceException, DSAccessException
    {
        if (data == null) 
            throw new IllegalArgumentException("No annotation to delete.");
        if (annotatedObject == null) 
            throw new IllegalArgumentException("No annotated DataObject."); 
        if (!(annotatedObject instanceof ImageData) && 
                !(annotatedObject instanceof DatasetData))
            throw new IllegalArgumentException("This method only supports " +
                    "ImageData and DatasetData objects.");
        Map options = (new PojoOptions()).map();
        gateway.deleteObject(data.asIObject(), options);
        return updateDataObject(annotatedObject);
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#updateAnnotationFor(DataObject, AnnotationData)
     */
    public DataObject updateAnnotationFor(DataObject annotatedObject,
                                          AnnotationData data)
            throws DSOutOfServiceException, DSAccessException
    {
        if (data == null) 
            throw new IllegalArgumentException("No annotation to update.");
        if (annotatedObject == null) 
            throw new IllegalArgumentException("No annotated DataObject.");
        if (!(annotatedObject instanceof ImageData) && 
                !(annotatedObject instanceof DatasetData))
            throw new IllegalArgumentException("This method only supports " +
                    "ImageData and DatasetData objects.");
        Map options = (new PojoOptions()).map();
        IObject updated = gateway.updateObject(annotatedObject.asIObject(),
                                                options);
        IObject toUpdate = data.asIObject();
        ModelMapper.setAnnotatedObject(updated, toUpdate);
        gateway.updateObject(toUpdate, options);
        return PojoMapper.asDataObject(updated);
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#createDataObject(DataObject, DataObject)
     */
    public DataObject createDataObject(DataObject child, DataObject parent)
            throws DSOutOfServiceException, DSAccessException
    {
        if (child == null) 
            throw new IllegalArgumentException("The child cannot be null.");
        IObject obj = ModelMapper.createIObject(child, parent);
        if (obj == null) 
            throw new NullPointerException("Cannot convert object.");
        IObject created = gateway.createObject(obj, (new PojoOptions()).map());
        if (parent != null)
            ModelMapper.linkParentToChild(created, parent.asIObject());
        return  PojoMapper.asDataObject(created);
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#removeDataObject(DataObject, DataObject)
     */
    public DataObject removeDataObject(DataObject child, DataObject parent)
            throws DSOutOfServiceException, DSAccessException
    {
        if (child == null) 
            throw new IllegalArgumentException("The child cannot be null.");
        if (!(child instanceof ImageData)) {
            if (parent == null) { //top container
                PojoMapper.asDataObject(gateway.updateObject(child.asIObject(),
                        (new PojoOptions()).map()));
            } else {
                IObject object = ModelMapper.removeIObject(child.asIObject(),
                        parent.asIObject());
                PojoMapper.asDataObject(gateway.updateObject(object,
                        (new PojoOptions()).map()));
            }
        } else {
            IObject p = parent.asIObject();
            Image img = child.asImage();
            IObject link = ModelMapper.unlinkChildFromParent(img, p);
            if (link != null)
                gateway.deleteObject(link, (new PojoOptions()).map());
            IObject[] objects = new IObject[1];
            objects[0] = p;
            //objects[1] = p;
            IObject[] results = gateway.updateObjects(objects,
                                (new PojoOptions()).map());
            for (int i = 0; i < results.length; i++)
                PojoMapper.asDataObject(results[i]);
        }
        
        return child;
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#updateDataObject(DataObject)
     */
    public DataObject updateDataObject(DataObject object)
            throws DSOutOfServiceException, DSAccessException
    {
        if (object == null) 
            throw new DSAccessException("No object to update.");  
        IObject ob = object.asIObject();
        ModelMapper.unloadCollections(ob);;
        IObject updated = gateway.updateObject(ob,
                                        (new PojoOptions()).map());
        return PojoMapper.asDataObject(updated);
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#classify(Set, Set)
     */
    public void classify(Set images, Set categories)
            throws DSOutOfServiceException, DSAccessException
    {
        try {
            images.toArray(new ImageData[] {});
        } catch (ArrayStoreException ase) {
            throw new IllegalArgumentException(
                    "images only contains ImageData elements.");
        }
        try {
            categories.toArray(new CategoryData[] {});
        } catch (ArrayStoreException ase) {
            throw new IllegalArgumentException(
                    "categories only contains CategoryData elements.");
        }
        
        Iterator category = categories.iterator();
        Iterator image;
        List objects = new ArrayList();
        Category mParent;
        CategoryImageLink l;
        while (category.hasNext()) {
            mParent = ((DataObject) category.next()).asCategory();
            image = images.iterator();
            while (image.hasNext()) {
                l = new CategoryImageLink();
                l.link(mParent, ((DataObject) image.next()).asImage());
                objects.add(l);
            }   
        }
        if (objects.size() != 0) {
            Iterator i = objects.iterator();
            IObject[] array = new IObject[objects.size()];
            int index = 0;
            while (i.hasNext()) {
                array[index] = (IObject) i.next();
                index++;
            }
            gateway.createObjects(array, (new PojoOptions()).map());
        }
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#declassify(Set, Set)
     */
    public void declassify(Set images, Set categories)
            throws DSOutOfServiceException, DSAccessException
    {
        try {
            images.toArray(new ImageData[] {});
        } catch (ArrayStoreException ase) {
            throw new IllegalArgumentException(
                    "The images set only contains ImageData elements.");
        }
        try {
            categories.toArray(new CategoryData[] {});
        } catch (ArrayStoreException ase) {
            throw new IllegalArgumentException(
                    "The categories set only contains CategoryData elements.");
        }
        Iterator category = categories.iterator();
        List links = new ArrayList();
        Iterator i;
        IObject mParent;
        IObject link;
        while (category.hasNext()) {
            mParent = ((DataObject) category.next()).asIObject();
            i = images.iterator();
            while (i.hasNext()) {
                link = ModelMapper.unlinkChildFromParent(
                        ((DataObject) i.next()).asImage(), mParent);
                if (link != null)
                    links.add(link);
            }   
        }
        IObject[] objects = new IObject[links.size()];
        i = links.iterator();
        int index = 0;
        while (i.hasNext()) {
            objects[index] = (IObject) i.next();
            index++;
        }
        gateway.deleteObjects(objects);
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#loadExistingObjects(Class, Set, Class, long)
     */
    public Set loadExistingObjects(Class nodeType, Set nodeIDs, Class rootLevel,
                                long rootID)
            throws DSOutOfServiceException, DSAccessException
    {
        Set all = null;
        Set objects = new HashSet();
        if (nodeType.equals(ProjectData.class)) {
            Set in = loadContainerHierarchy(nodeType, nodeIDs, true, rootLevel, 
                                            rootID);
            all = loadContainerHierarchy(DatasetData.class, null, true, 
                                            rootLevel, rootID);
            Iterator i = in.iterator();
            Iterator j; 
            while (i.hasNext()) {
                j = (((ProjectData) i.next()).getDatasets()).iterator();
                while (j.hasNext()) {
                    objects.add(new Long(((DatasetData) j.next()).getId()));
                } 
            }
        } else if (nodeType.equals(CategoryGroupData.class)) {
            Set in = loadContainerHierarchy(nodeType, nodeIDs, true, rootLevel, 
                    rootID);
            all = loadContainerHierarchy(CategoryData.class, null, true, 
                                rootLevel, rootID);
            Iterator i = in.iterator();
            Iterator j; 
            while (i.hasNext()) {
                j = (((CategoryGroupData) i.next()).getCategories()).iterator();
                while (j.hasNext()) {
                    objects.add(new Long(((CategoryData) j.next()).getId()));
                } 
            }
        } else if ((nodeType.equals(DatasetData.class)) || 
                    (nodeType.equals(CategoryData.class))) {
            Set in = getImages(nodeType, nodeIDs, rootLevel, rootID);
            all = getUserImages();
            Iterator i = in.iterator();
            while (i.hasNext()) {
                objects.add(new Long(((ImageData) i.next()).getId()));
            }
            
        }
        if (all == null) return new HashSet(1);
        Iterator k = all.iterator();
        Set toRemove = new HashSet();
        DataObject ho;
        Long id;
        while (k.hasNext()) {
            ho = (DataObject) k.next();
            id = new Long(ho.getId());
            if (objects.contains(id)) toRemove.add(ho);
        }
        all.removeAll(toRemove);
        return all;
        
    }

    /**
     * Implemented as specified by {@link OmeroService}.
     * @see OmeroService#addExistingObjects(DataObject, Set)
     */
    public void addExistingObjects(DataObject parent, Set children)
            throws DSOutOfServiceException, DSAccessException
    {
        if (parent instanceof ProjectData) {
            try {
                children.toArray(new DatasetData[] {});
            } catch (ArrayStoreException ase) {
                throw new IllegalArgumentException(
                        "items can only be datasets.");
            }
        } else if (parent instanceof DatasetData) {
            try {
                children.toArray(new ImageData[] {});
            } catch (ArrayStoreException ase) {
                throw new IllegalArgumentException(
                        "items can only be images.");
            }
        } else if (parent instanceof CategoryGroupData) {
                try {
                    children.toArray(new CategoryData[] {});
                } catch (ArrayStoreException ase) {
                    throw new IllegalArgumentException(
                            "items can only be categories.");
                }
        } else
            throw new IllegalArgumentException("parent object not supported");
        
        List objects = new ArrayList();
        if (parent instanceof ProjectData) {
            Project mParent = parent.asProject();
            ProjectDatasetLink l;
            Iterator child = children.iterator();
            while (child.hasNext()) {
                l = new ProjectDatasetLink();
                l.link(mParent, ((DataObject) child.next()).asDataset());  
                objects.add(l);
            }
        } else if (parent instanceof DatasetData) {
            Dataset mParent = parent.asDataset();
            DatasetImageLink l;
            Iterator child = children.iterator();
            while (child.hasNext()) {
                l = new DatasetImageLink();
                l.link(mParent, ((DataObject) child.next()).asImage());  
                objects.add(l);
            }
        } else if (parent instanceof CategoryGroupData) {
            CategoryGroup mParent = parent.asCategoryGroup();
            CategoryGroupCategoryLink l;
            Iterator child = children.iterator();
            while (child.hasNext()) {
                l = new CategoryGroupCategoryLink();
                l.link(mParent, ((DataObject) child.next()).asCategory());  
                objects.add(l);
            }
        }
        if (objects.size() != 0) {
            Iterator i = objects.iterator();
            IObject[] array = new IObject[objects.size()];
            int index = 0;
            while (i.hasNext()) {
                array[index] = (IObject) i.next();
                index++;
            }
            gateway.createObjects(array, (new PojoOptions()).map());
        } 
    }
    
}
