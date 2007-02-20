/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * A history of launches and favorites for a launch group
 */
public class LaunchHistory implements ILaunchListener, ILaunchConfigurationListener {

	/**
	 * Listing of the complete launch history, which includes favorites in the launched ordering
	 */
	private Vector fCompleteHistory = new Vector();
	
	private ILaunchGroup fGroup;
	private Vector fFavorites = new Vector();
	
	private static List fgLaunchHistoryInstances= new ArrayList();
	
	/**
	 * Creates a new launch history for the given launch group
	 */
	public LaunchHistory(ILaunchGroup group) {
		fGroup = group;
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager(); 
		manager.addLaunchListener(this);
		manager.addLaunchConfigurationListener(this);
		fgLaunchHistoryInstances.add(this);
	}
	
	/**
	 * Disposes this history
	 */
	public void dispose() {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		manager.removeLaunchListener(this);
		manager.removeLaunchConfigurationListener(this);
		fgLaunchHistoryInstances.remove(this);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (configuration != null && !configuration.isWorkingCopy() && DebugUIPlugin.doLaunchConfigurationFiltering(configuration) && accepts(configuration)) {
			addHistory(configuration, true);
		}
	}
	
	/**
	 * Returns if the current history contains the specified <code>ILaunchConfiguration</code>
	 * @param config the config to look for
	 * @return true if the current history contains the specified configuration, false otherwise
	 * @since 3.3
	 */
	public boolean contains(ILaunchConfiguration configuration) {
		return fCompleteHistory.contains(configuration);
	}
	
	/**
	 * Returns the index of the specified <code>ILaunchConfiguration</code> in the 
	 * current launch history. The value -1 is returned if the configuration is not
	 * in the current history, else its zero-relative index is returned.
	 * @param configuration
	 * @return the index of the specified <code>ILaunchConfiguration</code> in the 
	 * current launch history
	 * @since 3.3
	 */
	public int indexOf(ILaunchConfiguration configuration) {
		return fCompleteHistory.indexOf(configuration);
	}
	
	/**
	 * Adds the given configuration to this history
	 * 
	 * @param configuration
	 * @param prepend whether the configuration should be added to the beginning of
	 * the history list
	 */
	protected void addHistory(ILaunchConfiguration configuration, boolean prepend) {
		if(configuration.isWorkingCopy()) {
			return;
		}
		checkFavorites(configuration);
		int index = fCompleteHistory.indexOf(configuration);
		if(index == 0) {
			return;
		}
		if(index < 0) {
			if(prepend) {
				fCompleteHistory.add(0, configuration);
			}
			else {
				fCompleteHistory.add(configuration);
			}
		}
		else {
			fCompleteHistory.add(0, fCompleteHistory.remove(index));
		}
		resizeHistory();
		fireLaunchHistoryChanged();
	}

