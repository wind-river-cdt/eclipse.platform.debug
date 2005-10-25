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

package org.eclipse.debug.internal.ui.importexport.breakpoints;

/**
 *
 * XML tag constants for importing and exporting breakpoints
 */
public interface IImportExportConstants {

	/**
	 * <p>
	 * The name for the top level node in the XMLMemento for storing/restoring breakpoint information.</br>
	 * 
	 * General formulation of the XMLMemento is as follows:</br>
	 * 
	 * breakpoints := (breakpoint)*</br>
	 * 
	 * breakpoint := resource</br>
	 * 
	 * resource := (marker)+</br>
	 * 
	 * marker := (attribs)+</br>
	 * </p>	
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 * 
	 */
	public static final String IE_NODE_BREAKPOINTS = "breakpoints"; //$NON-NLS-1$
	
	/**
	 * The name of the node type for each of the imported or exported breakpoints
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_BREAKPOINT = "breakpoint"; //$NON-NLS-1$
	
	/**
	 * The ID for the classname node for exporting breakpoints from the wizards.
	 * This ID is searchable from the listing of XMLMementos gathered from the BreakPointManager.
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 * @see XMLMemento->getChild(String)
	 */
	public static final String IE_NODE_CLASSNAME = "classname"; //$NON-NLS-1$
	
	/**
	 * <p>
	 * The generalized ID for each of the values stored in a markers' attribute map.
	 * Since a marker can have any number of attributes and or values, we use a 
	 * (name, value) paring in the XMLmemento to store them, without having a dependence upon what the attribute
	 * is or what type it is.
	 * </p>
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_VALUE = "value"; //$NON-NLS-1$
	
	/**
	 * Each breakpoint has an associated resource, which is described with this element
	 * name
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_RESOURCE = "resource"; //$NON-NLS-1$
	
	/**
	 * To validate the resource when filtering importable breakpoints we need to know its path.
	 * This is the the name of XMLMemento node that stores that path.
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_PATH = "path"; //$NON-NLS-1$
	
	/**
	 * To filter the type of path searched for within the workspace to allow for the filtering of 
	 * breakpoints for import, we need to know the type to filter for.
	 * 
	 * This is the name of the XMLMemento node that stores the type of the resource
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_TYPE = "type"; //$NON-NLS-1$
	
	/**
	 * The name for the marker node in the corresponding XMLMemento
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_MARKER = "marker"; //$NON-NLS-1$
	
	/**
	 * The name for a marker attribute node in the corresponding XMLMemento
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_ATTRIB = "attrib"; //$NON-NLS-1$
	
	/**
	 * The generalized name for the "name" node used in marker attribute nodes.
	 * This is the other ID in the (name, value) pairing used to describe attributes
	 * of markers.
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_NODE_NAME = "name"; //$NON-NLS-1$
	
	/**
	 * The name of the enabled attribute, which is part of the breakpoint node information
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_BP_ENABLED = "enabled"; //$NON-NLS-1$
	
	/**
	 * The name of the registered attribute, whic is part of the breakpoint node information
	 * 
	 * @see WizardExportBreakpointsPage
	 * @see WizardImportBreakpointsPage
	 */
	public static final String IE_BP_REGISTERED = "registered"; //$NON-NLS-1$
	
	/**
	 * The name of the persistant attribute for breakpoint information
	 */
	public static final String IE_BP_PERSISTANT = "persistant"; //$NON-NLS-1$
	
	/**
	 * The default file extension for breakpoint export files
	 */
	public static final String EXTENSION = "bkpt";  //$NON-NLS-1$
	
	/**
	 * the charstart attribute from a marker
	 */
	public static final String CHARSTART = "charStart"; //$NON-NLS-1$
	
	/**
	 * the name of the workingsets node
	 */
	public static final String IE_NODE_WORKINGSETS = "workingsets";  //$NON-NLS-1$
	
	/**
	 * The name of the workingset node
	 */
	public static final String IE_NODE_WORKINGSET = "workingset"; //$NON-NLS-1$
	
	/**
	 * The name of the id attribute, reusable for a general attribute called id
	 */
	public static final String IE_NODE_ID = "id"; //$NON-NLS-1$
	
	/**
	 * The delimiter for the listing of working sets that a marker belongs to
	 */
	public static final String DELIMITER = "<;#>"; //$NON-NLS-1$
}//end interface