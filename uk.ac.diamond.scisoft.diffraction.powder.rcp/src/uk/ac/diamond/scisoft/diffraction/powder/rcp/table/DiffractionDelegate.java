/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import org.dawb.common.ui.selection.SelectedTreeItemInfo;
import org.dawb.common.ui.selection.SelectionUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorPropertyEvent;
import org.eclipse.dawnsci.analysis.api.diffraction.IDetectorPropertyListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ResourceTransfer;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;

/**
 * 
 * @author wqk87977
 *
 */
public class DiffractionDelegate {

	private TableViewer viewer;
	
	private DiffractionDataManager manager;
	private Composite parent;
	private IDetectorPropertyListener detectorPropertyListener;
	private Table table;
	private int tabIndex = 0;

	/**
	 * 
	 * @param parent
	 *           composite
	 * @param pathsList
	 *           list of all paths images to be displayed in the table viewer
	 * @param service
	 *           service loader, can be null
	 */
	public DiffractionDelegate(Composite parent, DiffractionDataManager manager) {
		
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER );
		this.parent = parent;
		this.table = viewer.getTable();
		this.manager = manager;
		
		initialize();
		createColumns(viewer);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setToolTipText("How to use Diffraction Calibration View:\n" +
				"1. Drag/drop or double click your calibration file to add it.\n"+
				"2. Choose the calibrant\n" +
				"3. Select the rings to use.\n" +
				"4. Run the calibration.");
		
		viewer.setContentProvider(new DiffCalContentProvider());
		viewer.setLabelProvider(new DiffCalLabelProvider());
		viewer.setInput(manager);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		
		final Action deleteAction = getDeleteAction();
		final Action clearAction = getClearAllAction();
		
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				Object[] selected = selection.toArray();
				if (selected.length > 0) {
					if (selected.length == 1) {
						deleteAction.setText("Delete "
								+ ((DiffractionImageData) selection
										.getFirstElement()).getName());
						mgr.add(deleteAction);
					} else {
						deleteAction.setText("Delete " + selected.length
								+ " images selected");
						mgr.add(deleteAction);
					}
				}
				mgr.add(clearAction);
			}
		});
		viewer.getControl().setMenu(mgr.createContextMenu(viewer.getControl()));
		// add drop support
		DropTarget dt = new DropTarget(viewer.getControl(), DND.DROP_MOVE
				| DND.DROP_DEFAULT | DND.DROP_COPY);
		dt.setTransfer(new Transfer[] { TextTransfer.getInstance(),
				FileTransfer.getInstance(), ResourceTransfer.getInstance(),
				LocalSelectionTransfer.getTransfer() });
		dt.addDropListener(getDropListener());

