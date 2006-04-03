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
package org.eclipse.debug.internal.ui.actions;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Adds the selected launch configuration to the launch favorites.
 */
public class AddToFavoritesAction extends SelectionListenerAction {
	
	private ILaunchConfiguration fConfiguration = null;
	private String fMode =null;
	private ILaunchGroup fGroup = null;

	/**
	 * Constructs a new action.
	 */
	public AddToFavoritesAction() {
		super(""); //$NON-NLS-1$
		setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.EDIT_LAUNCH_CONFIGURATION_ACTION);
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		setLaunchConfiguration(null);
		setMode(null);
		setGroup(null);
		if (selection.size() == 1) {
			Object object = selection.getFirstElement();
			ILaunch launch = null;
			if (object instanceof IAdaptable) {
				launch = (ILaunch)((IAdaptable)object).getAdapter(ILaunch.class); 
			}
			if (launch == null) {
				if (object instanceof ILaunch) {
					launch = (ILaunch)object;
				} else if (object instanceof IDebugElement) {
					launch = ((IDebugElement)object).getLaunch();
				} else if (object instanceof IProcess) {
					launch = ((IProcess)object).getLaunch();
				}
			}
			if (launch != null) {
				ILaunchConfiguration configuration = launch.getLaunchConfiguration();
				if (configuration != null) {
					ILaunchGroup group= DebugUITools.getLaunchGroup(configuration, getMode());
					if (group == null) {
					    return false;
					}
					setGroup(group);
					setLaunchConfiguration(configuration);
					setMode(launch.getLaunchMode());				
					setText(MessageFormat.format(ActionMessages.AddToFavoritesAction_1, new String[]{DebugUIPlugin.removeAccelerators(getGroup().getLabel())})); 
				}
			}
		}
		
		// Disable the action if the launch config is private
		ILaunchConfiguration config = getLaunchConfiguration();
		if (config == null) {
			return false;
		} 
		if (DebugUITools.isPrivate(config)) {
				return false;
		}
		
		if (getGroup() != null) {
			try {
				List groups = config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List)null);
				if (groups != null) {
					return !groups.contains(getGroup().getIdentifier());
				}
				return true;
			} catch (CoreException e) {
			}
			
		}
		
		return false;
	}

	protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfiguration = configuration;
	}
	
	protected ILaunchConfiguration getLaunchConfiguration() {
		return fConfiguration;
	}
	
	protected void setMode(String mode) {
		fMode = mode;
	}
	
	protected String getMode() {
		return fMode;
	}
	
	protected void setGroup(ILaunchGroup group) {
		fGroup = group;
	}
	
	protected ILaunchGroup getGroup() {
		return fGroup;
	}
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		final CoreException[] ex = new CoreException[1];
		BusyIndicator.showWhile(DebugUIPlugin.getStandardDisplay(), new Runnable() {
			public void run() {
				try {
					List list = getLaunchConfiguration().getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List)null);
					if (list == null) {
						list = new ArrayList();
					}
					list.add(getGroup().getIdentifier());
					ILaunchConfigurationWorkingCopy copy = getLaunchConfiguration().getWorkingCopy();
					copy.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, list);
					copy.doSave();
					setEnabled(false);
				} catch (CoreException e) {
					ex[0] = e;
				}
			}
		});
		if (ex[0] != null) {
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.AddToFavoritesAction_2, ActionMessages.AddToFavoritesAction_3, ex[0].getStatus()); // 
		}
	}

}