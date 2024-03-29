/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.viewer.model;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipe.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;

/**
 * Tests to verify that the viewer properly handles initial columns width.
 */
public class ColumnPresentationTests extends TestCase implements ITestModelUpdatesListenerConstants {
	private Display fDisplay;
	private Shell fShell;
	private TreeModelViewer fViewer;
	private TestModelUpdatesListener fListener;
	private boolean fResized = false; 

	public ColumnPresentationTests(String name) {
		super(name);
	}

	/**
	 * @throws java.lang.Exception
	 */
	protected void setUp() throws Exception {
		createViewer();
	}

	/**
	 * @throws java.lang.Exception
	 */
	protected void tearDown() throws Exception {
		destroyViewer();
	}

	void createViewer() {
		fDisplay = PlatformUI.getWorkbench().getDisplay();
        fShell = new Shell(fDisplay);
        fShell.setSize(800, 600);
		fShell.setLayout(new FillLayout());
		fViewer = new TreeModelViewer(fShell, SWT.VIRTUAL, new PresentationContext("TestViewer"));
        fViewer.getTree().addControlListener(new ControlListener() {
            public void controlResized(ControlEvent e) {
                fResized = true;
            }
            
            public void controlMoved(ControlEvent e) {
            }
        });
		fListener = new TestModelUpdatesListener(fViewer, false, false);
		fShell.open();
	}

	void destroyViewer() throws InterruptedException {
		fListener.dispose();
		fViewer.getPresentationContext().dispose();
		// Close the shell.
		fShell.close();
		while (!fShell.isDisposed()) {
			if (!fDisplay.readAndDispatch()) {
				Thread.sleep(0);
			}
		}
	}

	void recreateViewer() throws InterruptedException {
		destroyViewer();
		createViewer();
	}

	static class MyColumnPresentation implements IColumnPresentation {
		private IPresentationContext fContext;
		protected String[] columnIds;

		MyColumnPresentation() {
		    this (makeDefaultColumnIds());
        }

		static String[] makeDefaultColumnIds() {
            String[] columnIds = new String[5];
            for (int i = 0; i < columnIds.length; i++) {
                columnIds[i] = "ColumnId_" + i;
            }
            return columnIds;
		}
		
		MyColumnPresentation(String[] columnIds) {
			this.columnIds = columnIds;
		}

		public void init(IPresentationContext context) {
			fContext = context;
		}

		public void dispose() {
			fContext = null;
		}

		public ImageDescriptor getImageDescriptor(String id) {
			return null;
		}

		protected IPresentationContext getPresentationContext() {
			return fContext;
		}

		public String[] getAvailableColumns() {
			return columnIds;
		}

		public String[] getInitialColumns() {
			return columnIds;
		}

		public String getHeader(String id) {
		    if (Arrays.asList(columnIds).indexOf(id) != -1) {
		        return id;
		    }
		    return null;
		}

		public String getId() {
			return "MyColumnPresentationID";
		}

		public boolean isOptional() {
			return true;
		}

	}

	static class MyColumnPresentation2 extends MyColumnPresentation implements IColumnPresentation2 {
		int[] repliedWidths;

		MyColumnPresentation2() {
			super();
			for (int i = 0; i < columnIds.length; i++) {
				columnIds[i] += "_2";
			}
			repliedWidths = new int[columnIds.length];
			Arrays.fill(repliedWidths, -2);
		}

		public int getInitialColumnWidth(String id, int treeWidgetWidth,
				String[] visibleColumnIds) {
			for (int i = 0; i < columnIds.length; i++) {
				if (id.equals(columnIds[i]) && i == 0) {
					repliedWidths[i] = 2 * (treeWidgetWidth / visibleColumnIds.length);
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && (i == 1 || i == 2)) {
					repliedWidths[i] = (treeWidgetWidth / visibleColumnIds.length) / 2;
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && i == 3) {
					repliedWidths[i] = (treeWidgetWidth / visibleColumnIds.length);
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && i == 4) {
					repliedWidths[i] = -1;
					return repliedWidths[i];
				}
			}
			return -1;
		}

		public String getId() {
			return "MyColumnPresentation2ID";
		}
	}

	class MyModel extends TestModel implements IColumnPresentationFactory {
		MyColumnPresentation colPresenation;

		MyModel(MyColumnPresentation cp1) {
			colPresenation = cp1;
		}

		public IColumnPresentation createColumnPresentation(
				IPresentationContext context, Object element) {
			if (colPresenation != null) {
				return colPresenation;
			}
			return null;
		}

		public String getColumnPresentationId(IPresentationContext context,
				Object element) {
			if (colPresenation != null) {
				return colPresenation.getId();
			}
			return null;
		}

	}

