/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package org.eclipse.debug.internal.ui.viewers.update;

import java.util.Iterator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.memory.provisional.IMemoryViewPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.ModelDelta;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

public class MemoryRetrievalProxy extends AbstractModelProxy implements IMemoryBlockListener {
	private IMemoryBlockRetrieval fRetrieval;

	public MemoryRetrievalProxy(IMemoryBlockRetrieval retrieval)
	{
		fRetrieval = retrieval;
	
	}
	
	public void memoryBlocksAdded(IMemoryBlock[] memory) {
		
		ModelDelta delta = new ModelDelta(fRetrieval, IModelDelta.NO_CHANGE);
		
		for (int i=0; i<memory.length; i++){
			IMemoryBlockRetrieval retrieval = (IMemoryBlockRetrieval)memory[i].getAdapter(IMemoryBlockRetrieval.class);
			if (retrieval == null)
				retrieval = memory[i].getDebugTarget();
			
			if (retrieval != null)
			{
				if (retrieval == fRetrieval)
				{
					if (toSelect(memory[i]))
						delta.addNode(memory[i], IModelDelta.ADDED | IModelDelta.SELECT);
					else
						delta.addNode(memory[i], IModelDelta.ADDED);
				}
			}
		}
		
		fireModelChanged(delta);
	}

	public void memoryBlocksRemoved(IMemoryBlock[] memory) {
		ModelDelta delta = new ModelDelta(fRetrieval, IModelDelta.NO_CHANGE);
		
		// find a memory block to select
		
		for (int i=0; i<memory.length; i++){
			IMemoryBlockRetrieval retrieval = (IMemoryBlockRetrieval)memory[i].getAdapter(IMemoryBlockRetrieval.class);
			if (retrieval == null)
				retrieval = memory[i].getDebugTarget();
			
			if (retrieval != null)
			{
				if (retrieval == fRetrieval)
				{
					// do not change selection if the memory block removed is not 
					// currently selected
					if (isMemoryBlockSelected(getCurrentSelection(), memory[i]))
						addSelectDeltaNode(delta);
					delta.addNode(memory[i], IModelDelta.REMOVED);
				}
			}
		}
		
		fireModelChanged(delta);
		
	}

	public void init(IPresentationContext context) {
		super.init(context);
		DebugPlugin.getDefault().getMemoryBlockManager().addListener(this);
	}

	public synchronized void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getMemoryBlockManager().removeListener(this);
	}

	private void addSelectDeltaNode(ModelDelta delta)
	{
		IMemoryBlock[] memoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		if (memoryBlocks != null && memoryBlocks.length > 0)
		{
			delta.addNode(memoryBlocks[0], IModelDelta.SELECT);
		}
	}
	
	private IStructuredSelection getCurrentSelection()
	{
		if (getPresentationContext() == null)
		{
			return StructuredSelection.EMPTY;
		}
		
		ISelection selection = getPresentationContext().getPart().getSite().getSelectionProvider().getSelection();
		
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection)selection;
		
		return StructuredSelection.EMPTY;
	}
	
	private boolean isMemoryBlockSelected(IStructuredSelection selection, IMemoryBlock memoryBlock)
	{
		if (!selection.isEmpty())
		{
			Iterator iter = selection.iterator();
			while (iter.hasNext())
			{
				Object sel = iter.next();
				if (sel == memoryBlock)
					return true;
				
				if (sel instanceof IMemoryRendering)
				{
					if (((IMemoryRendering)sel).getMemoryBlock() == memoryBlock)
						return true;
				}
			}
		}
		return false;
	}

	public void installed() {
		IMemoryBlock[] memoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		if (memoryBlocks.length > 0)
		{
			ModelDelta delta = new ModelDelta(fRetrieval, IModelDelta.NO_CHANGE);
			
			// Select the first memory block if nothing is selected in the view
			// If something is selected, it means the view has restored selection and the
			// proxy should not interfere.
			IStructuredSelection selection = getCurrentSelection();
			if (selection.isEmpty())
			{
				addSelectDeltaNode(delta);
			}

			fireModelChanged(delta);
		}
	}
	
	private boolean toSelect(IMemoryBlock memoryBlock)
	{
		// if it's the first memory block, always select
		IMemoryBlock[] memoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		if (memoryBlocks.length == 1)
			return true;
		
		if (getPresentationContext() instanceof IMemoryViewPresentationContext)
		{
			// if registered, meaning the memory block is added from this view, select
			IMemoryViewPresentationContext context = (IMemoryViewPresentationContext)getPresentationContext();
			if (context.isMemoryBlockRegistered(memoryBlock))
				return true;
			// if display is not pinned, select
			else if (!context.isPinned())
				return true;
		}
		return false;
	}
	
}
