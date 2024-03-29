/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.breakpoints;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAVariable;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Adapter to create specialized watchpoints in PDA files and the variables views.
 */
public class PDAToggleWatchpointsTarget extends PDABreakpointAdapter {

    final private boolean fAccessModeEnabled;
    final private boolean fModificationModeEnabled;
    
    PDAToggleWatchpointsTarget(boolean access, boolean modification) {
        fAccessModeEnabled = access;
        fModificationModeEnabled = modification;
    }
    
    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
        if (super.canToggleWatchpoints(part, selection)) {
            return true;
        } else {
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection ss = (IStructuredSelection)selection;
                return ss.getFirstElement() instanceof PDAVariable;
            }
        }
        return false;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleWatchpoints(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
	    String[] variableAndFunctionName = getVariableAndFunctionName(part, selection);
	    
	    if (variableAndFunctionName != null && part instanceof ITextEditor && selection instanceof ITextSelection) {
	    	// Selection inside text editor.  Create a watchpoint based on 
	    	// current source line.
	        ITextEditor editorPart = (ITextEditor)part;
	        int lineNumber = ((ITextSelection)selection).getStartLine();
	        IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
	        String var = variableAndFunctionName[0];
	        String fcn = variableAndFunctionName[1];
	        toggleWatchpoint(resource, lineNumber, fcn, var, fAccessModeEnabled, fModificationModeEnabled);
	    } else if (selection instanceof IStructuredSelection && 
	               ((IStructuredSelection)selection).getFirstElement() instanceof PDAVariable ) 
	    {
	        // Selection is inside a variables view.  Create a watchpoint 
	        // using information from  the variable.  Retrieving information
	        // from the model requires performing source lookup which should be 
	        // done on a background thread.
	        final PDAVariable var = (PDAVariable)((IStructuredSelection)selection).getFirstElement();
	        final PDAStackFrame frame = var.getStackFrame();
	        final Shell shell = part.getSite().getShell(); 
	        
	        new Job("Toggle PDA Watchpoint") {
	            { setSystem(true); }
	            
	            protected IStatus run(IProgressMonitor monitor) {
	                try {
    	                IFile file = getResource(var.getStackFrame());
    	                String varName = var.getName();
    	                int line = findLine(file, varName);
    	                toggleWatchpoint(file, line, frame.getName(), varName, 
    	                    fAccessModeEnabled, fModificationModeEnabled);
	                } catch (final CoreException e) {
	                    // Need to switch back to the UI thread to show the error
	                    // dialog.
	                    new WorkbenchJob(shell.getDisplay(), "Toggle PDA Watchpoint") {
	                        { setSystem(true); }
	                        
	                        public IStatus runInUIThread(IProgressMonitor monitor) {
	                            ErrorDialog.openError(shell, "Failed to create PDA watchpoint", "Failed to create PDA watchpoint.\n", e.getStatus());
	                            return Status.OK_STATUS;
	                        }
	                    }.schedule();
	                }
	                return Status.OK_STATUS;
	            }
	        }.schedule();
	    }
	}

	private IFile getResource(PDAStackFrame frame) {
	    ISourceLocator locator = frame.getLaunch().getSourceLocator();
	    Object sourceElement = locator.getSourceElement(frame);
	    if (sourceElement instanceof IFile) {
	        return (IFile)sourceElement;
	    }
	    return null;
	}
	
	private int findLine(IFile file, String var) throws CoreException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
	    
	    int lineNum = 0;
	    try {
    	    while(true) {
    	        String line = reader.readLine().trim();
    	        if (line.startsWith("var")) {
    	            String varName = line.substring("var".length()).trim();
    	            if (varName.equals(var)) {
    	                break;
    	            }
    	        }
                lineNum++;
    	    }
	    } catch (IOException e) {
	        // end of file reached and line wasn't found
	        return -1;
	    } finally {
	        try {
	            reader.close();
	        } catch (IOException e) {}
	    }
	    return lineNum;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#toggleBreakpoints(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
     */
    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        if (canToggleWatchpoints(part, selection)) {
            toggleWatchpoints(part, selection);
        } else {
            toggleLineBreakpoints(part, selection);
        }    
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#canToggleBreakpoints(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
     */
    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleLineBreakpoints(part, selection) || canToggleWatchpoints(part, selection);
    }
}
