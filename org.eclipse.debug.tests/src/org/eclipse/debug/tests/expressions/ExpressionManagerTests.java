/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.expressions;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionListener;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.core.ExpressionManager;
import org.eclipse.debug.internal.core.IExpressionsListener2;

/**
 * Tests expression manager and listener call backs
 */
public class ExpressionManagerTests extends TestCase {
	
	class SinlgeListener implements IExpressionListener {
		
		List added = new ArrayList();
		List removed = new ArrayList();
		List changed = new ArrayList();
		int addedCallbacks = 0;
		int removedCallbacks = 0;
		int changedCallbacks = 0;

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionListener#expressionAdded(org.eclipse.debug.core.model.IExpression)
		 */
		public void expressionAdded(IExpression expression) {
			added.add(expression);
			addedCallbacks++;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionListener#expressionRemoved(org.eclipse.debug.core.model.IExpression)
		 */
		public void expressionRemoved(IExpression expression) {
			removed.add(expression);
			removedCallbacks++;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionListener#expressionChanged(org.eclipse.debug.core.model.IExpression)
		 */
		public void expressionChanged(IExpression expression) {
			changed.add(expression);
			changedCallbacks++;
		}
		
	}
	
	class MultiListener implements IExpressionsListener {
		
		List added = new ArrayList();
		List removed = new ArrayList();
		List changed = new ArrayList();
		int addedCallbacks = 0;
		int removedCallbacks = 0;
		int changedCallbacks = 0;

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionsListener#expressionsAdded(org.eclipse.debug.core.model.IExpression[])
		 */
		public void expressionsAdded(IExpression[] expressions) {
			for (int i = 0; i < expressions.length; i++) {
				added.add(expressions[i]);
			}
			addedCallbacks++;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionsListener#expressionsRemoved(org.eclipse.debug.core.model.IExpression[])
		 */
		public void expressionsRemoved(IExpression[] expressions) {
			for (int i = 0; i < expressions.length; i++) {
				removed.add(expressions[i]);
			}
			removedCallbacks++;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IExpressionsListener#expressionsChanged(org.eclipse.debug.core.model.IExpression[])
		 */
		public void expressionsChanged(IExpression[] expressions) {
			for (int i = 0; i < expressions.length; i++) {
				changed.add(expressions[i]);
			}
			changedCallbacks++;
		}
		
	}
	
	class InsertMoveListener extends MultiListener implements IExpressionsListener2 {

		List moved = new ArrayList();
		List inserted = new ArrayList();
		int insertIndex = -1;
		int movedCallbacks = 0;
		int insertedCallbacks = 0;
		
		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.core.IExpressionsListener2#expressionsMoved(org.eclipse.debug.core.model.IExpression[], int)
		 */
		public void expressionsMoved(IExpression[] expressions, int index) {
			for (int i = 0; i < expressions.length; i++) {
				moved.add(expressions[i]);
			}
			movedCallbacks++;
			insertIndex = index;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.core.IExpressionsListener2#expressionsInserted(org.eclipse.debug.core.model.IExpression[], int)
		 */
		public void expressionsInserted(IExpression[] expressions, int index) {
			for (int i = 0; i < expressions.length; i++) {
				inserted.add(expressions[i]);
			}
			insertedCallbacks++;
			insertIndex = index;
		}
		
	}

	/**
	 * Returns the expression manager.
	 * 
	 * @return expression manager
	 */
	protected IExpressionManager getManager() {
		return DebugPlugin.getDefault().getExpressionManager();
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		// remove all expressions from the manager
		super.tearDown();
		getManager().removeExpressions(getManager().getExpressions());
	}
	
	/**
	 * Returns the index of the given expression in the given list or -1 if not present.
	 * 
	 * @param expression candidate
	 * @param list list to search
	 * @return index or -1
	 */
	private int indexOf(IExpression expression, IExpression[] list) {
		for (int i = 0; i < list.length; i++) {
			if (expression.equals(list[i])) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Add expressions and ensure proper call backs are received.
	 */
	public void testAddExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length);
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}
	
	/**
	 * Remove expressions and ensure proper call backs are received.
	 */
	public void testRemoveExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			manager.removeExpressions(new IExpression[]{exp1, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 1, expressions.length);
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(2, single.removedCallbacks);
			assertEquals(0, single.removed.indexOf(exp1));
			assertEquals(1, single.removed.indexOf(exp3));
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(1, multi.removedCallbacks);
			assertEquals(0, multi.removed.indexOf(exp1));
			assertEquals(1, multi.removed.indexOf(exp3));
			assertEquals(0, multi.changedCallbacks);
			assertEquals(-1, indexOf(exp1, expressions));
			assertEquals(0, indexOf(exp2, expressions));
			assertEquals(-1, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}
	
	/**
	 * Change expressions and ensure proper call backs are received.
	 */
	public void testChangeExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			exp1.setEnabled(false);
			exp2.setExpressionText("exp2changed");
			assertEquals("Wrong number of expressions", 3, expressions.length);
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(0, single.removedCallbacks);
			assertEquals(2, single.changedCallbacks);
			assertEquals(0, single.changed.indexOf(exp1));
			assertEquals(1, single.changed.indexOf(exp2));
			assertEquals(1, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(2, multi.changedCallbacks);
			assertEquals(0, multi.changed.indexOf(exp1));
			assertEquals(1, multi.changed.indexOf(exp2));
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}	
	
	/**
	 * Insert expressions and ensure proper call backs are received.
	 */
	public void testInsertBeforeExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			IWatchExpression exp4 = manager.newWatchExpression("exp4");
			IWatchExpression exp5 = manager.newWatchExpression("exp5");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);
			
			manager.insertExpressions(new IExpression[] {exp4, exp5}, exp2, true);
			
			assertEquals(2, single.addedCallbacks);
			assertEquals(2, single.added.size());
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(2, multi.added.size());
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(1, insert.insertedCallbacks);
			assertEquals(1, insert.insertIndex);
			assertEquals(0, insert.movedCallbacks);
			assertEquals(2, insert.inserted.size());
			assertEquals(0, insert.inserted.indexOf(exp4));
			assertEquals(1, insert.inserted.indexOf(exp5));
			
			expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp4, expressions));
			assertEquals(2, indexOf(exp5, expressions));
			assertEquals(3, indexOf(exp2, expressions));
			assertEquals(4, indexOf(exp3, expressions));
			
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}
	
	/**
	 * Insert expressions and ensure proper call backs are received.
	 */
	public void testInsertAfterExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			IWatchExpression exp4 = manager.newWatchExpression("exp4");
			IWatchExpression exp5 = manager.newWatchExpression("exp5");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);
			
