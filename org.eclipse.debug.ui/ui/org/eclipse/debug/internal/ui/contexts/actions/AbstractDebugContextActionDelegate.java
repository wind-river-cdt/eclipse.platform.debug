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

package org.eclipse.debug.internal.ui.contexts.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

public abstract class AbstractDebugContextActionDelegate implements IWorkbenchWindowActionDelegate, IViewActionDelegate, IActionDelegate2, IDebugContextListener {

	/**
	 * The underlying action for this delegate
	 */
	private IAction fAction;
	/**
	 * This action's view part, or <code>null</code> if not installed in a
	 * view.
	 */
	private IViewPart fViewPart;

	/**
	 * Cache of the most recent seletion
	 */
	private IStructuredSelection fSelection = StructuredSelection.EMPTY;

	/**
	 * Whether this delegate has been initialized
	 */
	private boolean fInitialized = false;

	/**
	 * The window associated with this action delegate May be <code>null</code>
	 */
	protected IWorkbenchWindow fWindow;

	/**
	 * Background job for this action, or <code>null</code> if none.
	 */
	private DebugRequestJob fBackgroundJob = null;

	/**
	 * Background job to update enablement.
	 */
	private UpdateEnablementJob fUpdateEnablementJob = new UpdateEnablementJob();

	/**
	 * Used to schedule jobs, or <code>null</code> if none
	 */
	private IWorkbenchSiteProgressService fProgressService = null;

	class UpdateEnablementJob extends Job {

		IAction targetAction = null;
		ISelection targetSelection = null;

		public UpdateEnablementJob() {
			super(ActionMessages.AbstractDebugActionDelegate_1);
			setPriority(Job.INTERACTIVE);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			IAction action = null;
			ISelection selection = null;
			synchronized (this) {
				action = targetAction;
				selection = targetSelection;
			}
			update(action, selection);
			return Status.OK_STATUS;
		}

		public synchronized void setTargets(IAction action, ISelection selection) {
			targetAction = action;
			targetSelection = selection;
		}
	}

	class DebugRequestJob extends Job {

		private Object[] fElements = null;

		/**
		 * Constructs a new job to perform a debug request (for example, step)
		 * in the background.
		 * 
		 * @param name job name
		 */
		public DebugRequestJob(String name) {
			super(name);
			setPriority(Job.INTERACTIVE);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			MultiStatus status = new MultiStatus(DebugUIPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED, getStatusMessage(), null);
			Object[] targets = null;
			synchronized (this) {
				targets = fElements;
				fElements = null;
			}
			for (int i = 0; i < targets.length; i++) {
				Object element = targets[i];
				Object target = getTarget(element);
				try {
					// Action's enablement could have been changed since
					// it was last enabled. Check that the action is still
					// enabled before running the action.
					if (target != null && isEnabledFor(target))
						doAction(target);
				} catch (DebugException e) {
					status.merge(e.getStatus());
				}
			}
			return status;
		}

		/**
		 * Sets the selection to operate on.
		 * 
		 * @param elements
		 */
		public synchronized void setTargets(Object[] elements) {
			fElements = elements;
		}

	}

