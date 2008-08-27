 /*
 * org.openmicroscopy.shoola.agents.editor.browser.Browser 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.agents.editor.browser;

//Java imports

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;

//Third-party libraries

//Application-internal dependencies

import org.openmicroscopy.shoola.util.ui.component.ObservableComponent;


/** 
 * The public interface for the browser component
 *
 * @author  William Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:will@lifesci.dundee.ac.uk">will@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public interface Browser
	extends ObservableComponent
{
	/**
	 * A String that specifies that the UI should be for tree display only,
	 * not for editing. Used by BrowserFactory
	 */
	public static final String TREE_DISPLAY = "treeDisplay";
	
	/**
	 * A String that specifies that the UI should be for tree editing, not
	 * simply display. Used by BrowserFactory
	 */
	public static final String TREE_EDIT = "treeEdit";
	
    /**
     * Sets a new treeModel for the browser. 
     * 
     * @param model		The new treeModel.
     */
    public void setTreeModel(TreeModel model);
    
    /** 
     * Returns the UI component. 
     * 
     * @return See above.
     */
    public JComponent getUI();
   
}