	/**
	 * Notifies all <code>ILaunchHistoryChangedListener</code>s that the launch history has been modified
	 * 
	 * @since 3.3
	 */
	private void fireLaunchHistoryChanged() {
		DebugUIPlugin.getDefault().getLaunchConfigurationManager().fireLaunchHistoryChanged();
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {}

	/**
	 * Returns the most recently launched configuration in this history, or
	 * <code>null</code> if none.
	 * 
	 * @return the most recently launched configuration in this history, or
	 * <code>null</code> if none 
	 */
	public ILaunchConfiguration getRecentLaunch() {
		ILaunchConfiguration[] history = getCompleteLaunchHistory();
		if(history.length > 0) {
			return history[0];
		}
		return null;
	}
	
	/**
	 * Returns the launch configurations in this history, in most recently
	 * launched order, not including any entries from the favorites listing.
	 * 
	 * @return launch history
	 */
	public ILaunchConfiguration[] getHistory() {
		Vector history = new Vector();
		try {
			ILaunchConfiguration config = null;
			for(Iterator iter = fCompleteHistory.iterator(); iter.hasNext();) {
				config = (ILaunchConfiguration) iter.next();
				if(config.exists() && !fFavorites.contains(config) && 
						DebugUIPlugin.doLaunchConfigurationFiltering(config) && 
						!WorkbenchActivityHelper.filterItem(new LaunchConfigurationTypeContribution(config.getType()))) {
					history.add(config);
				}
			}
			//size it to the max specified history size
			if(history.size() > getMaxHistorySize()) {
				history.setSize(getMaxHistorySize());
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		return (ILaunchConfiguration[]) history.toArray(new ILaunchConfiguration[history.size()]);
	}
	
	/**
	 * Returns the complete launch history in the order they were last launched, this listing includes all
	 * entries including those from the favorites listing, but not those that have been filtered via
	 * launch configuration filtering or capabilities filtering
	 * @return the list of last launched <code>ILaunchConfiguration</code>s
	 * 
	 * @since 3.3
	 */
	public ILaunchConfiguration[] getCompleteLaunchHistory() {
		Vector history = new Vector();
		try {
			ILaunchConfiguration config = null;
			for(Iterator iter = fCompleteHistory.iterator(); iter.hasNext();){
				config = (ILaunchConfiguration) iter.next();
				if(config.exists() && DebugUIPlugin.doLaunchConfigurationFiltering(config) && 
				!WorkbenchActivityHelper.filterItem(new LaunchConfigurationTypeContribution(config.getType()))) {
					history.add(config);
				}
			}
		}
		catch (CoreException ce) {DebugUIPlugin.log(ce);}
		return (ILaunchConfiguration[]) history.toArray(new ILaunchConfiguration[history.size()]);
	}
	
	/**
	 * Returns the favorite launch configurations in this history, in the order
	 * they were created.
	 * 
	 * @return launch favorites
	 */
	public ILaunchConfiguration[] getFavorites() {
		return (ILaunchConfiguration[])fFavorites.toArray(new ILaunchConfiguration[fFavorites.size()]);
	}
	
	/**
	 * Sets this container's favorites.
	 * 
	 * @param favorites
	 */
	public void setFavorites(ILaunchConfiguration[] favorites) {
		fFavorites = new Vector(Arrays.asList(favorites));
	}	
	
	/**
	 * Adds the given configuration to the favorites list.
	 * 
	 * @param configuration
	 */
	public void addFavorite(ILaunchConfiguration configuration) {
		if (!fFavorites.contains(configuration)) {
			fFavorites.add(configuration);
		}
	}
	
	/**
	 * Returns the launch group associated with this history
	 * 
	 * @return group
	 */
	public ILaunchGroup getLaunchGroup() {
		return fGroup;
	}
	
	/**
	 * Returns whether the given configuration is included in the group
	 * associated with this launch history.
	 * 
	 * @param launch
	 * @return boolean
	 */
	public boolean accepts(ILaunchConfiguration configuration) {
		try {
			if (!LaunchConfigurationManager.isVisible(configuration)) {
				return false;
			}
			if (configuration.getType().supportsMode(getLaunchGroup().getMode())) {
				String launchCategory = null;
				launchCategory = configuration.getCategory();
				String category = getLaunchGroup().getCategory();
				if (launchCategory == null || category == null) {
					return launchCategory == category;
				}
				return category.equals(launchCategory);
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		return false;
	}	
	
	/**
	 * Notifies all launch histories that the launch history size has changed.
	 */
	public static void launchHistoryChanged() {
		for(Iterator iter = fgLaunchHistoryInstances.iterator(); iter.hasNext();) {
			((LaunchHistory) iter.next()).resizeHistory();		
		}
	}
	
	/**
	 * The max history size has changed - remove any histories if current
	 * collection is too long.
	 */
	protected void resizeHistory() {
		int max = getMaxHistorySize() + fFavorites.size();
		if (fCompleteHistory.size() > max) {
			fCompleteHistory.setSize(max);
		}
	}

	/**
	 * Returns the maximum number of entries allowed in this history
	 * 
	 * @return the maximum number of entries allowed in this history
	 */
	protected int getMaxHistorySize() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getInt(IDebugUIConstants.PREF_MAX_HISTORY_SIZE);
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		checkFavorites(configuration);
	}
	
	/**
	 * Adds the given config to the favorites list if it is a favorite, and
	 * returns whether the config was added to the favorites list.
	 * 
	 * @param configuration
	 * @return whether added to the favorites list
	 */
	protected boolean checkFavorites(ILaunchConfiguration configuration) {
		// update favorites
		if (configuration.isWorkingCopy()) {
			return false;
		}
		try {
			List favoriteGroups = configuration.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List)null);
			if (favoriteGroups == null) {
				// check deprecated attributes for backwards compatibility
				String groupId = getLaunchGroup().getIdentifier();
				boolean fav = false;
				if (groupId.equals(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP)) {
					fav = configuration.getAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, false);
				} else if (groupId.equals(IDebugUIConstants.ID_RUN_LAUNCH_GROUP)) {
					fav = configuration.getAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, false);
				}
				if (fav) {
					addFavorite(configuration);
					return true;
				} 
				removeFavorite(configuration);
				return false;
			} 
			else if (favoriteGroups.contains(getLaunchGroup().getIdentifier())) {
				addFavorite(configuration);
				return true;
			} 
			else {
				removeFavorite(configuration);
				return false;
			}
		} 
		catch (CoreException e) {}		
		return false;
	}
	
	/**
	 * Removes the given config from the favorites list, if needed.
	 * 
	 * @param configuration
	 */
	protected void removeFavorite(ILaunchConfiguration configuration) {
		fFavorites.remove(configuration);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		checkFavorites(configuration);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		ILaunchConfiguration newConfig = DebugPlugin.getDefault().getLaunchManager().getMovedTo(configuration);
		if (newConfig == null) {
			//deleted
			fCompleteHistory.remove(configuration);
			fFavorites.remove(configuration);
		} else {
			// moved/renamed
			int index = fCompleteHistory.indexOf(configuration);
			if (index >= 0) {
				fCompleteHistory.remove(index);
				fCompleteHistory.add(index, newConfig);
			} 
			index = fFavorites.indexOf(configuration);
			if (index >= 0) {
				fFavorites.remove(index);
				fFavorites.add(index, newConfig);
			}
			checkFavorites(newConfig);
		}
		fireLaunchHistoryChanged();
	}
}