			manager.insertExpressions(new IExpression[] {exp4, exp5}, exp2, false);
			
			assertEquals(2, single.addedCallbacks);
			assertEquals(2, single.added.size());
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(2, multi.added.size());
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(1, insert.insertedCallbacks);
			assertEquals(2, insert.insertIndex);
			assertEquals(0, insert.movedCallbacks);
			assertEquals(2, insert.inserted.size());
			assertEquals(0, insert.inserted.indexOf(exp4));
			assertEquals(1, insert.inserted.indexOf(exp5));
			
			expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp4, expressions));
			assertEquals(3, indexOf(exp5, expressions));
			assertEquals(4, indexOf(exp3, expressions));
			
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}		
	
	/**
	 * Move expressions and ensure proper call backs are received.
	 */
	public void testMoveBeforeExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			IWatchExpression exp4 = manager.newWatchExpression("exp4");
			IWatchExpression exp5 = manager.newWatchExpression("exp5");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);
			
			manager.moveExpressions(new IExpression[]{exp1,exp2}, exp5, true);
			
			assertEquals(0, single.addedCallbacks);
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(0, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, insert.insertedCallbacks);
			assertEquals(1, insert.movedCallbacks);
			assertEquals(2, insert.moved.size());
			assertEquals(0, insert.moved.indexOf(exp1));
			assertEquals(1, insert.moved.indexOf(exp2));
			assertEquals(2, insert.insertIndex);
			
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length);
			assertEquals(0, indexOf(exp3, expressions));
			assertEquals(1, indexOf(exp4, expressions));
			assertEquals(2, indexOf(exp1, expressions));
			assertEquals(3, indexOf(exp2, expressions));
			assertEquals(4, indexOf(exp5, expressions));
			
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}
	
	/**
	 * Move expressions and ensure proper call backs are received.
	 */
	public void testMoveAfterExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1");
			IWatchExpression exp2 = manager.newWatchExpression("exp2");
			IWatchExpression exp3 = manager.newWatchExpression("exp3");
			IWatchExpression exp4 = manager.newWatchExpression("exp4");
			IWatchExpression exp5 = manager.newWatchExpression("exp5");
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);
			
			manager.moveExpressions(new IExpression[]{exp1,exp2}, exp3, false);
			
			assertEquals(0, single.addedCallbacks);
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(0, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, insert.insertedCallbacks);
			assertEquals(1, insert.movedCallbacks);
			assertEquals(2, insert.moved.size());
			assertEquals(0, insert.moved.indexOf(exp1));
			assertEquals(1, insert.moved.indexOf(exp2));
			assertEquals(1, insert.insertIndex);
			
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length);
			assertEquals(0, indexOf(exp3, expressions));
			assertEquals(1, indexOf(exp1, expressions));
			assertEquals(2, indexOf(exp2, expressions));
			assertEquals(3, indexOf(exp4, expressions));
			assertEquals(4, indexOf(exp5, expressions));
			
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}	
	
	/**
	 * Test persist and restore of expressions
	 */
	public void testPersistExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		IWatchExpression exp1 = manager.newWatchExpression("exp1");
		IWatchExpression exp2 = manager.newWatchExpression("exp2");
		IWatchExpression exp3 = manager.newWatchExpression("exp3");
		IWatchExpression exp4 = manager.newWatchExpression("exp4");
		IWatchExpression exp5 = manager.newWatchExpression("exp5");
		manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
		manager.storeWatchExpressions();
		
		// create a new manager that will restore the expressions
		ExpressionManager manager2 = new ExpressionManager();
		IExpression[] expressions = manager2.getExpressions();
		assertEquals("Wrong number of expressions", 5, expressions.length);
		assertEquals("exp1", expressions[0].getExpressionText());
		assertEquals("exp2", expressions[1].getExpressionText());
		assertEquals("exp3", expressions[2].getExpressionText());
		assertEquals("exp4", expressions[3].getExpressionText());
		assertEquals("exp5", expressions[4].getExpressionText());
	}
	
	/**
	 * Tests concurrent access to expressions.
	 * 
	 * @throws InterruptedException
	 */
	public void testConcurrentAccess() throws InterruptedException {
		final boolean[] done = new boolean[]{false};
		final Exception[] ex = new Exception[]{null};
		Runnable add = new Runnable() {
			public void run() {
				try {
					for (int i = 0; i < 1000; i++) {
						getManager().addExpression(getManager().newWatchExpression(Integer.toHexString(i)));
					}
					done[0] = true;
				} catch (Exception e) {
					ex[0] = e;
				}
			}
		};
		Runnable remove = new Runnable() {
			public void run() {
				try {
					do {
						getManager().removeExpressions(getManager().getExpressions());
					} while (!done[0] || getManager().getExpressions().length > 0);
				} catch (Exception e) {
					ex[0] = e;
				}
			}
		};
		Thread t1 = new Thread(add);
		Thread t2 = new Thread(remove);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		assertEquals(0, getManager().getExpressions().length);
		assertNull(ex[0]);
	}
		
}
