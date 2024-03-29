/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.editor;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * PDA editor
 */
public class PDAEditor extends AbstractDecoratedTextEditor {
    
    /**
     * Creates a PDE editor
     */
    public PDAEditor() {
        super();
        setSourceViewerConfiguration(new PDASourceViewerConfiguration());
        setRulerContextMenuId("pda.editor.rulerMenu");
        setEditorContextMenuId("pda.editor.editorMenu");
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     */
    protected void createActions() {
        super.createActions();
        ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.debug.examples.ui.pda.editor.PDAEditorMessages"); //$NON-NLS-1$
        IAction action = new ContentAssistAction(bundle, "ContentAssistProposal.", this); //$NON-NLS-1$
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction("ContentAssistProposal", action); //$NON-NLS-1$
    }
    
    
}
