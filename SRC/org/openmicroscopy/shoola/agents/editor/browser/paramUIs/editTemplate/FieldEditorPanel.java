 /*
 * treeEditingComponents.FieldEditorPanel 
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
package org.openmicroscopy.shoola.agents.editor.browser.paramUIs.editTemplate;

//Java imports

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

//Third-party libraries

//Application-internal dependencies

import org.openmicroscopy.shoola.agents.editor.browser.BrowserControl;
import org.openmicroscopy.shoola.agents.editor.browser.paramUIs.ITreeEditComp;
import org.openmicroscopy.shoola.agents.editor.model.IAttributes;
import org.openmicroscopy.shoola.agents.editor.model.IField;
import org.openmicroscopy.shoola.agents.editor.model.params.FieldParamsFactory;
import org.openmicroscopy.shoola.agents.editor.model.params.IParam;
import org.openmicroscopy.shoola.agents.editor.uiComponents.CustomLabel;

/** 
 * The Panel for editing the "Template" of each field.
 * This includes the Name, Description etc.
 * Also, this panel contains the components for template editing of 0, 1 or more 
 * parameters of the field. eg Default values, units etc. 
 *
 * @author  William Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:will@lifesci.dundee.ac.uk">will@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class FieldEditorPanel 
	extends JPanel 
	implements PropertyChangeListener 
{
	/**
	 * Defines a minimum size for this Panel. 
	 */
	public static final Dimension MINIMUM_SIZE = new Dimension(290,300);
	
	/**
	 * The field that this UI component edits.
	 */
	private IField 				field;
	
	/**
	 * The controller for managing undo/redo. Eg manages attribute editing...
	 */
	private BrowserControl 		controller;
	
	/**
	 * The JTree that this field is displayed in. 
	 * Used eg. to notify that this field has been edited (needs refreshing)
	 */
	private JTree 				tree;
	
	/**
	 * A reference to the node represented by this field. 
	 * Used eg. to set the selected field to this node with undo/redo
	 */
	private DefaultMutableTreeNode treeNode;

	/**
	 * Vertical Box layout panel. Main panel.
	 */
	private JPanel 				attributeFieldsPanel;
	
	/**
	 * A comboBox for changing the type of parameter
	 */
	private JComboBox 			inputTypeSelector;
	
	/**
	 * A pop-up menu for choosing a background colour for the field
	 */
	private JPopupMenu 			colourPopupMenu;

	/**
	 * Builds the UI. 
	 */
	private void buildPanel() {
		
		// Panel to hold all components, vertically 
		attributeFieldsPanel = new JPanel();
		attributeFieldsPanel.setLayout(new BoxLayout
				(attributeFieldsPanel, BoxLayout.Y_AXIS));
		attributeFieldsPanel.setBorder(new EmptyBorder(5, 5, 5,5));
		
		
		// For each parameter of this field, add the components for
		// editing their default or template values. 
		buildParamComponents();
		
		this.setLayout(new BorderLayout());
		this.add(attributeFieldsPanel, BorderLayout.NORTH);
		
		//this.setPreferredSize(MINIMUM_SIZE);
		//this.setMinimumSize(MINIMUM_SIZE);
		this.validate();
	}

	/**
	 * Each parameter editing component is added here.
	 * This class becomes a property change listener for each one.
	 * 
	 * @param defaultEdit	A component for editing the defaults of each param
	 */
	private void addFieldComponent(JComponent defaultEdit) 
	{
		attributeFieldsPanel.add(Box.createVerticalStrut(5));
		attributeFieldsPanel.add(defaultEdit);
		defaultEdit.addPropertyChangeListener( 
				ITreeEditComp.VALUE_CHANGED_PROPERTY, 
				this);
	}

	/**
	 * Add additional UI components for editing the value of this field.
	 * Use a Factory to create the UI components, depending on the value type
	 */
	private void buildParamComponents() 
	{
		int paramCount = field.getParamCount();
		
		attributeFieldsPanel.add(new JSeparator());
		attributeFieldsPanel.add(Box.createVerticalStrut(5));
		JLabel paramLabel = new CustomLabel("Parameters:");
		JPanel paramHeader = new JPanel(new BorderLayout());
		paramHeader.add(paramLabel, BorderLayout.WEST);
		
		JButton addParamsButton = new AddParamActions(field, tree, 
				treeNode, controller).getButton();
		addParamsButton.addPropertyChangeListener(
				AddParamActions.PARAM_ADDED_PROPERTY, this);
		paramHeader.add(addParamsButton, BorderLayout.EAST);
		
		attributeFieldsPanel.add(paramHeader);
		
		for (int i=0; i<paramCount; i++) {
			IParam param = field.getParamAt(i);
			JComponent edit = ParamTemplateUIFactory.
									getEditDefaultComponent(param);
			if (edit != null)
				addFieldComponent(edit);
		}
	}

	/**
	 * A MouseAdapter to display the colour chooser pop-up menu.
	 * 
	 * @author will
	 */
	private class PopupListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e) {
	        showPopUp(e);
	    }
	    public void mouseReleased(MouseEvent e) {
	        showPopUp(e);
	    }
	    private void showPopUp(MouseEvent e) {
	    	colourPopupMenu.show(e.getComponent(),
	                       e.getX(), e.getY());
	    }
	}

	/**
	 * Launches the colour pop-up menu
	 */
	protected JButton 			colourSelectButton;
	
	/**
	 * A bound property of this panel.
	 * A change in this property indicates that this panel should be rebuilt
	 * from the data model. 
	 */
	public static final String PANEL_CHANGED_PROPERTY = "panelChangedProperty";
	
	/**
	 * Creates a blank panel. Displayed when more than one field is selected
	 * (also when no fields selected eg when application starts)
	 */
	public FieldEditorPanel() {	// a blank 
		this.setPreferredSize(MINIMUM_SIZE);
		//this.setMinimumSize(MINIMUM_SIZE);
	}
	
	/**
	 * Creates an instance of this class for editing the field.
	 * 
	 * @param field		The Field to edit
	 * @param tree		The JTree in which the field is displayed
	 * @param treeNode	The node of the Tree which contains the field
	 */
	public FieldEditorPanel(IField field, JTree tree, 
			DefaultMutableTreeNode treeNode, BrowserControl controller) 
	{
		this.field = field;
		
		this.tree = tree;
		this.treeNode = treeNode;
		this.controller = controller;
		
		buildPanel();
	}
	
	/**
	 * takes the parameter-type of the comboBox and 
	 * calls paramTypeChanged(String paramType)
	 */
	public class ParamTypeSelectorListener implements ActionListener 
	{	
		public void actionPerformed (ActionEvent event) {
			JComboBox source = (JComboBox)event.getSource();
			int selectedIndex = source.getSelectedIndex();
			String newType = FieldParamsFactory.INPUT_TYPES[selectedIndex];
			paramTypeChanged(newType);
		}
	}	
	
	/**
	 * Changes the Parameter type of the first parameter of this field.
	 * In future, it may be preferable to allow users to change the type
	 * of other parameters of this field, depending on selection etc. 
	 * 
	 * @param newType	A String that defines the type of parameter selected
	 */
	public void paramTypeChanged(String newType) {
		
		if (field.getParamCount() > 0) {
			IParam oldParam = field.getParamAt(0);
			field.removeParam(oldParam);
		}
		IParam newParam = FieldParamsFactory.getFieldParam(newType);
		field.addParam(0, newParam);
		
		/* refresh this node in the JTree, and rebuild this panel*/
		updateEditingOfTreeNode();
		rebuildEditorPanel();
	}
	
	/**
	 * Called by components of this panel when they want to perform an edit
	 * that is added to the undo/redo queue. 
	 * 
	 * @param attrName		The name of the attribute (can be null if more than
	 * 		one attribute is being edited)
	 * @param newVal		The new value of the attribute. Could be a string
	 *  	(if one attribute edited) or a Map, if more than one value edited. 	
	 * @param displayName	A display name for undo/redo
	 */
	public void fieldEdited(String attrName, Object newVal, String displayName) {
		
		/* Need controller to pass on the edit  */
		if (controller == null) return;
		
		System.out.println("FieldEditorPanel fieldEdited " + attrName + " " + newVal);

		if ((newVal instanceof String) || (newVal == null)){
			String newValue = (newVal == null ? null : newVal.toString());
		 	//controller.editAttribute(field, attrName, newValue, 
		 	//		displayName, tree, treeNode);
		}
		
		else if (newVal instanceof HashMap) {
			HashMap newVals = (HashMap)newVal;
			//controller.editAttributes(field, displayName, newVals, 
			//		tree, treeNode);
		}
	}
	
	/**
	 * If the size of a sub-component of this panel changes, 
	 * the JTree in which it is contained must be required to 
	 * re-draw the panel. 
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		
		
		String propName = evt.getPropertyName();
		
		//System.out.println("FieldEditorPanel propertyChanege: " + propName);
				
		if (ITreeEditComp.VALUE_CHANGED_PROPERTY.equals(propName)) {
			
			if (evt.getSource() instanceof ITreeEditComp) {
				
				/* Need controller to pass on the edit  */
				if (controller == null) return;
				
				ITreeEditComp src = (ITreeEditComp)evt.getSource();
				IAttributes param = src.getParameter();
				String attrName = src.getAttributeName();
				String displayName = src.getEditDisplayName();
				
				String newValue;
				Object newVal = evt.getNewValue();
				
				
				if ((newVal instanceof String) || (newVal == null)){
					newValue = (newVal == null ? null : newVal.toString());
				 	// controller.editAttribute(param, attrName, newValue, 
				 	//		displayName, tree, treeNode);
				}
				
				else if (newVal instanceof HashMap) {
					HashMap newVals = (HashMap)newVal;
					// controller.editAttributes(param, displayName, newVals, 
						//	tree, treeNode);
				}
				
				updateEditingOfTreeNode();
				
			}
		} else if (AddParamActions.PARAM_ADDED_PROPERTY.equals(propName)) {
			updateEditingOfTreeNode();
			rebuildEditorPanel();
		}
	}
	
	
	/**
	 * This method is used to refresh the size of the corresponding
	 * node in the JTree.
	 * It must also remain in the editing mode, otherwise the user who
	 * is currently editing it will be required to click again to 
	 * continue editing.
	 * This can be achieved by calling startEditingAtPath(tree, path)
	 */
	public void updateEditingOfTreeNode() {
		if ((tree != null) && (treeNode !=null)) {
			
			TreePath path = new TreePath(treeNode.getPath());
			
			tree.getUI().startEditingAtPath(tree, path);
		}
	}
	
	public void rebuildEditorPanel() {
		this.firePropertyChange(PANEL_CHANGED_PROPERTY, null, "refresh");
	}
}
