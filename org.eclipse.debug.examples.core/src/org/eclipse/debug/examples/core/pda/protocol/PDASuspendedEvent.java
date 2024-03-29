/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;

/**
 * Suspended event generated when a thread is suspended.
 * 
 * <pre>
 *    E: suspended {thread_id} [reason]
 * </pre>
 * 
 * <code>[reason]</code> is the cause of the suspension and it's optional:
 * <ul>
 *   <li><code>breakpoint N</code> - a breakpoint at line <code>N</code> was hit</li>
 *   <li><code>client</code> - a client request to suspend has completed</li>
 *   <li><code>drop</code> - a client request to drop a frame has completed</li>
 *   <li><code>event E</code> - an error was encountered, where <code>E</code> is one
 *       of <code>unimpinstr</code> or <code>nosuchlabel</code></li>
 *   <li><code>step</code> - a step request has completed</li>
 *   <li><code>watch A F::V</code> - a watchpoint was hit for reason <code>A</code>
 *       (<code>read</code> or <code>write</code>), on variable <code>V</code> in
 *       function <code>F</code></li>
 * </ul>

 */
public class PDASuspendedEvent extends PDARunControlEvent {
    
    public PDASuspendedEvent(String message) {
        super(message);
    }
    
    public static boolean isEventMessage(String message) {
        return message.startsWith("suspended");
    }
}
