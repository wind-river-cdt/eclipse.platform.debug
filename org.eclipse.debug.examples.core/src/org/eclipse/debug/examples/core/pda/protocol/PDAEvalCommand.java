/*******************************************************************************
 * Copyright (c) 2008, 2009 Wind River Systems and others.
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
 * Causes the interperter to execute the given set of instructions.  At the end 
 * of the evaluation the top value is poped off the stack and returned in the
 * evaluation result.
 * 
 * <pre>
 *    C: eval {thread_id} {instruction}%20{parameter}|{instruction}%20{parameter}|...
 *    R: ok
 *    E: resumed {thread_id} client
 *    E: evalresult result
 *    E: suspended {thread_id} eval
 *    
 * Errors:
 *    error: invalid thread
 *    error: cannot evaluate while vm is suspended
 *    error: thread running        
 * </pre>
 * 
 * Where event_name could be <code>unimpinstr</code> or <code>nosuchlabel</code>.  
 */
public class PDAEvalCommand extends PDACommand {

    public PDAEvalCommand(int threadId, String operation) {
        super("eval " + threadId + " " + operation);
    }
    
    public PDACommandResult createResult(String resultText) {
        return new PDACommandResult(resultText);
    }
}
