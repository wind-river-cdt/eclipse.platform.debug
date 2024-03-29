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

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.swt.widgets.Shell;

public class PDAContentAssistant extends ContentAssistant {
    
    public PDAContentAssistant() {
        super();
        
        PDAContentAssistProcessor processor= new PDAContentAssistProcessor(); 
        setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
    
        enableAutoActivation(false);
        enableAutoInsert(false);
        
        setInformationControlCreator(getInformationControlCreator());   
    }

    private IInformationControlCreator getInformationControlCreator() {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent);
            }
        };
    }
}
