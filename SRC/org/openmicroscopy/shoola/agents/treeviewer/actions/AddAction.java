/*
 * org.openmicroscopy.shoola.agents.treeviewer.actions.AddAction
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.treeviewer.actions;


//Java imports
import java.awt.event.ActionEvent;
import javax.swing.Action;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.treeviewer.IconManager;
import org.openmicroscopy.shoola.agents.treeviewer.browser.Browser;
import org.openmicroscopy.shoola.agents.treeviewer.browser.TreeImageDisplay;
import org.openmicroscopy.shoola.agents.treeviewer.cmd.ClassifyCmd;
import org.openmicroscopy.shoola.agents.treeviewer.view.TreeViewer;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.CategoryData;
import pojos.CategoryGroupData;
import pojos.DataObject;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.ProjectData;

/** 
 *  Adds existing objects to the selected <code>DataObject</code>.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Rev$ $LastChangedDate$)
 * </small>
 * @since OME2.2
 */
public class AddAction
    extends TreeViewerAction
{

    /** The default name of the action. */
    private static final String NAME = "...";
    
    /** The name of the action to add existing <code>Datasets</code>. */
    private static final String NAME_DATASET = "Add existing Dataset...";
    
    /** The name of the action to add existing <code>Categories</code>. */
    private static final String NAME_CATEGORY = "Add existing Category...";
    
    /** The name of the action to add existing <code>Images</code>. */
    private static final String NAME_IMAGE = "Add existing Image...";
    
    /** Description of the action. */
    private static final String DESCRIPTION = "Add existing elements to the " +
                                                "selected container.";
    
    /**
     * Modifies the name of the action and sets it enabled depending on
     * the selected type.
     * @see TreeViewerAction#onDisplayChange(TreeImageDisplay)
     */
    protected void onDisplayChange(TreeImageDisplay selectedDisplay)
    {
        if (selectedDisplay == null) {
            setEnabled(false);
            putValue(Action.NAME, NAME); 
            return;
        }
        Object ho = selectedDisplay.getUserObject();
        if (ho instanceof String || ho instanceof ExperimenterData) { // root
            setEnabled(false);
            putValue(Action.NAME, NAME); 
        } else if (ho instanceof ProjectData) {
            setEnabled(model.isObjectWritable((DataObject) ho));
            putValue(Action.NAME, NAME_DATASET); 
        } else if (ho instanceof CategoryGroupData) {
            setEnabled(model.isObjectWritable((DataObject) ho));
            putValue(Action.NAME, NAME_CATEGORY);
        } else if (ho instanceof CategoryData) {
            setEnabled(model.isObjectWritable((DataObject) ho));
            putValue(Action.NAME, NAME_IMAGE);
        } else if (ho instanceof DatasetData) {
            setEnabled(model.isObjectWritable((DataObject) ho));
            putValue(Action.NAME, NAME_IMAGE);
        } else {
            setEnabled(false);
            putValue(Action.NAME, NAME);
        }
        name = (String) getValue(Action.NAME);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param model Reference to the Model. Mustn't be <code>null</code>.
     */
    public AddAction(TreeViewer model)
    {
        super(model);
        putValue(Action.NAME, NAME);
        name = (String) getValue(Action.NAME);
        putValue(Action.SHORT_DESCRIPTION, 
                UIUtilities.formatToolTipText(DESCRIPTION));
        IconManager im = IconManager.getInstance();
        putValue(Action.SMALL_ICON, im.getIcon(IconManager.ADD_EXISTING));
    }

    /**
     * Adds existing items to the currently selected node.
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Browser b = model.getSelectedBrowser();
        if (b == null) return;
        TreeImageDisplay d = b.getLastSelectedDisplay();
        if (d == null) return;
        Object ho = d.getUserObject();
        if ((ho instanceof ProjectData) || (ho instanceof CategoryGroupData) ||
            (ho instanceof DatasetData))
            model.addExistingObjects((DataObject) ho);
        else if (ho instanceof CategoryData) {
            ClassifyCmd cmd = new ClassifyCmd(model, ClassifyCmd.CLASSIFY);
            cmd.execute();
        }
    }
    
}
