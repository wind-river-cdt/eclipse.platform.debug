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
package org.eclipse.debug.examples.ui.midi.detailpanes;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.examples.core.midi.launcher.ClockControl;
import org.eclipse.debug.examples.core.midi.launcher.TempoControl;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Creates detail panes for sequencer controls.
 * 
 * @since 1.0
 */
public class ControlDetailPaneFactory implements IDetailPaneFactory {
	
	/**
	 * Identifier for the tempo slider detail pane
	 */
	public static final String ID_TEMPO_SLIDER = "TEMPO_SLIDER";
	
	/**
	 * Identifier for the clock slider detail pane
	 */
	public static final String ID_CLOCK_SLIDER = "CLOCK_SLIDER";	

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPaneFactory#createDetailPane(java.lang.String)
	 */
	public IDetailPane createDetailPane(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return new TempoSliderDetailPane();
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return new ClockSliderDetailPane();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPaneFactory#getDefaultDetailPane(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public String getDefaultDetailPane(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof TempoControl) {
				return ID_TEMPO_SLIDER;
			}
			if (element instanceof ClockControl) {
				return ID_CLOCK_SLIDER;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPaneFactory#getDetailPaneDescription(java.lang.String)
	 */
	public String getDetailPaneDescription(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return "Tempo Slider";
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return "Clock Slider";
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPaneFactory#getDetailPaneName(java.lang.String)
	 */
	public String getDetailPaneName(String paneID) {
		if (ID_TEMPO_SLIDER.equals(paneID)) {
			return "Tempo Slider";
		}
		if (ID_CLOCK_SLIDER.equals(paneID)) {
			return "Clock Slider";
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPaneFactory#getDetailPaneTypes(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public Set getDetailPaneTypes(IStructuredSelection selection) {
		Set set = new HashSet();
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof TempoControl) {
				set.add(ID_TEMPO_SLIDER);
			}
			if (element instanceof ClockControl) {
				set.add(ID_CLOCK_SLIDER);
			}
		}
		return set;
	}

}
