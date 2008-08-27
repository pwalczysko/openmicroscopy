 /*
 * org.openmicroscopy.shoola.agents.editor.browser.NavTree 
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

//Third-party libraries

//Application-internal dependencies

/** 
 * This class extends JTree and provides an outline of the Tree (text only).
 * This is used to navigate the tree and select nodes to be displayed in
 * another JTree (main window). 
 * Nodes are selected by double-clicking nodes of the navTree.
 * Conversely, the selection path of the navTree mimics that of the mainTree,
 * using a TreeSelectionListener on the main Tree. 
 * 
 * This class is build using the mainTree as a source for its treeModel. 
 *
 * @author  William Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:will@lifesci.dundee.ac.uk">will@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class NavTree 
	extends JTree
{
	
	/**
	 * This Navigator tree is used to navigate the main Tree.
	 * mainTree is the main UI display of the Tree. 
	 */
	private JTree mainTree;
	
	/**
	 * Creates an instance of the Navigation tree, based on the model 
	 * from mainTree. Also calls initialise() to add treeSelectionListeners and
	 * mouseListeners. 
	 * 
	 * @param mainTree
	 */
	public NavTree(JTree mainTree) 
	{
		this.mainTree = mainTree;
		
		initialise();
	}
	
	/**
	 * Called by constructor. 
	 * Sets the CellRenderer, SelectionModel and adds appropriate listeners
	 * to the NavTree and the main display Tree. 
	 */
	private void initialise() 
	{
		setCellRenderer(new TreeOutlineCellRenderer());
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        addMouseListener(new DoubleClickListener());
        mainTree.addTreeSelectionListener(new NavSelectionListener());
	}
	
	/**
	 * A mouse listener that responds to double-clicks on the NavTree.
	 * 
	 * If a mouseClicked event comes from the NavTree and clickCount == 2,
	 * the selection path of the navTree is applied to the main display Tree.
	 * 
	 * @author will
	 *
	 */
	private class DoubleClickListener implements MouseListener 
	{
		public void mouseClicked(MouseEvent e) 
		{
			JTree navTree = NavTree.this;
			
			if (e.getSource().equals(navTree)) 
			{
				if (e.getClickCount() == 2) 
				{
					TreePath path = navTree.getSelectionPath();
					
					mainTree.expandPath(path.getParentPath());
					mainTree.setSelectionPath(path);
					mainTree.scrollPathToVisible(path);
				}
			}
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}
	
	/**
	 * A treeSelectionListener, added to the main display tree.
	 * Every selection of the main Tree is mimicked by the NavTree,
	 * 
	 * @author will
	 *
	 */
	private class NavSelectionListener implements TreeSelectionListener 
	{
		public void valueChanged(TreeSelectionEvent e) 
		{
			if (e.getSource().equals(mainTree)) 
			{
				if (mainTree.getSelectionCount() == 0) return;
				
				TreePath selPath = mainTree.getSelectionPath();
				
				/* make sure the node is visible (expand parent) */
				NavTree.this.expandPath(selPath.getParentPath());
				NavTree.this.setSelectionPath(selPath);
				NavTree.this.scrollPathToVisible(selPath);
			}
		}
	}
}