	/**
	 * It's crucial that delegate actions have a zero-arg constructor so that
	 * they can be reflected into existence when referenced in an action set in
	 * the plugin's plugin.xml file.
	 */
	public AbstractDebugContextActionDelegate() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		IWorkbenchWindow window = getWindow();
		if (getWindow() != null) {
			IViewPart view = getView();
			if (view != null) {
				String partId = view.getSite().getId();
				DebugContextManager.getDefault().removeDebugContextListener(this, window, partId);
			} else {
				DebugContextManager.getDefault().removeDebugContextListener(this, window);
			}
		}
		fBackgroundJob = null;
		fSelection = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		setWindow(window);
		DebugContextManager.getDefault().addDebugContextListener(this, window);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (action.isEnabled()) {
			IStructuredSelection selection = getContext();
			// disable the action so it cannot be run again until an event or
			// selection change updates the enablement
			action.setEnabled(false);
			runInBackground(action, selection);
		}
	}

	/**
	 * Runs this action in a background job.
	 */
	private void runInBackground(IAction action, IStructuredSelection selection) {
		if (fBackgroundJob == null) {
			fBackgroundJob = new DebugRequestJob(DebugUIPlugin.removeAccelerators(action.getText()));
		}
		fBackgroundJob.setTargets(selection.toArray());
		schedule(fBackgroundJob);
	}

	/**
	 * AbstractDebugActionDelegates come in 2 flavors: IViewActionDelegate,
	 * IWorkbenchWindowActionDelegate delegates.
	 * </p>
	 * <ul>
	 * <li>IViewActionDelegate delegate: getView() != null</li>
	 * <li>IWorkbenchWindowActionDelegate: getView == null</li>
	 * </ul>
	 * <p>
	 * Only want to call update(action, selection) for IViewActionDelegates. An
	 * initialize call to update(action, selection) is made for all flavors to
	 * set the initial enabled state of the underlying action.
	 * IWorkbenchWindowActionDelegate's listen to selection changes in the debug
	 * view only.
	 * </p>
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection s) {
		boolean wasInitialized = initialize(action, s);
		if (!wasInitialized) {
			if (getView() != null) {
				fUpdateEnablementJob.setTargets(action, s);
				schedule(fUpdateEnablementJob);

			}
		}
	}

	protected void update(IAction action, ISelection s) {
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			action.setEnabled(getEnableStateForContext(ss));
			setContext(ss);
		} else {
			action.setEnabled(false);
			setContext(StructuredSelection.EMPTY);
		}
	}

	/**
	 * Performs the specific action on this element.
	 */
	protected abstract void doAction(Object element) throws DebugException;

	/**
	 * Returns the String to use as an error dialog message for a failed action.
	 * This message appears as the "Message:" in the error dialog for this
	 * action. Default is to return null.
	 */
	protected String getErrorDialogMessage() {
		return null;
	}

	/**
	 * Returns the String to use as a status message for a failed action. This
	 * message appears as the "Reason:" in the error dialog for this action.
	 * Default is to return the empty String.
	 */
	protected String getStatusMessage() {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
		fViewPart = view;
		fProgressService = (IWorkbenchSiteProgressService) view.getAdapter(IWorkbenchSiteProgressService.class);
	}

	/**
	 * Returns this action's view part, or <code>null</code> if not installed
	 * in a view.
	 * 
	 * @return view part or <code>null</code>
	 */
	protected IViewPart getView() {
		return fViewPart;
	}

	/**
	 * Initialize this delegate, updating this delegate's presentation. As well,
	 * all of the flavors of AbstractDebugActionDelegates need to have the
	 * initial enabled state set with a call to update(IAction, ISelection).
	 * 
	 * @param action the presentation for this action
	 * @return whether the action was initialized
	 */
	protected boolean initialize(IAction action, ISelection selection) {
		if (!isInitialized()) {
			setAction(action);
			if (getView() == null) {
				// update on the selection in the debug view
				IWorkbenchWindow window = getWindow();
				if (window != null && window.getShell() != null && !window.getShell().isDisposed()) {
					IWorkbenchPage page = window.getActivePage();
					if (page != null) {
						selection = page.getSelection(IDebugUIConstants.ID_DEBUG_VIEW);
					}
				}
			}
			update(action, selection);
			setInitialized(true);
			return true;
		}
		return false;
	}

	/**
	 * Returns the most recent selection
	 * 
	 * @return structured selection
	 */
	protected IStructuredSelection getContext() {
		return fSelection;
	}

	/**
	 * Sets the most recent selection
	 * 
	 * @parm selection structured selection
	 */
	private void setContext(IStructuredSelection context) {
		fSelection = context;
	}

	public void contextActivated(ISelection context, IWorkbenchPart part) {
		fUpdateEnablementJob.setTargets(getAction(), context);
		schedule(fUpdateEnablementJob);		
	}

	protected void setAction(IAction action) {
		fAction = action;
	}

	protected IAction getAction() {
		return fAction;
	}

	protected void setView(IViewPart viewPart) {
		fViewPart = viewPart;
	}

	protected boolean isInitialized() {
		return fInitialized;
	}

	protected void setInitialized(boolean initialized) {
		fInitialized = initialized;
	}

	protected IWorkbenchWindow getWindow() {
		return fWindow;
	}

	protected void setWindow(IWorkbenchWindow window) {
		fWindow = window;
	}

	/**
	 * Return whether the action should be enabled or not based on the given
	 * selection.
	 */
	protected boolean getEnableStateForContext(IStructuredSelection selection) {
		if (selection.size() == 0) {
			return false;
		}
		Iterator itr = selection.iterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			Object target = getTarget(element);
			if (target == null || !isEnabledFor(target)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Translates the selected object to the target to operate
	 * on as required. For example, an adpater on the selection.
	 *  
	 * @param selectee
	 * @return target to operate/enable action on
	 */
	protected Object getTarget(Object selectee) {
		return selectee;
	}

	protected boolean isEnabledFor(Object element) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction,
	 *      org.eclipse.swt.widgets.Event)
	 */
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 */
	public void init(IAction action) {
	}

	/**
	 * Schedules the given job with this action's progress service
	 * 
	 * @param job
	 */
	private void schedule(Job job) {
		if (fProgressService == null) {
			job.schedule();
		} else {
			fProgressService.schedule(job);
		}
	}
}