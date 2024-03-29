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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * @see PDAStackCommand
 */

public class PDAStackCommandResult extends PDACommandResult {
    
    /**
     * Array of frames return by the stack commands.  The frames are ordered 
     * with the highest-level frame first.
     */
    final public PDAFrameData[] fFrames;
    
    PDAStackCommandResult(String response) {
        super(response);
        StringTokenizer st = new StringTokenizer(response, "#");
        List framesList = new ArrayList();
        
        while (st.hasMoreTokens()) {
            framesList.add(new PDAFrameData(st.nextToken()));
        }
        fFrames = (PDAFrameData[])framesList.toArray(new PDAFrameData[framesList.size()]);
    }
}
