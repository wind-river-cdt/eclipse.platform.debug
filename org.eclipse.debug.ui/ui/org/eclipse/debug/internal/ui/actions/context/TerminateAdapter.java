/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.context;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.internal.ui.actions.provisional.IAsynchronousTerminateAdapter;
import org.eclipse.debug.internal.ui.actions.provisional.IBooleanRequestMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousRequestMonitor;

/**
 * Default terminate adapter for standard debug model.
 * 
 * @since 3.2
 */
public class TerminateAdapter implements IAsynchronousTerminateAdapter {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.actions.provisional.IAsynchronousTerminateAdapter#canTerminate(java.lang.Object, org.eclipse.debug.internal.ui.actions.provisional.IBooleanRequestMonitor)
	 */
	public void canTerminate(final Object element, final IBooleanRequestMonitor requestMonitor) {
		Assert.isTrue(element instanceof ITerminate, "element must be instance of ITerminate"); //$NON-NLS-1$
		Job job = new Job("canTerminate") { //$NON-NLS-1$
			protected IStatus run(IProgressMonitor monitor) {
				ITerminate terminate = (ITerminate) element;
				requestMonitor.setResult(terminate.canTerminate());
				requestMonitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.actions.provisional.IAsynchronousTerminateAdapter#isTerminated(java.lang.Object, org.eclipse.debug.internal.ui.actions.provisional.IBooleanRequestMonitor)
	 */
	public void isTerminated(final Object element, final IBooleanRequestMonitor requestMonitor) {
		Assert.isTrue(element instanceof ITerminate, "element must be instance of ITerminate"); //$NON-NLS-1$
		Job job = new Job("isTerminated") { //$NON-NLS-1$
			protected IStatus run(IProgressMonitor monitor) {
				ITerminate terminate = (ITerminate) element;
				requestMonitor.setResult(terminate.isTerminated());
				requestMonitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.actions.provisional.IAsynchronousTerminateAdapter#terminate(java.lang.Object, org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousRequestMonitor)
	 */
	public void terminate(final Object element, final IAsynchronousRequestMonitor requestMonitor) {
		Assert.isTrue(element instanceof ITerminate, "element must be instance of ITerminate"); //$NON-NLS-1$
		Job job = new Job("terminate") { //$NON-NLS-1$
			protected IStatus run(IProgressMonitor monitor) {
				ITerminate terminate = (ITerminate) element;
				try {
					terminate.terminate();
				} catch (DebugException e) {
					requestMonitor.setStatus(e.getStatus());
				}
				requestMonitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();

	}

}