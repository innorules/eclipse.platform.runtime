/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.search;

import java.text.Collator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInTargetList;

/**
 *
 */
public class FindUnusedSearchResultPage extends AbstractTextSearchViewPage implements ISearchResultPage, IAdaptable {

	public class DecoratorIgnoringViewerSorter extends ViewerSorter {

		private Collator collator;

		public DecoratorIgnoringViewerSorter() {
			super(null); // lazy initialization
			collator = null;
		}

		@Override
		public int compare(Viewer aViewer, Object e1, Object e2) {
			String name1 = labelProvider.getText(e1);
			String name2 = labelProvider.getText(e2);
			if (name1 == null)
				name1 = "";//$NON-NLS-1$
			if (name2 == null)
				name2 = "";//$NON-NLS-1$
			return getCollator().compare(name1, name2);
		}

		@Override
		public final Collator getCollator() {
			if (collator == null) {
				collator = Collator.getInstance();
			}
			return collator;
		}
	}

	class SortAction extends Action {
		int order;
		SortAction(String label, int order) {
			super(label);
			this.order = order;
		}

		@Override
		public void run() {
			setSortOrder(order);
		}
	}

	public static class TableContentProvider implements IStructuredContentProvider {
		private AbstractTextSearchResult fSearchResult;
		private TableViewer fTableViewer;

		public void clear() {
			fTableViewer.refresh();
		}

		@Override
		public void dispose() {
			//nothing to dispose
		}

		public void elementsChanged(Object[] updatedElements) {
			for (int i = 0; i < updatedElements.length; i++) {
				if (fSearchResult.getMatchCount(updatedElements[i]) > 0) {
					if (fTableViewer.testFindItem(updatedElements[i]) != null)
						fTableViewer.refresh(updatedElements[i]);
					else
						fTableViewer.add(updatedElements[i]);
				} else {
					fTableViewer.remove(updatedElements[i]);
				}
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (fSearchResult != null)
				return fSearchResult.getElements();
			return new Object[0];
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fTableViewer = (TableViewer) viewer;
			fSearchResult = (AbstractTextSearchResult) newInput;
		}
	}

	public static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
		@Override
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};

	static final String[] SHOW_IN_TARGETS = new String[] {JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV};

	private static final int SORT_BY_NAME = 0;
	private static final int SORT_BY_PATH = 1;

	TableContentProvider contentProvider;
	JavaElementLabelProvider labelProvider;
	Action sortByName = new SortAction("Name", SORT_BY_NAME);
	Action sortByPath = new SortAction("Path", SORT_BY_PATH);
	TableViewer viewer;

	private int currentSortOrder = -1;

	public FindUnusedSearchResultPage() {
		super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	@Override
	protected void clear() {
		if (contentProvider != null)
			contentProvider.clear();
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	@Override
	protected void configureTableViewer(TableViewer aViewer) {
		this.viewer = aViewer;
		contentProvider = new TableContentProvider();
		aViewer.setContentProvider(contentProvider);
		setSortOrder(SORT_BY_PATH);
		aViewer.setSorter(new DecoratorIgnoringViewerSorter());
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	@Override
	protected void configureTreeViewer(TreeViewer aViewer) {
		throw new IllegalStateException("Doesn't support tree mode.");
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	@Override
	protected void elementsChanged(Object[] objects) {
		if (contentProvider != null)
			contentProvider.elementsChanged(objects);
	}

	@Override
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		MenuManager sortMenu = new MenuManager("Sort By");
		sortMenu.add(sortByName);
		sortMenu.add(sortByPath);
		sortByName.setChecked(currentSortOrder == SORT_BY_NAME);
		sortByPath.setChecked(currentSortOrder == SORT_BY_PATH);
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (IShowInTargetList.class.equals(adapter)) {
			return SHOW_IN_TARGET_LIST;
		}
		return null;
	}

	void setSortOrder(int order) {
		if (currentSortOrder == order)
			return;
		currentSortOrder = order;
		int flags;
		if (order == SORT_BY_NAME) {
			flags = JavaElementLabelProvider.SHOW_POST_QUALIFIED;
		} else {
			flags = JavaElementLabelProvider.SHOW_QUALIFIED;
		}
		flags |= JavaElementLabelProvider.SHOW_PARAMETERS;
		labelProvider = new JavaElementLabelProvider(flags);
		viewer.setLabelProvider(labelProvider);
	}

	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		try {
			Object element = match.getElement();
			if (element instanceof IJavaElement) {
				JavaUI.openInEditor((IJavaElement) element);
			}
		} catch (JavaModelException e1) {
			throw new PartInitException(e1.getStatus());
		}
	}

}
