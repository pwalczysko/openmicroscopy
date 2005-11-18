/*
 * org.openmicroscopy.shoola.agents.hiviewer.util.TreeCellRenderer
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

package org.openmicroscopy.shoola.agents.hiviewer.util;


//Java imports
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.hiviewer.IconManager;
import org.openmicroscopy.shoola.agents.hiviewer.browser.ImageNode;
import org.openmicroscopy.shoola.agents.hiviewer.browser.ImageSet;
import pojos.CategoryData;
import pojos.CategoryGroupData;
import pojos.DatasetData;
import pojos.ImageData;
import pojos.ProjectData;

/** 
 * Determines and sets the icon and text associated to a data object.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class TreeCellRenderer
    extends DefaultTreeCellRenderer
{
    
    /** Reference to the {@link IconManager}. */
    private IconManager         icons;
    
    /**
     * Sets the icon and the text corresponding to the user's object.
     * If an icon is passed, the passed icon is set
     * 
     * @param usrObject The user's object.
     * @param icon  If <code>null</code>, a default icon is set according to the
     *              data type of the user's object.
     */
    private void setValues(Object usrObject, Icon icon)
    {
        if (usrObject instanceof ProjectData)  {
            setText(((ProjectData) usrObject).getName());
            if (icon == null) icon = icons.getIcon(IconManager.PROJECT);
            setIcon(icon);
        } else if (usrObject instanceof DatasetData) {
            setText(((DatasetData) usrObject).getName());
            if (icon == null) icon = icons.getIcon(IconManager.DATASET);
            setIcon(icon);
        } else if (usrObject instanceof ImageData) {
            setText(((ImageData) usrObject).getName());
            if (icon == null) icon = icons.getIcon(IconManager.IMAGE);
            setIcon(icon);
        } else if (usrObject instanceof CategoryGroupData) {
            setText(((CategoryGroupData) usrObject).getName());
            if (icon == null) icon = icons.getIcon(IconManager.CATEGORY_GROUP);
            setIcon(icon);
        } else if (usrObject instanceof CategoryData) {
            setText(((CategoryData) usrObject).getName());
            if (icon == null) icon = icons.getIcon(IconManager.CATEGORY);
            setIcon(icon);
        } else if (usrObject instanceof String) setIcon(null);
    }

    /** Creates a new instance. */
    public TreeCellRenderer()
    {
        icons = IconManager.getInstance();
    }
    
    /**
     * Sets the icon and the text.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                        boolean sel, boolean expanded, boolean leaf,
                        int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, 
                                                row, hasFocus);
        
        DefaultMutableTreeNode  node = (DefaultMutableTreeNode) value;
        if (node.getLevel() == 0) {
            setIcon(icons.getIcon(IconManager.ROOT));
            return this;
        }
        Object usrObject = node.getUserObject();
        if (usrObject instanceof ImageSet) {
            setValues(((ImageSet) usrObject).getHierarchyObject(), null);
        } else if (usrObject instanceof ImageNode) {
            ImageNode imgNode = (ImageNode) usrObject;
            setValues(imgNode.getHierarchyObject(),
                    imgNode.getThumbnail().getIcon());
        } else setValues(usrObject, null);
        return this;
    }
    
}
