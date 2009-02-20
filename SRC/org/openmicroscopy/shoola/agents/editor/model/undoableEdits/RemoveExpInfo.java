 /*
 * org.openmicroscopy.shoola.agents.editor.model.undoableEdits.RemoveExpInfo 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2009 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.agents.editor.model.undoableEdits;

//Java imports

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;

//Third-party libraries

//Application-internal dependencies

import org.openmicroscopy.shoola.agents.editor.model.ExperimentInfo;
import org.openmicroscopy.shoola.agents.editor.model.IAttributes;
import org.openmicroscopy.shoola.agents.editor.model.IField;
import org.openmicroscopy.shoola.agents.editor.model.Note;
import org.openmicroscopy.shoola.agents.editor.model.ProtocolRootField;
import org.openmicroscopy.shoola.agents.editor.model.TreeIterator;

/** 
 * This Edit Removes the Experimental Info from an experiment to create a 
 * protocol. It should also remove all the Step Notes, which are only 
 * relevant to an experiment. 
 *
 * @author  William Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:will@lifesci.dundee.ac.uk">will@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class RemoveExpInfo 
	extends AbstractUndoableEdit 
	implements TreeEdit
{
	/**
	 * A reference to the root {@link IField} that the new {@link ExperimentInfo}
	 * is being removed from
	 */
	private ProtocolRootField 			field;
	
	/**
	 * A reference to the new {@link ExperimentInfo}, being removed.
	 */
	private IAttributes 				expInfo;
	
	/**
	 * The root node (that contains the field being edited).
	 * This is used to notify the TreeModel that nodeChanged()
	 */
	private DefaultMutableTreeNode 		node;
	
	/**
	 * The model we are editing the root node of. Used to notify changes. 
	 */
	DefaultTreeModel 					treeModel;
	
	/**
	 * A map of all the step notes we are deleting. 
	 * NB removing all notes and adding them back to steps may not preserve
	 * the order of multiple notes attached to the same step. 
	 */
	private	HashMap<Note, IField> 		stepNotes;
	
	/**
	 * Iterate through the tree model and add all the step notes to the
	 * map. 
	 */
	private void getStepNotes() 
	{
		// make a map of all the step notes and the fields they come from
		stepNotes  = new HashMap<Note, IField>();
		
		TreeNode tn;
		IField f;
		Object userOb;
		DefaultMutableTreeNode node;
		
		Object r = treeModel.getRoot();
		if (! (r instanceof TreeNode)) 		return;
		TreeNode root = (TreeNode)r;
		
		Iterator<TreeNode> iterator = new TreeIterator(root);
		
		while (iterator.hasNext()) {
			tn = iterator.next();
			if (!(tn instanceof DefaultMutableTreeNode)) continue;
			node = (DefaultMutableTreeNode)tn;
			userOb = node.getUserObject();
			if (!(userOb instanceof IField)) continue;
			f = (IField)userOb;
			
			int noteCount = f.getNoteCount();
			for (int i=0; i<noteCount; i++) {
				stepNotes.put(f.getNoteAt(i), f);
			}
		}
	}
	
	/**
	 * Creates an instance and performs the add.
	 * 
	 * @param field		The field/step to add the data-reference to.
	 * @param tree		The tree that contains the field. Needed to notify edits
	 * @param node		The node in the tree that contains the field to edit.
	 */
	public RemoveExpInfo(JTree tree) {
		setTree(tree);
		doEdit();
	}
	
	public void doEdit() {
		if (treeModel == null)	return;
		node = (DefaultMutableTreeNode)treeModel.getRoot();
		
		Object ob = node.getUserObject();
		if (! (ob instanceof ProtocolRootField))	return;
		field = (ProtocolRootField)ob;
		
		expInfo = field.getExpInfo();
		
		getStepNotes();
		
		redo();
	}
	
	public void undo() {
		// add all notes to their fields. 
		Iterator<Note> i = stepNotes.keySet().iterator();
		Note n;
		IField f;
		while(i.hasNext()) {
			n = i.next();
			f = stepNotes.get(n);
			f.addNote(n);
		}
		
		field.setExpInfo(expInfo);
		notifyNodeChanged();
	}
	
	public void redo() {
		// remove all notes from their fields. 
		Iterator<Note> i = stepNotes.keySet().iterator();
		Note n;
		IField f;
		while(i.hasNext()) {
			n = i.next();
			f = stepNotes.get(n);
			f.removeNote(n);
		}
			
		field.setExpInfo(null);
		notifyNodeChanged();
	}
	
	public boolean canUndo() {
		return true;
	}
	
	public boolean canRedo() {
		return true;
	}
	
	/**
	 * Overrides this method in {@link AbstractUndoableEdit} to return
	 * "Experiment Info"
	 */
	 public String getPresentationName() 
	 {
		 return "Delete Experiment Info";
	 }
	
	/**
	 * This notifies all listeners to the TreeModel of a change to the node
	 * in which the attribute has been edited. 
	 * This will update any JTrees that are currently displaying the model. 
	 */
	private void notifyNodeChanged() 
	{
		if (treeModel != null) {
			treeModel.nodeChanged(node);
		}
	}

	/**
	 * Implemented as specified by the {@link TreeEdit} interface.
	 * 
	 * @see TreeEdit#setTree(JTree)
	 */
	public void setTree(JTree tree) {
		if (tree != null)
			treeModel = (DefaultTreeModel)tree.getModel();
	}
}