//		Label infoEditableLabel = new Label(parent, SWT.NONE);
//		infoEditableLabel.setText("* Click to change value");
	}

	private DropTargetAdapter getDropListener() {
		return new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				Object dropData = event.data;
				if (dropData instanceof IResource[]) {
					IResource[] res = (IResource[]) dropData;
					for (int i = 0; i < res.length; i++) {
						manager.loadData(res[i].getRawLocation().toOSString(), null);
					}
				} else if (dropData instanceof ITreeSelection) {
					SelectedTreeItemInfo[] results = SelectionUtils.parseAsTreeSelection((ITreeSelection) dropData);
					for (SelectedTreeItemInfo i : results) {
						manager.loadData(i.getFile(), i.getNode());
					}
				} else if (dropData instanceof String[]) {
					String[] selectedData = (String[]) dropData;
					for (int i = 0; i < selectedData.length; i++) {
						manager.loadData(selectedData[i], null);
					}
				}
			}
		};
	}
	
	private Action getDeleteAction() {
		return new Action("Remove item", Activator.getImageDescriptor("icons/delete_obj.png")) {
			@Override
			public void run() {
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				Object[] selected = selection.toArray();
				for (int i = 0; i < selected.length; i++) {
					DiffractionImageData selectedData = (DiffractionImageData) selected[i];
					if (manager.getSize() > 0) {
						if (manager.remove(selectedData)) {
							if (selectedData.getMetaData() != null)
								selectedData.getMetaData().getDetector2DProperties().removeDetectorPropertyListener(detectorPropertyListener);
						}
					}
				}
				if (manager.getSize() > 0) {
					viewer.setSelection(new StructuredSelection((DiffractionImageData) viewer.getElementAt(0)));
				} else {
					viewer.setSelection(new StructuredSelection());
				}
				updateTableColumnsAndLayout(tabIndex);
			}
		};
	}
	
	private Action getClearAllAction() {
		return new Action("Clear All", Activator.getImageDescriptor("icons/delete_obj.png")) {
			@Override
			public void run() {
				manager.clear();
				updateTableColumnsAndLayout(tabIndex);
			}
		};
	}
	
	private void initialize(){
		detectorPropertyListener = new IDetectorPropertyListener() {
			@Override
			public void detectorPropertiesChanged(DetectorPropertyEvent evt) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.refresh();
					}
				});
			}
		};
	}

	/**
	 * Add DetectorPropertyListener for the given DiffractionTableData
	 * @param data
	 */
	public void addDetectorPropertyListener(DiffractionImageData data) {
		data.getMetaData().getDetector2DProperties().addDetectorPropertyListener(detectorPropertyListener);
	}


	private void createColumns(TableViewer tv) {
		TableViewerColumn tvc = new TableViewerColumn(tv, SWT.NONE);
		TableColumn tc = tvc.getColumn();
		tc.setText("Image");
		tc.setWidth(200);
		tvc.setEditingSupport(new DiffCalEditingSupport(tv, 0));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("# of rings");
		tc.setWidth(0);
		tc.setMoveable(false);
		tvc.setEditingSupport(new DiffCalEditingSupport(tv, 1));

		tvc = new TableViewerColumn(tv, SWT.NONE);
		tc = tvc.getColumn();
		tc.setText("Distance");
		tc.setToolTipText("in mm");

		tc.setWidth(80);
		tc.setMoveable(true);

		
		tvc.setEditingSupport(new DiffCalEditingSupport(tv, 2));
	}

	/**
	 * Update the visibility of the Table columns and parent layout
	 */
	public void updateTableColumnsAndLayout(int tabIndex) {
		this.tabIndex  = tabIndex;
		TableColumn[] columns = table.getColumns();
		for (int i = 1; i < columns.length; i++) {
			if (tabIndex == 0) {	// auto mode
				int width = 0;
				// if more than one image and distance column index
				if (manager.getSize() > 1 && i == 2)
					width = 80;
				table.getColumns()[i].setWidth(width);
				table.getColumns()[i].setMoveable(width>0);
			} else if (tabIndex == 1) {	// manual mode
				int width = 80;
				// if less than 2 images and column is distance
				if (manager.getSize() <= 1 && i == 2)
					width = 0;
				table.getColumns()[i].setWidth(width);
				table.getColumns()[i].setMoveable(width>0);
			}
		}
		// update parent composite
		viewer.refresh();
		Rectangle r = parent.getClientArea();
		if (parent.getParent() instanceof ScrolledComposite) {
			ScrolledComposite scrollHolder = (ScrolledComposite)parent.getParent();
			scrollHolder.setMinSize(parent.computeSize(r.width, SWT.DEFAULT));
			scrollHolder.layout();
		}
	}

	public IDetectorPropertyListener getDetectorPropertyListener() {
		return detectorPropertyListener;
	}

	public void addSelectionChangedListener(ISelectionChangedListener selectionChangeListener) {
		viewer.addSelectionChangedListener(selectionChangeListener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener selectionChangeListener) {
		viewer.removeSelectionChangedListener(selectionChangeListener);
	}

	public void setLayoutData(GridData data) {
		viewer.getTable().setLayoutData(data);
	}

	public void refresh() {
		viewer.refresh();
	}

	public void setSelection(StructuredSelection structuredSelection) {
		viewer.setSelection(structuredSelection);
	}

	public void setSelection(ISelection structuredSelection, boolean reveal) {
		viewer.setSelection(structuredSelection, reveal);
	}

	public ISelection getSelection() {
		return viewer.getSelection();
	}

	public boolean isDisposed() {
		return viewer.getTable().isDisposed();
	}

	public void setFocus() {
		viewer.getTable().setFocus();
	}
	
}