	private TestModel makeModel(MyColumnPresentation cp, String rootSufffix) throws InterruptedException {
		MyModel model = new MyModel(cp);
		model.setRoot(new TestElement(model, "root" + rootSufffix, new TestElement[] {
				new TestElement(model, "1", true, true, new TestElement[0]),
				new TestElement(model, "2", true, false, new TestElement[0]),
				new TestElement(model, "3", false, true, new TestElement[0]),
				new TestElement(model, "4", false, false, new TestElement[0]),
				new TestElement(model, "5", new TestElement[0]),
				new TestElement(model, "6", new TestElement[0]) }));
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);
		fViewer.setInput(model.getRootElement());
		while (!fListener.isFinished()) {
			if (!fDisplay.readAndDispatch()) {
				Thread.sleep(0);
			}
		}
		model.validateData(fViewer, TreePath.EMPTY);
		return model;
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation
	 * implementation without requiring the presentation being
	 * IColumnPresentation2 (i.e. ensure we do not break backward compatibility
	 * of interface). Also, we verify that the initial columns width is the
	 * average of tree width / number of visible columns, which is the logic
	 * in InternalTreeModelViewer.
	 */
	public void testInitialColumnAverageWidth() throws InterruptedException {
        // Try to wait for the shell painting to settle
        if (!fDisplay.readAndDispatch ()) Thread.sleep(0);
        fResized = false;
	    
		MyColumnPresentation colPre = new MyColumnPresentation();
		makeModel(colPre, "m1");
		Tree tree = fViewer.getTree();
		TreeColumn[] columns = tree.getColumns();
		int treeWidth = tree.getSize().x;
		int avgWidth = treeWidth / columns.length;

        // Resizing the tree invalidates the test.
		if (fResized) {
		    return;
		}

		for (int i = 0; i < columns.length - 1; i++) {
			assertEquals(avgWidth, columns[i].getWidth());
		}
		// Checking of the width of the last column is not reliable.  
		// I.e. it's handled differenty on different platforms.
        //int remainder = treeWidth % columns.length;
        //assertEquals(avgWidth + remainder, columns[columns.length - 1].getWidth());
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width is the width computed by
	 * the IColumnPresentation2 implementation.
	 */
	public void testInitialColumnWidth() throws InterruptedException {
        // Try to wait for the shell painting to settle
        if (!fDisplay.readAndDispatch ()) Thread.sleep(0);
        fResized = false;

        MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2");
		Tree tree = fViewer.getTree();
		TreeColumn[] columns = tree.getColumns();
		
        // Resizing the tree invalidates the test.
		if (fResized) {
		    return;
		}
		
		for (int i = 0; i < columns.length; i++) {
			int width = colPre.repliedWidths[i];
			if (width != -1) {
	            assertEquals(width, columns[i].getWidth());
			}
		}
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width from IColumnPresentation2
	 * is not used when there are user settings inside the viewer which are
	 * created from user resizing columns.
	 */
	public void testRespectUserSettings() throws InterruptedException {
		MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2");
		TreeColumn[] columns = fViewer.getTree().getColumns();
		// simulate user resizing each column width
		int[] newWidths = new int[columns.length];
		for (int i = 0; i < columns.length; i++) {
			newWidths[i] = columns[i].getWidth() + 10;
			columns[i].setWidth(newWidths[i]);
		}
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
		// get InternalTreeModelViewer to rebuild columns due to change of
		// model and presentation - first set to another model and column
		// presentation, then switch to a model with original presentation.
		makeModel(new MyColumnPresentation(), "m1");
		makeModel(colPre, "m3");
		// verify user resized widths are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}

		// get InternalTreeModelViewer to rebuild columns due to hide and show columns
		fViewer.setShowColumns(false);
		do {
			if (!fDisplay.readAndDispatch()) {
				Thread.sleep(0);
			}
		} while (fViewer.getTree().getColumns().length > 0);
		fViewer.setShowColumns(true);
		do {
			if (!fDisplay.readAndDispatch()) {
				Thread.sleep(0);
			}
		} while (fViewer.getTree().getColumns().length != newWidths.length);
		// verify user resized widths are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width from IColumnPresentation2
	 * is not used when there are user settings inside the viewer which are
	 * restored from memento, e.g., restoring workspace, etc.
	 */
	public void testRespectMemento() throws InterruptedException {
		MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2");
		TreeColumn[] columns = fViewer.getTree().getColumns();
		// simulate user resizing each column width
		int[] newWidths = new int[columns.length];
		for (int i = 0; i < columns.length; i++) {
			newWidths[i] = columns[i].getWidth() + 10;
			columns[i].setWidth(newWidths[i]);
		}
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
		// save memento
		final XMLMemento memento = XMLMemento.createWriteRoot("TEST"); 
		fViewer.saveState(memento);
		// throw away any settings inside the viewer and create a new viewer
		// with memento settings, this is the same effect resulted from closing
		// and opening workspace again.
		recreateViewer();
		fViewer.initState(memento);
		// get InternalTreeModelViewer to rebuild columns
		makeModel(colPre, "m2");
		// verify widths from memento are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
	}
	
	/**
     * In this test: verify that tree viewer can handle the column presentation changing 
     * its available column IDs between runs (bug 360015).
     */
    public void testChangedColumnIds() throws InterruptedException {
        MyColumnPresentation colPre = new MyColumnPresentation();
        
        makeModel(colPre, "m1");
        TreeColumn[] columns = fViewer.getTree().getColumns();
        // Select visible columns
        fViewer.setVisibleColumns(new String[] { colPre.columnIds[0] });
        do {
            if (!fDisplay.readAndDispatch()) {
                Thread.sleep(0);
            }
        } while (fViewer.getTree().getColumns().length != 1);
        
        // get InternalTreeModelViewer to rebuild columns due to change of
        // model and presentation - first set to another model and column
        // presentation, then switch to a model with original presentation.
        makeModel(new MyColumnPresentation2(), "m2");
        
        String[] newColumnIds = MyColumnPresentation.makeDefaultColumnIds();
        newColumnIds[0] = "new_column_id";
        colPre = new MyColumnPresentation(newColumnIds);
        
        makeModel(colPre, "m3");
        
        // verify user resized widths are used instead of the initial widths from IColumnPresentation2
        columns = fViewer.getTree().getColumns();
        for (int i = 0; i < columns.length; i++) {
            assertEquals(newColumnIds[i], columns[i].getText());
        }
    }

}
