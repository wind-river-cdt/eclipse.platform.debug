/*******************************************************************************
 * Copyright (c) 2009, 2010 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.viewer.model;

import java.util.LinkedList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * 
 */
public class TreeModelViewerAutopopulateAgent implements IViewerUpdateListener {

    private TreeModelViewer fViewer;
    
    
    public TreeModelViewerAutopopulateAgent(TreeModelViewer viewer) {
        fViewer = viewer;
        fViewer.addViewerUpdateListener(this);
    }
    
    public void dispose() {
        fViewer.removeViewerUpdateListener(this);
        fViewer = null;
    }
    
    public void updateComplete(IViewerUpdate update) {
        if (update instanceof IChildrenCountUpdate) {
            TreePath path = update.getElementPath();
            ILazyTreePathContentProvider contentProvider = (ILazyTreePathContentProvider) fViewer.getContentProvider(); 
                
            Widget[] items = fViewer.testFindItems(update.getElement());
            for (int i = 0; i < items.length; i++) {
                if ( path.equals(getTreePath(items[i])) ) {
                    int itemCount = getItemChildCount(items[i]);
                    for (int j = 0; j < itemCount; j++) {
                        contentProvider.updateElement(path, j);
                    }
                }
            }
        }
    }

    public void updateStarted(IViewerUpdate update) {
        // TODO Auto-generated method stub

    }

    public void viewerUpdatesBegin() {
        // TODO Auto-generated method stub

    }

    public void viewerUpdatesComplete() {
        // TODO Auto-generated method stub

    }
    
    private TreePath getTreePath(Widget w) {
        if (w instanceof TreeItem) {
            TreeItem item = (TreeItem)w;
            LinkedList segments = new LinkedList();
            while (item != null) {
                Object segment = item.getData();
                Assert.isNotNull(segment);
                segments.addFirst(segment);
                item = item.getParentItem();
            }
            return new TreePath(segments.toArray());
        }
        return TreePath.EMPTY;
    }

    private int getItemChildCount(Widget w) {
        if (w instanceof Tree) {
            return ((Tree)w).getItemCount();
        } else if (w instanceof TreeItem) {
            return ((TreeItem)w).getItemCount();
        }
        return 0;
    }        
}
