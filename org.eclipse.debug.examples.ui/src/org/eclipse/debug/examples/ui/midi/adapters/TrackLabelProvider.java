/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.midi.adapters;

import javax.sound.midi.Track;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunch;
import org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

/**
 * Provides labels for MIDI tracks.
 * 
 * @since 1.0
 */
public class TrackLabelProvider extends ElementLabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider#getLabel(org.eclipse.jface.viewers.TreePath, org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext, java.lang.String)
	 */
	protected String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Track track = (Track) elementPath.getLastSegment();
		MidiLaunch launch = (MidiLaunch) elementPath.getSegment(0);
		Track[] tracks = launch.getSequencer().getSequence().getTracks();
		int i = 0;
		for (i = 0; i < tracks.length; i++) {
			if (track.equals(tracks[i])) {
				break;
			}
		}
		StringBuffer buf = new StringBuffer();
		buf.append("Track ");
		buf.append(i);
		buf.append(" [");
		buf.append(track.size());
		buf.append(" events]");
		return buf.toString();
	}

}
