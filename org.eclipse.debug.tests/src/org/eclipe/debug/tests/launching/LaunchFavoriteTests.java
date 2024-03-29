/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.launching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Test the launch history favorites get updated properly as configurations as
 * modified.
 * 
 * @since 3.6
 */
public class LaunchFavoriteTests extends AbstractLaunchTest {
	
	/**
	 * Configuration to use for the test. One is created for each test during 
	 * setup and deleted during tear down.
	 */
	private ILaunchConfiguration fConfig;

	/**
	 * Constructor
	 * @param name
	 */
	public LaunchFavoriteTests(String name) {
		super(name);
	}
	
	/**
	 * Returns the run launch history
	 * @return
	 */
	private LaunchHistory getRunLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
	}
		
	/**
	 * Returns the debug launch history
	 * @return
	 */
	private LaunchHistory getDebugLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		// clear the favorites
		getRunLaunchHistory().setFavorites(new ILaunchConfiguration[0]);
		getDebugLaunchHistory().setFavorites(new ILaunchConfiguration[0]);
		fConfig = getLaunchConfiguration(getName());
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		// delete the configuration used during this test
		ILaunchConfiguration configuration = getLaunchConfiguration();
		if (configuration.exists()) {
			configuration.delete();
		}
	}
	
	/**
	 * Returns the new/initial launch configuration to use for this test.
	 * 
	 * @return
	 */
	private ILaunchConfiguration getLaunchConfiguration() {
		return fConfig;
	}
	
	/**
	 * Makes the configuration a favorite in the specified group.
	 * 
	 * @param config configuration to make a favorite - may be a working copy already
	 * @param groupId group to add it to
	 * @return working copy after making it a favorite
	 * 
	 * @throws CoreException
	 */
	private ILaunchConfigurationWorkingCopy addFavorite(ILaunchConfiguration config, String groupId) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = getWorkingCopy(config);
		List list = config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List)null);
		if (list == null) {
			list = new ArrayList();
		}
		list.add(groupId);
		wc.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, list);
		return wc;
	}
	
	/**
	 * Removes the favorite group from the configuration's favorites attribute. Returns
	 * the resulting working copy.
	 * 
	 * @param config configuration to remove favorite attribute from, may already be a working copy
	 * @param groupId group to remove
	 * @return working copy after removing favorite
	 * @throws CoreException
	 */
	private ILaunchConfigurationWorkingCopy removeFavorite(ILaunchConfiguration config, String groupId) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = getWorkingCopy(config);
		List list = config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List)null);
		if (list != null) {
			if (list.remove(groupId)) {
				if (list.isEmpty()) {
					list = null;
				}
				wc.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, list);
			}
		}
		return wc;
	}
	
	private ILaunchConfigurationWorkingCopy getWorkingCopy(ILaunchConfiguration config) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (config.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) config;
		} else {
			wc = config.getWorkingCopy();
		}
		return wc;
	}
	
	/**
	 * Returns whether the launch history contains the given configuration as a favorite and is 
	 * the expected size.
	 * 
	 * @param history launch history
	 * @param configuration launch configuration
	 * @param size expected size of favorites or -1 if unknown
	 * @return whether the launch history contains the given configuration as a favorite and is 
	 * 	the expected size
	 */
	private boolean containsFavorite(LaunchHistory history, ILaunchConfiguration configuration, int size) {
		ILaunchConfiguration[] favorites = history.getFavorites();
		assertNotNull("No favorites", favorites);
		if (size != -1) {
			assertEquals("Favorites wrong size", size, favorites.length);
		}
		for (int i = 0; i < favorites.length; i++) {
			if (configuration.equals(favorites[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Tests that a configuration becoming a favorite appears in the favorites.
	 * 
	 * @throws CoreException
	 */
	public void testBecomeFavorite() throws CoreException {
		ILaunchConfigurationWorkingCopy wc = addFavorite(getLaunchConfiguration(), IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
		addFavorite(wc, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		ILaunchConfiguration saved = wc.doSave();
		assertTrue("Missing from debug favorites", containsFavorite(getDebugLaunchHistory(), saved, 1));
		assertTrue("Missing from run favorites", containsFavorite(getRunLaunchHistory(), saved, 1));
	}
	
	/**
	 * Tests that a when a favorite is no longer a favorite, it gets removed from the favorites.
	 * 
	 * @throws CoreException
	 */
	public void testUnFavorite() throws CoreException {
		testBecomeFavorite(); // create favorite in two histories
		ILaunchConfigurationWorkingCopy wc = removeFavorite(getLaunchConfiguration(), IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
		removeFavorite(wc, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		ILaunchConfiguration saved = wc.doSave();
		assertFalse("Should not be a debug favorite", containsFavorite(getDebugLaunchHistory(), saved, 0));
		assertFalse("Should not be a run favorite", containsFavorite(getRunLaunchHistory(), saved, 0));
	}
	
	/**
	 * When a configuration is deleted, it should no longer be in the list.
	 * 
	 * @throws CoreException
	 */
	public void testDeleteConfiguration() throws CoreException {
		testBecomeFavorite(); // create a favorite in two histories
		ILaunchConfiguration configuration = getLaunchConfiguration();
		configuration.delete();
		assertFalse("Should not be a debug favorite", containsFavorite(getDebugLaunchHistory(), configuration, 0));
		assertFalse("Should not be a run favorite", containsFavorite(getRunLaunchHistory(), configuration, 0));
	}
	
	/**
	 * When a favorite is renamed, it should still be in the list, with the new name.
	 * 
	 * @throws CoreException
	 */
	public void testRenameFavorite() throws CoreException {
		testBecomeFavorite();
		ILaunchConfiguration original = getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy copy = original.getWorkingCopy();
		copy.rename("rename"+original.getName());
		ILaunchConfiguration saved = copy.doSave();
		assertTrue("Missing from debug favorites", containsFavorite(getDebugLaunchHistory(), saved, 1));
		assertTrue("Missing from run favorites", containsFavorite(getRunLaunchHistory(), saved, 1));
		saved.delete();
	}
	
	/**
	 * Renaming a configuration and making it a favorite at the same time - should appear in the list.
	 * 
	 * @throws CoreException
	 */
	public void testRenameBecomeFavorite() throws CoreException {
		ILaunchConfiguration original = getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy copy = original.getWorkingCopy();
		copy.rename("rename"+original.getName());
		addFavorite(copy, IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
		addFavorite(copy, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		ILaunchConfiguration saved = copy.doSave();
		assertTrue("Missing from debug favorites", containsFavorite(getDebugLaunchHistory(), saved, 1));
		assertTrue("Missing from run favorites", containsFavorite(getRunLaunchHistory(), saved, 1));
		saved.delete();
	}
	
	/**
	 * Renaming a configuration and removing its favorite status should remove it from the list.
	 *  
	 * @throws CoreException
	 */
	public void testRenameUnFavorite() throws CoreException {
		testBecomeFavorite();
		ILaunchConfiguration original = getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy copy = original.getWorkingCopy();
		copy.rename("rename"+original.getName());
		removeFavorite(copy, IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
		removeFavorite(copy, IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		ILaunchConfiguration saved = copy.doSave();
		assertFalse("Should not be a debug favorite", containsFavorite(getDebugLaunchHistory(), saved, 0));
		assertFalse("Should not be a run favorite", containsFavorite(getRunLaunchHistory(), saved, 0));
		saved.delete();
	}
}
