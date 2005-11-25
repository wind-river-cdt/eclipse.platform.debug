/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.variables;

 
import java.text.MessageFormat;
import java.util.Iterator;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValueModification;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.VariableValueEditorManager;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.IVariableValueEditor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for changing the value of primitives and <code>String</code> variables.
 * This action will attempt to delegate the editing operation to a registered
 * variable value editor, if any is provided for the variable's debug model.
 * @see org.eclipse.debug.ui.actions.VariableValueEditorManager
 */
public class ChangeVariableValueAction extends SelectionProviderAction {
    
	protected IVariable fVariable;
    private VariablesView fView;
    private boolean fEditing= false;
	
    /**
     * Creates a new ChangeVariableValueAction for the given variables view
     * @param view the varibles view in which this action will appear
     */
	public ChangeVariableValueAction(VariablesView view) {
		super(view.getViewer(), ActionMessages.ChangeVariableValue_title); 
		setDescription(ActionMessages.ChangeVariableValue_toolTipText); 
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_CHANGE_VARIABLE_VALUE));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_CHANGE_VARIABLE_VALUE));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_CHANGE_VARIABLE_VALUE));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			this,
			IDebugHelpContextIds.CHANGE_VALUE_ACTION);
		fView= view;
	}
	
	/**
	 * Edit the variable value with an inline text editor.  
	 */
	protected void doActionPerformed(final IVariable variable) {
	    Shell shell = fView.getViewSite().getShell();
		// If a previous edit is still in progress, don't start another		
	    if (fEditing) {
	        return;
	    }
	    fEditing= true;
		fVariable = variable;
	    if (!delegateEdit(shell)) {
	        doDefaultEdit(shell);
	    }
		fEditing= false;
	}
	
	/**
	 * Attempts to edit the variable by delegating to anyone who's
	 * contributed a variable value editor via extension. Returns
	 * <code>true</code> if a delegate handled the edit, <code>false</code>
	 * if the variable still needs to be edited.
	 * 
     * @param shell a shell for prompting the user
     * @return whether or not a delegate attempted to edit the variable
     */
    private boolean delegateEdit(Shell shell) {
        String modelIdentifier = fVariable.getModelIdentifier();
        IVariableValueEditor editor= VariableValueEditorManager.getDefault().getVariableValueEditor(modelIdentifier);
        if (editor != null) {
            return editor.editVariable(fVariable, shell);
        }
        return false;
    }

    /**
     * Edits the variable using the default variable editor
     * @param shell a shell for prompting the user
     */
    protected void doDefaultEdit(Shell shell) {
	    String name= ""; //$NON-NLS-1$
		String value= ""; //$NON-NLS-1$
		try {
			name= fVariable.getName();
			value= fVariable.getValue().getValueString();
		} catch (DebugException exception) {
			DebugUIPlugin.errorDialog(shell, ActionMessages.ChangeVariableValue_errorDialogTitle,ActionMessages.ChangeVariableValue_errorDialogMessage, exception);	// 
			return;
		}
		ChangeVariableValueInputDialog inputDialog= new ChangeVariableValueInputDialog(shell, ActionMessages.ChangeVariableValue_1, MessageFormat.format(ActionMessages.ChangeVariableValue_2, new String[] {name}), value, new IInputValidator() { // 
			/**
			 * Returns an error string if the input is invalid
			 */
			public String isValid(String input) {
				try {
					if (fVariable.verifyValue(input)) {
						return null; // null means valid
					}
				} catch (DebugException exception) {
					return ActionMessages.ChangeVariableValue_3; 
				}
				return ActionMessages.ChangeVariableValue_4; 
			}
		});
		
		inputDialog.open();
		String newValue= inputDialog.getValue();
		if (newValue != null) {
			// null value means cancel was pressed
			try {
				fVariable.setValue(newValue);
				getSelectionProvider().setSelection(new StructuredSelection(fVariable));
			} catch (DebugException de) {
				DebugUIPlugin.errorDialog(shell, ActionMessages.ChangeVariableValue_errorDialogTitle,ActionMessages.ChangeVariableValue_errorDialogMessage, de);	// 
			}
		}
	}
		
	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	protected void update(IStructuredSelection sel) {
		Iterator iter= sel.iterator();
		if (iter.hasNext()) {
			Object object= iter.next();
			if (object instanceof IValueModification) {
				IValueModification varMod= (IValueModification)object;
				if (!varMod.supportsValueModification()) {
					setEnabled(false);
					return;
				}
				setEnabled(!iter.hasNext());
				return;
			}
		}
		setEnabled(false);
	}

	/**
	 * @see IAction#run()
	 */
	public void run() {
		Iterator iterator= getStructuredSelection().iterator();
		doActionPerformed((IVariable)iterator.next());
	}
	
	/**
	 * @see SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection sel) {
		update(sel);
	}
}
