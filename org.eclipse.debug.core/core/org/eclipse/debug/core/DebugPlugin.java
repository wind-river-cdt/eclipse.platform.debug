/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.BreakpointManager;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.ExpressionManager;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.core.ListenerList;
import org.eclipse.debug.internal.core.LogicalStructureManager;
import org.eclipse.debug.internal.core.memory.MemoryBlockManager;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * There is one instance of the debug plug-in available from
 * <code>DebugPlugin.getDefault()</code>. The debug plug-in provides:
 * <ul>
 * <li>access to the breakpoint manager</li>
 * <li>access to the launch manager</li>
 * <li>access to the expression manager</li>
 * <li>access to the registered launcher extensions</li>
 * <li>debug event notification</li>
 * <li>status handlers</li>
 * </ul>
 * <p>
 * Clients may not instantiate or subclass this class.
 * </p>
 */
public class DebugPlugin extends Plugin {
	
	/**
	 * Unique identifier constant (value <code>"org.eclipse.debug.core"</code>)
	 * for the Debug Core plug-in.
	 */
	private static final String PI_DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"launchConfigurationTypes"</code>)
	 * for the launch configuration types extension point.
	 * 
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES= "launchConfigurationTypes"; //$NON-NLS-1$	
	
	/**
	 * Simple identifier constant (value <code>"launchConfigurationComparators"</code>)
	 * for the launch configuration comparators extension point.
	 * 
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_CONFIGURATION_COMPARATORS= "launchConfigurationComparators"; //$NON-NLS-1$		
	
	/**
	 * Simple identifier constant (value <code>"breakpoints"</code>) for the
	 * breakpoints extension point.
	 * 
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_BREAKPOINTS= "breakpoints";	 //$NON-NLS-1$
	
	/**
	 * Simple identifier constant (value <code>"statusHandlers"</code>) for the
	 * status handlers extension point.
	 * 
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_STATUS_HANDLERS= "statusHandlers";	 //$NON-NLS-1$	

	/**
	 * Simple identifier constant (value <code>"sourceLocators"</code>) for the
	 * source locators extension point.
	 * 
	 * @since 2.0
	 */
	public static final String EXTENSION_POINT_SOURCE_LOCATORS= "sourceLocators";	 //$NON-NLS-1$	
	
	/**
	 * Simple identifier constant (value <code>"launchModes"</code>) for the
	 * source modes extension point.
	 * 
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_MODES= "launchModes";	 //$NON-NLS-1$	
	
	/**
	 * Simple identifier constant (value <code>"launchDelegates"</code>) for the
	 * launch delegates extension point.
	 * 
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LAUNCH_DELEGATES= "launchDelegates";	 //$NON-NLS-1$
	
	/**
	 * Simple identifier constant (value <code>"processFactories"</code>) for the
	 * process factories extension point.
	 * 
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_PROCESS_FACTORIES = "processFactories"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"logicalStructureTypes"</code>) for the
	 * logical structure types extension point.
	 * 
	 * @since 3.0
	 */
	public static final String EXTENSION_POINT_LOGICAL_STRUCTURE_TYPES = "logicalStructureTypes"; //$NON-NLS-1$
	
	/**
	 * Simple identifier constant (value <code>"sourceContainerTypes"</code>) for the
	 * source container types extension point.
	 * 
	 * @since 3.0
	 */	
	public static final String EXTENSION_POINT_SOURCE_CONTAINER_TYPES = "sourceContainerTypes";	 //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"sourcePathComputers"</code>) for the
	 * source path computers extension point.
	 * 
	 * @since 3.0
	 */		
	public static final String EXTENSION_POINT_SOURCE_PATH_COMPUTERS = "sourcePathComputers"; //$NON-NLS-1$
	
	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 120;		

	/**
	 * Status code indicating that the Eclipse runtime does not support
	 * launching a program with a working directory. This feature is only
	 * available if Eclipse is run on a 1.3 runtime or higher.
	 * <p>
	 * A status handler may be registered for this error condition,
	 * and should return a Boolean indicating whether the program
	 * should be relaunched with the default working directory.
	 * </p>
	 */
	public static final int ERR_WORKING_DIRECTORY_NOT_SUPPORTED = 115;
	
	/**
	 * The launch configuration attribute that designates the process factory ID
	 * for the process factory to be used when creating a new process as a result of launching
	 * the launch configuration.
	 * @since 3.0
	 */
	public static final String ATTR_PROCESS_FACTORY_ID = "process_factory_id"; //$NON-NLS-1$
	
	/**
	 * The singleton debug plug-in instance.
	 */
	private static DebugPlugin fgDebugPlugin= null;

	/**
	 * The singleton breakpoint manager.
	 */
	private BreakpointManager fBreakpointManager;
	
	/**
	 * The singleton expression manager.
	 */
	private ExpressionManager fExpressionManager;	

	/**
	 * The singleton launch manager.
	 */
	private LaunchManager fLaunchManager;
	
	/**
	 * The collection of debug event listeners.
	 */
	private ListenerList fEventListeners;
	
	/**
	 * Event filters, or <code>null</code> if none.
	 */
	private ListenerList fEventFilters = null;

	/**
	 * Whether this plugin is in the process of shutting
	 * down.
	 */
	private boolean fShuttingDown= false;
	
	/**
	 * Whether event dispatch is in progress (if > 0)
	 * 
	 * @since 2.1
	 */
	private int fDispatching = 0;
	
	/**
	 * Queue of runnables to execute after event dispatch is
	 * complete.
	 * 
	 * @since 2.1
	 */
	private Vector fRunnables = null;
	
	/**
	 * Job that executes runnables
	 * 
	 * @since 3.0
	 */
	private AsynchJob fAsynchJob = null;
		
	/**
	 * Table of status handlers. Keys are {plug-in identifier, status code}
	 * pairs, and values are associated <code>IConfigurationElement</code>s.
	 */
	private HashMap fStatusHandlers = null;
	
	/**
	 * Map of process factories. Keys are process factory IDs
	 * and values are associated <code>IConfigurationElement</code>s.
	 * @since 3.0
	 */
	private HashMap fProcessFactories = null;
	
	/**
	 * Mode constants for the event notifier
	 */
	private static final int NOTIFY_FILTERS = 0;
	private static final int NOTIFY_EVENTS = 1;	
			
	/**
	 * Returns the singleton instance of the debug plug-in.
	 */
	public static DebugPlugin getDefault() {
		return fgDebugPlugin;
	}
	
	/**
	 * Sets the singleton instance of the debug plug-in.
	 * 
	 * @param plugin the debug plug-in, or <code>null</code>
	 *  when shutting down
	 */
	private static void setDefault(DebugPlugin plugin) {
		fgDebugPlugin = plugin;
	}
	
	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		return PI_DEBUG_CORE;
	}

	/**
	 * Constructs the debug plug-in.
	 * <p>
	 * An instance of this plug-in runtime class is automatically created 
	 * when the facilities provided by this plug-in are required.
	 * <b>Clients must never explicitly instantiate a plug-in runtime class.</b>
	 * </p>
	 */
	public DebugPlugin() {
		super();
		setDefault(this);
	}
	
	/**
	 * Adds the given listener to the collection of registered debug
	 * event listeners. Has no effect if an identical listener is already
	 * registered.
	 *
	 * @param listener the listener to add
	 * @since 2.0
	 */
	public void addDebugEventListener(IDebugEventSetListener listener) {
		if (fEventListeners == null) {
			fEventListeners = new ListenerList(20);
		}
		fEventListeners.add(listener);
	}	
	
	/**
	 * Notifies all registered debug event set listeners of the given
	 * debug events. Events which are filtered by a registered debug event
	 * filter are not fired.
	 * 
	 * @param events array of debug events to fire
	 * @see IDebugEventFilter
	 * @see IDebugEventSetListener
	 * @since 2.0
	 */
	public void fireDebugEventSet(DebugEvent[] events) {
		if (isShuttingDown() || events == null || fEventListeners == null)
			return;
		getEventNotifier().dispatch(events);
	}
	
	/**
	 * Asynchronously executes the given runnable in a seperate
	 * thread, after debug event dispatch has completed. If debug
	 * events are not currently being dispatched, the runnable is
	 * scheduled to run in a seperate thread immediately.
	 * 
	 * @param r runnable to execute asynchronously
	 * @since 2.1
	 */
	public void asyncExec(Runnable r) {
		if (fRunnables == null) {
			// initialize runnables and async job
			fRunnables= new Vector(5);
			fAsynchJob = new AsynchJob();
		}
		fRunnables.add(r);
		if (!isDispatching()) {
			fAsynchJob.schedule();
		} 
	}
	
	/**
	 * Returns the breakpoint manager.
	 *
	 * @return the breakpoint manager
	 * @see IBreakpointManager
	 */
	public IBreakpointManager getBreakpointManager() {
		if (fBreakpointManager == null) {
			fBreakpointManager = new BreakpointManager();
		}
		return fBreakpointManager;
	}
	
	/**
	 * Returns the launch manager.
	 *
	 * @return the launch manager
	 * @see ILaunchManager
	 */
	public ILaunchManager getLaunchManager() {
		if (fLaunchManager == null) {
			fLaunchManager = new LaunchManager();
		}
		return fLaunchManager;
	}
	
	/**
	 * Returns the status handler registered for the given
	 * status, or <code>null</code> if none.
	 *
	 * @return the status handler registered for the given
	 *  status, or <code>null</code> if none
	 * @since 2.0
	 */
	public IStatusHandler getStatusHandler(IStatus status) {
		StatusHandlerKey key = new StatusHandlerKey(status.getPlugin(), status.getCode());
		if (fStatusHandlers == null) {
			initializeStatusHandlers();
		}
		IConfigurationElement config = (IConfigurationElement)fStatusHandlers.get(key);
		if (config != null) {
			try {
				Object handler = config.createExecutableExtension("class"); //$NON-NLS-1$
				if (handler instanceof IStatusHandler) {
					return (IStatusHandler)handler;
				} else {
					invalidStatusHandler(null, MessageFormat.format(DebugCoreMessages.getString("DebugPlugin.Registered_status_handler_{0}_does_not_implement_required_interface_IStatusHandler._1"), new String[] {config.getDeclaringExtension().getUniqueIdentifier()})); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				log(e);
			}
		}
		return null;
	}	
	
	/**
	 * Returns the expression manager.
	 *
	 * @return the expression manager
	 * @see IExpressionManager
	 * @since 2.0
	 */
	public IExpressionManager getExpressionManager() {
		if (fExpressionManager == null) {
			fExpressionManager = new ExpressionManager();
		}
		return fExpressionManager;
	}	
		
	/**
	 * Removes the given listener from the collection of registered debug
	 * event listeners. Has no effect if an identical listener is not already
	 * registered.
	 *
	 * @param listener the listener to remove
	 * @since 2.0
	 */
	public void removeDebugEventListener(IDebugEventSetListener listener) {
		if (fEventListeners != null) {
			fEventListeners.remove(listener);
		}
	}	

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			setShuttingDown(true);
		
			if (fAsynchJob != null) {
				fAsynchJob.cancel();
			}
			if (fLaunchManager != null) {
				fLaunchManager.shutdown();
			}
			if (fBreakpointManager != null) {
				fBreakpointManager.shutdown();
			}
			MemoryBlockManager.pluginShutdown();
			if (fEventListeners != null) {
				fEventListeners.removeAll();
			}
			SourceLookupUtils.shutdown();
			setDefault(null);
			ResourcesPlugin.getWorkspace().removeSaveParticipant(this);
		} finally {
			super.stop(context);
		}
	}
	
	/**
	 * Creates and returns a new process representing the given
	 * <code>java.lang.Process</code>. A streams proxy is created
	 * for the I/O streams in the system process. The process
	 * is added to the given launch.
	 *
	 * @param launch the launch the process is contained in
	 * @param process the system process to wrap
	 * @param label the label assigned to the process
	 * @return the process
	 * @see IProcess
	 */
	public static IProcess newProcess(ILaunch launch, Process process, String label) {
		return newProcess(launch, process, label, null);
	}
	
	/**
	 * Creates and returns a new process representing the given
	 * <code>java.lang.Process</code>. A streams proxy is created
	 * for the I/O streams in the system process. The process
	 * is added to the given launch, and the process is initialized
	 * with the given attribute map.
	 * 
	 * The process will be created by the <code>IProcessFactory<code> if it has been 
	 * designated via the org.eclipse.debug.core.processFactories extension point for the
	 * process factory id indicated in the launch configuration associated with the launch.
	 * 
	 * @param launch the launch the process is contained in
	 * @param process the system process to wrap
	 * @param label the label assigned to the process
	 * @param attributes initial values for the attribute map
	 * @return the process <code>null</code> can be returned if errors occur dealing with the process factory
	 * designated to create the process.
	 * @see IProcess
	 * @since 2.1
	 */
	public static IProcess newProcess(ILaunch launch, Process process, String label, Map attributes) {
		ILaunchConfiguration config= launch.getLaunchConfiguration();
		String processFactoryID= null;
		if (config != null) {
			try {
				processFactoryID= config.getAttribute(ATTR_PROCESS_FACTORY_ID, (String)null);
			} catch (CoreException e) {
			}
		}
		if (processFactoryID != null) {
			DebugPlugin plugin= DebugPlugin.getDefault();
			if (plugin.fProcessFactories == null) {
				plugin.initializeProcessFactories();
			}
			IConfigurationElement element= (IConfigurationElement) plugin.fProcessFactories.get(processFactoryID);
			if (element == null) {
				return null;
			}
			IProcessFactory processFactory= null;
			try {
				processFactory = (IProcessFactory)element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException exception) {
				log(exception);
				return null;
			}
			return processFactory.newProcess(launch, process, label, attributes);
		} else {
			return new RuntimeProcess(launch, process, label, attributes);
		}
	}	
	
	/**
	 * Returns any logical structure types that have been contributed for the given
	 * value.
	 * 
	 * @param value the value for which logical structure types have been requested
	 * @return logical structure types that have been contributed for the given
	 * value, possibly an empty collection
	 * 
	 * @since 3.0
	 */
	public static ILogicalStructureType[] getLogicalStructureTypes(IValue value) {
		return LogicalStructureManager.getDefault().getLogicalStructureTypes(value);
	}
	
	/**
	 * Convenience method that performs a runtime exec on the given command line
	 * in the context of the specified working directory, and returns the
	 * resulting process. If the current runtime does not support the
	 * specification of a working directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if the
	 * exec should be re-executed without specifying a working directory.
	 * 
	 * @param cmdLine the command line
	 * @param workingDirectory the working directory, or <code>null</code>
	 * @return the resulting process or <code>null</code> if the exec is
	 *  cancelled
	 * @see Runtime
	 * 
	 * @since 2.1
	 */
	public static Process exec(String[] cmdLine, File workingDirectory) throws CoreException {
		return exec(cmdLine, workingDirectory, null);
	}

	/**
	 * Convenience method that performs a runtime exec on the given command line
	 * in the context of the specified working directory, and returns the
	 * resulting process. If the current runtime does not support the
	 * specification of a working directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if the
	 * exec should be re-executed without specifying a working directory.
	 * 
	 * @param cmdLine the command line
	 * @param workingDirectory the working directory, or <code>null</code>
	 * @param envp the environment variables set in the process, or <code>null</code>
	 * @return the resulting process or <code>null</code> if the exec is
	 *  cancelled
	 * @see Runtime
	 * 
	 * @since 3.0
	 */
	public static Process exec(String[] cmdLine, File workingDirectory, String[] envp) throws CoreException {
		Process p= null;
		try {

			if (workingDirectory == null) {
				p= Runtime.getRuntime().exec(cmdLine, envp);
			} else {
				p= Runtime.getRuntime().exec(cmdLine, envp, workingDirectory);
			}
		} catch (IOException e) {
				if (p != null) {
					p.destroy();
				}
				Status status = new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, DebugCoreMessages.getString("DebugPlugin.Exception_occurred_executing_command_line._1"), e); //$NON-NLS-1$
				throw new CoreException(status);
		} catch (NoSuchMethodError e) {
			//attempting launches on 1.2.* - no ability to set working directory			
			IStatus status = new Status(IStatus.ERROR, getUniqueIdentifier(), ERR_WORKING_DIRECTORY_NOT_SUPPORTED, DebugCoreMessages.getString("DebugPlugin.Eclipse_runtime_does_not_support_working_directory_2"), e); //$NON-NLS-1$
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
			
			if (handler != null) {
				Object result = handler.handleStatus(status, null);
				if (result instanceof Boolean && ((Boolean)result).booleanValue()) {
					p= exec(cmdLine, null);
				}
			}
		}
		return p;
	}	
	
	/**
	 * Returns whether this plug-in is in the process of 
	 * being shutdown.
	 * 
	 * @return whether this plug-in is in the process of 
	 *  being shutdown
	 */
	private boolean isShuttingDown() {
		return fShuttingDown;
	}
	
	/**
	 * Sets whether this plug-in is in the process of 
	 * being shutdown.
	 * 
	 * @param value whether this plug-in is in the process of 
	 *  being shutdown
	 */
	private void setShuttingDown(boolean value) {
		fShuttingDown = value;
	}
	
	/**
	 * Returns the collection of debug event listeners registered
	 * with this plug-in.
	 * 
	 * @return list of registered debug event listeners, instances
	 *  of <code>IDebugEventSetListeners</code>
	 */
	private Object[] getEventListeners() {
		return fEventListeners.getListeners();
	}
	
	/**
	 * Adds the given debug event filter to the registered
	 * event filters. Has no effect if an identical filter
	 * is already registerd.
	 * 
	 * @param filter debug event filter
	 * @since 2.0
	 */
	public void addDebugEventFilter(IDebugEventFilter filter) {
		if (fEventFilters == null) {
			fEventFilters = new ListenerList(2);
		}
		fEventFilters.add(filter);
	}
	
	/**
	 * Removes the given debug event filter from the registered
	 * event filters. Has no effect if an identical filter
	 * is not already registered.
	 * 
	 * @param filter debug event filter
	 * @since 2.0
	 */
	public void removeDebugEventFilter(IDebugEventFilter filter) {
		if (fEventFilters != null) {
			fEventFilters.remove(filter);
			if (fEventFilters.size() == 0) {
				fEventFilters = null;
			}
		}
	}	
	
	/**
	 * Logs the given message if in debug mode.
	 * 
	 * @param message the message to log
	 * @since 2.0
	 */
	public static void logDebugMessage(String message) {
		if (getDefault().isDebugging()) {
			// this message is intentionally not internationalized, as an exception may
			// be due to the resource bundle itself
			log(new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, "Internal message logged from Debug Core: " + message, null)); //$NON-NLS-1$
		}
	}
	
	/**
	 * Logs the given message with this plug-in's log and the given
	 * throwable or <code>null</code> if none.
	 * @param message the message to log
	 * @param throwable the exception that occurred or <code>null</code> if none
	 */
	public static void logMessage(String message, Throwable throwable) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, message, throwable));
	}
	
	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 * @since 2.0
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 * @since 2.0
	 */
	public static void log(Throwable t) {
		IStatus status= new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, "Error logged from Debug Core: ", t); //$NON-NLS-1$
		log(status);
	}
	
	/**
	 * Register status handlers.
	 * 
	 */
	private void initializeStatusHandlers() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.PI_DEBUG_CORE, EXTENSION_POINT_STATUS_HANDLERS);
		IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
		fStatusHandlers = new HashMap(infos.length);
		for (int i= 0; i < infos.length; i++) {
			IConfigurationElement configurationElement = infos[i];
			String id = configurationElement.getAttribute("plugin"); //$NON-NLS-1$
			String code = configurationElement.getAttribute("code"); //$NON-NLS-1$
			
			if (id != null && code != null) {
				try {
					StatusHandlerKey key = new StatusHandlerKey(id, Integer.parseInt(code));
					fStatusHandlers.put(key, configurationElement);
				} catch (NumberFormatException e) {
					// invalid status handler
					invalidStatusHandler(e, configurationElement.getAttribute("id")); //$NON-NLS-1$
				}
			} else {
				// invalid status handler
				invalidStatusHandler(null, configurationElement.getAttribute("id")); //$NON-NLS-1$
			}
		}			
	}
	
	/**
	 * Register process factories.
	 * 
	 */
	private void initializeProcessFactories() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.PI_DEBUG_CORE, EXTENSION_POINT_PROCESS_FACTORIES);
		IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
		fProcessFactories = new HashMap(infos.length);
		for (int i= 0; i < infos.length; i++) {
			IConfigurationElement configurationElement = infos[i];
			String id = configurationElement.getAttribute("id"); //$NON-NLS-1$
			String clss = configurationElement.getAttribute("class"); //$NON-NLS-1$
			if (id != null && clss != null) {
					fProcessFactories.put(id, configurationElement);
			} else {
				// invalid process factory
				String badDefiner= infos[i].getDeclaringExtension().getNamespace();
				log(new Status(IStatus.ERROR, DebugPlugin.PI_DEBUG_CORE, INTERNAL_ERROR, MessageFormat.format(DebugCoreMessages.getString("DebugPlugin.31"), new String[] {badDefiner, id}), null)); //$NON-NLS-1$
			}
		}			
	}
	
	private void invalidStatusHandler(Exception e, String id) {
		log(new Status(IStatus.ERROR, DebugPlugin.PI_DEBUG_CORE, INTERNAL_ERROR, MessageFormat.format(DebugCoreMessages.getString("DebugPlugin.Invalid_status_handler_extension__{0}_2"), new String[] {id}), e)); //$NON-NLS-1$
	}
	
	/**
	 * Key for status handler extensions - a plug-in identifier/code pair
	 */
	class StatusHandlerKey {
		
		String fPluginId;
		int fCode;
		
		StatusHandlerKey(String pluginId, int code) {
			fPluginId = pluginId;
			fCode = code;
		}
		
		public int hashCode() {
			return fPluginId.hashCode() + fCode;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof StatusHandlerKey) {
				StatusHandlerKey s = (StatusHandlerKey)obj;
				return fCode == s.fCode && fPluginId.equals(s.fPluginId);
			}
			return false;
		}
	}

	/**
	 * Returns whether any event filters are registered
	 * 
	 * @return whether any event filters are registered
	 */
	private boolean hasEventFilters() {
		return fEventFilters != null && fEventFilters.size() > 0;
	}
	
	/**
	 * Sets whether debug events are being dispatched 
	 */
	private synchronized void setDispatching(boolean dispatching) {
		if (dispatching) {
			fDispatching++;
		} else {
			fDispatching--;
		}
		if (!isDispatching()) {
			if (fAsynchJob != null) {
				fAsynchJob.schedule();
			}
		}
	}
	
	/**
	 * Returns whether debug events are being dispatched
	 */
	private synchronized boolean isDispatching() {
		return fDispatching > 0;
	}	
	
	/**
	 * Executes runnables after event dispatch is complete.
	 * 
	 * @since 3.0
	 */
	class AsynchJob extends Job {
		
		public AsynchJob() {
			super(DebugCoreMessages.getString("DebugPlugin.Debug_async_queue_1")); //$NON-NLS-1$
			setPriority(Job.INTERACTIVE);
			setSystem(true);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#shouldRun()
		 */
		public boolean shouldRun() {
			return !fShuttingDown && !fRunnables.isEmpty();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus run(IProgressMonitor monitor) {
			// Executes runnables and empties the queue
			Vector v = null;
			synchronized (fRunnables) {
				v = fRunnables;
				fRunnables = new Vector(5);
			}
			monitor.beginTask(DebugCoreMessages.getString("DebugPlugin.Debug_async_queue_1"), v.size()); //$NON-NLS-1$
			Iterator iter = v.iterator();
			while (iter.hasNext() && !fShuttingDown && !monitor.isCanceled()) {
				Runnable r = (Runnable)iter.next();
				try {
					r.run();
				} catch (Throwable t) {
					log(t);
				}
				monitor.worked(1);
			}
			return Status.OK_STATUS;
		}

	}
		
	/**
	 * Returns an event notifier.
	 * 
	 * @return an event notifier
	 */
	private EventNotifier getEventNotifier() {
		return new EventNotifier();
	}
	
	/**
	 * Filters and dispatches events in a safe runnable to handle any
	 * exceptions.
	 */
	class EventNotifier implements ISafeRunnable {
		
		private DebugEvent[] fEvents;
		private IDebugEventSetListener fListener;
		private IDebugEventFilter fFilter;
		private int fMode;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			switch (fMode) {
				case NOTIFY_FILTERS:
					IStatus status = new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, DebugCoreMessages.getString("DebugPlugin.An_exception_occurred_while_filtering_debug_events._3"), exception); //$NON-NLS-1$
					log(status);
					break;
				case NOTIFY_EVENTS:				
					status = new Status(IStatus.ERROR, getUniqueIdentifier(), INTERNAL_ERROR, DebugCoreMessages.getString("DebugPlugin.An_exception_occurred_while_dispatching_debug_events._2"), exception); //$NON-NLS-1$
					log(status);
					break;
			}
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			switch (fMode) {
				case NOTIFY_FILTERS:
					fEvents = fFilter.filterDebugEvents(fEvents);
					break;
				case NOTIFY_EVENTS:
					fListener.handleDebugEvents(fEvents);
					break;
			}
		}
		
		/**
		 * Filter and dispatch the given events. If an exception occurs in one
		 * listener, events are still fired to subsequent listeners.
		 * 
		 * @param events debug events
		 */
		public void dispatch(DebugEvent[] events) {
			fEvents = events;
			try {
				setDispatching(true);
				
				if (hasEventFilters()) {
					fMode = NOTIFY_FILTERS;
					Object[] filters = fEventFilters.getListeners();
					for (int i = 0; i < filters.length; i++) {
						fFilter = (IDebugEventFilter)filters[i];
						Platform.run(this);
						if (fEvents == null || fEvents.length == 0) {
							return;
						}
					}	
				}				
				
				fMode = NOTIFY_EVENTS;
				Object[] listeners= getEventListeners();
				for (int i= 0; i < listeners.length; i++) {
					fListener = (IDebugEventSetListener)listeners[i]; 
					Platform.run(this);
				}
				
			} finally {
				setDispatching(false);
			}
			fEvents = null;
			fFilter = null;
			fListener = null;			
		}

	}

	/**
	 * Creates and returns a new XML document.
	 * 
	 * @return a new XML document
	 * @throws CoreException if unable to create a new document
	 * @since 3.0
	 */
	public static Document newDocument()throws CoreException {
		try {
			return LaunchManager.getDocument();
		} catch (ParserConfigurationException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.3"), e); //$NON-NLS-1$
		}		
		return null;
	}	
	
	/**
	 * Serializes the given XML document into a string.
	 * 
	 * @param document XML document to serialize
	 * @return a string representing the given document
	 * @throws CoreException if unable to serialize the document
	 * @since 3.0
	 */
	public static String serializeDocument(Document document) throws CoreException {
		try {
			return LaunchManager.serializeDocument(document);
		} catch (TransformerException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.4"), e); //$NON-NLS-1$
		} catch (IOException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.5"), e); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Parses the given string representing an XML document, returning its
	 * root element.
	 * 
	 * @param document XML document as a string
	 * @return the document's root element
	 * @throws CoreException if unable to parse the document
	 * @since 3.0
	 */
	public static Element parseDocument(String document) throws CoreException {
		Element root = null;
		InputStream stream = null;
		try{		
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			stream = new ByteArrayInputStream(document.getBytes());
			root = parser.parse(stream).getDocumentElement();
		} catch (ParserConfigurationException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.6"), e); //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.7"), e); //$NON-NLS-1$
		} catch (SAXException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.8"), e); //$NON-NLS-1$
		} catch (IOException e) {
			abort(SourceLookupMessages.getString("SourceLookupUtils.9"), e); //$NON-NLS-1$
		} finally { 
			try{
				stream.close();
			} catch(IOException e) {
				abort(SourceLookupMessages.getString("SourceLookupUtils.10"), e); //$NON-NLS-1$
			}
		}		
		return root;
	}	
	
	/**
	 * Throws an exception with the given message and underlying exception.
	 * 
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, message, exception);
		throw new CoreException(status);
	}	
}


