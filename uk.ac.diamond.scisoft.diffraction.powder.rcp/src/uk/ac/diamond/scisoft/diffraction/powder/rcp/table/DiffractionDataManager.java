/*
 * Copyright (c) 2012-2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.dawb.workbench.ui.Activator;
import org.dawnsci.plotting.tools.diffraction.DiffractionDefaultMetadata;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.dawnsci.plotting.tools.preference.diffraction.DiffractionRingsComposite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.dataset.SliceND;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.diffraction.IDetectorPropertyListener;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.metadata.IMetadata;
import org.eclipse.dawnsci.analysis.dataset.impl.AbstractDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;
import org.eclipse.dawnsci.analysis.dataset.slicer.SliceViewIterator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListDialog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.LocalServiceManager;

public class DiffractionDataManager {
	
	private List<DiffractionTableData> model;
	private DiffractionTableData currentData;
	private ILoaderService service;
	
	// Logger
	private final static Logger logger = LoggerFactory.getLogger(DiffractionDataManager.class);
	
	private HashSet<IDiffractionDataListener> listeners;
	
	public DiffractionDataManager() {
		this(new ArrayList<DiffractionTableData>(7));
	}
	public DiffractionDataManager(List<DiffractionTableData> model) {
		this.model = model;
		service    = Activator.getService(ILoaderService.class);
		listeners  = new HashSet<IDiffractionDataListener>();
		
		BundleContext ctx = FrameworkUtil.getBundle(DiffractionDataManager.class).getBundleContext();
		EventHandler fileLoadedHandler = new EventHandler() {
			
			@Override
			public void handleEvent(Event event) {
				Object property = event.getProperty("paths");
				if (property instanceof String[]){
					String[] names = (String[])property;
					for (String name : names) DiffractionDataManager.this.loadData(name, null);
				}
				
			}
		};
		
		Dictionary<String, String> props = new Hashtable<>();
		props.put(EventConstants.EVENT_TOPIC, "org/dawnsci/events/file/powder/OPEN");
		ctx.registerService(EventHandler.class, fileLoadedHandler, props);
	}
	
	public void setModel(List<DiffractionTableData> model) {
		this.model = model;
	}
	
	public boolean isEmpty() {
		return model.isEmpty();
	}
	
	public void setCurrentData(DiffractionTableData data) {
		this.currentData = data;
	}
	
	public DiffractionTableData getCurrentData() {
		return currentData;
	}
	
	public void setWavelength(double wavelength) {
		for (DiffractionTableData data : model) {
			data.getMetaData().getDiffractionCrystalEnvironment().setWavelength(wavelength);
		}
	}
	
	public DiffractionTableData[] toArray() {
		return model.toArray(new DiffractionTableData[model.size()]);
	}

	public Iterable<DiffractionTableData> iterable() {
		return model;
	}
	
	public int getSize() {
		return model.size();
	}
	
	public boolean remove(DiffractionTableData selectedData) {
		return model.remove(selectedData);
	}

	public boolean isValidModel() {
		return model!=null && getSize()>0;
	}
	
	public DiffractionTableData getLast() {
		return isValidModel() ? model.get(model.size()-1) : null;
	}
    
	/**
	 * Resets the meta data
	 */
	public void reset() {
		for (DiffractionTableData model : iterable()) {
			// Restore original metadata
			DetectorProperties originalProps = model.getMetaData().getOriginalDetector2DProperties();
			DiffractionCrystalEnvironment originalEnvironment =model.getMetaData().getOriginalDiffractionCrystalEnvironment();
			model.getMetaData().getDetector2DProperties().restore(originalProps);
			model.getMetaData().getDiffractionCrystalEnvironment().restore(originalEnvironment);
		}		
	}

	public void loadData(String filePath, String dataFullName) {
		if (filePath == null) return;

		for (DiffractionTableData d : model) {
			if (filePath.equals(d.getPath())) {
				return;
			}
		}
		
		DiffractionTableData data = new DiffractionTableData();
		data.setPath(filePath);
		int j = filePath.lastIndexOf(File.separator);
		String fileName = j > 0 ? filePath.substring(j + 1) : "file";
		data.setName(fileName);
		model.add(data);
		PowderFileLoaderJob job = new PowderFileLoaderJob(filePath, dataFullName, data);
		job.schedule();

	}
	
	public void addFileListener(IDiffractionDataListener listener) {
		listeners.add(listener);
	}
	
	public void removeFileListener(IDiffractionDataListener listener) {
		listeners.remove(listener);
	}
	
	private void fireDiffractionDataListeners(DiffractionDataChanged event) {
		for (IDiffractionDataListener listener : listeners) listener.dataChanged(event);
	}
	
	private class PowderFileLoaderJob extends Job {

		private final String path;
		private final String fullName;
		private final DiffractionTableData data;
		
		public PowderFileLoaderJob(String filePath, String dataFullName, DiffractionTableData data) {
			super("Load powder file");
			this.path = filePath;
			this.fullName = dataFullName;
			this.data = data;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			ILazyDataset image = null;
			IDataHolder dh = null;
			
			
			try {
				dh = LocalServiceManager.getLoaderService().getData(path, null);

			} catch (Exception e1) {
				model.remove(data);
				fireDiffractionDataListeners(null);
				return Status.CANCEL_STATUS;
			}
			
			if (dh.size() == 1) {
				ILazyDataset ld = dh.getLazyDataset(0);
				ld.squeezeEnds();
				if (ld.getRank() == 2) {
					image = ld;
				}
			}
			
			if (image == null && fullName != null) {
				ILazyDataset ld = dh.getLazyDataset(fullName);
				ld.squeezeEnds();
				if (ld.getRank() == 2) {
					image = ld;
				}
			}
			
			final String[] outName = new String[2];
			
			if (image == null &&  fullName == null) {
				try {
					IMetadata metaData = LocalServiceManager.getLoaderService().getMetadata(path, null);
					final Map<String, int[]> dataShapes = metaData.getDataShapes();
					final List<String> dataNames = new ArrayList<String>();
					for (String name : dataShapes.keySet()) {
						int[] shape = dataShapes.get(name);
						int[] ss = AbstractDataset.squeezeShape(shape, false);
						if (ss.length >= 2) {
							dataNames.add(name);
						}
					}
					
					Display.getDefault().syncExec(new Runnable() {
						
						@Override
						public void run() {
							ListDialog dia = new ListDialog(Display.getDefault().getActiveShell());
							dia.setTitle("Multiple dataset file!");
							dia.setMessage("Select dataset to calibrate:");
							dia.setContentProvider(new ArrayContentProvider());
							dia.setLabelProvider(new LabelProvider());
							dia.setInput(dataNames);
							if (dia.open() == ListDialog.OK) {
								outName[0] = dia.getResult()[0].toString();
							}
							
							int[] shape = dataShapes.get(outName[0]);
							
							int[] ss = AbstractDataset.squeezeShape(shape,false);
							
							if (ss.length != 2) {
								if (ss.length == 3) {
									int size = ss[0];
									final List<String> dataNames = new ArrayList<String>();
									for (String name : dataShapes.keySet()) {
										shape = dataShapes.get(name);
										ss = AbstractDataset.squeezeShape(shape, false);
										if (shape.length == 1 && shape[0] == size) {
											dataNames.add(name);
										}
									}
									
									dia = new ListDialog(Display.getDefault().getActiveShell());
									dia.setTitle("Multiple Frame Dataset!");
									dia.setMessage("Select Detector Distance Data:");
									dia.setContentProvider(new ArrayContentProvider());
									dia.setLabelProvider(new LabelProvider());
									dia.setInput(dataNames);
									if (dia.open() == ListDialog.OK) {
										outName[1] = dia.getResult()[0].toString();
									}
									
								} else {
									return;
								}
							}
							
							
						}
					});
					
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			
//			if (outName[0] != null) image = SelectionUtils.loadData(path, outName[0]);
			
			return setUpImage(path, outName, data) ? Status.OK_STATUS : Status.CANCEL_STATUS;
			
//			if (image == null){
//				model.remove(data);
//				fireDiffractionDataListeners(null);
//				return Status.CANCEL_STATUS;
//			}
//			
//			int j = path.lastIndexOf(File.separator);
//			String fileName = j > 0 ? path.substring(j + 1) : null;
//			image.setName(fileName + ":" + image.getName());
//			data.setImage(image);
//			String[] statusString = new String[1];
//			data.setMetaData(DiffractionUtils.getDiffractionMetadata(image, path, service, statusString));
//			data.getImage().setMetadata(data.getMetaData());
//			
//			fireDiffractionDataListeners(new DiffractionDataChanged(data));
//
//			return Status.OK_STATUS;
//		}

	}
	}
	
	private boolean setUpImage(String path, String[] datasetNames, DiffractionTableData data){
		if (datasetNames == null) return false;
		
		if (datasetNames[1] == null) {
			return setUpImage(path,datasetNames[0],data);
		}
		
		ILazyDataset ld = null;
		IDataset dist = null;
		try {
			ld = LocalServiceManager.getLoaderService().getData(path, null).getLazyDataset(datasetNames[0]);
			dist = LocalServiceManager.getLoaderService().getData(path, null).getLazyDataset(datasetNames[1]).getSlice();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (ld == null) return false;
		
		ld = ld.getSliceView();
		ld.squeezeEnds();
		
		SliceViewIterator it = new SliceViewIterator(ld, null, new int[]{1,2});
		
		int j = path.lastIndexOf(File.separator);
		String fileName = j > 0 ? path.substring(j + 1) : null;
		it.hasNext();
		IDataset next = it.next().getSlice().squeeze();
		
		int count = 0;
		
		next.setName(fileName + ":" + next.getName());
		data.setImage(next);
		data.setName(fileName + ": " + count);
		data.setMetaData(DiffractionDefaultMetadata.getDiffractionMetadata(next.getShape()));
		data.getImage().setMetadata(data.getMetaData());
		data.setDistance(dist.getDouble(count++));
		
		while (it.hasNext()) {
			ILazyDataset n = it.next().getSlice().squeeze();
			n.setName(fileName + ":" + n.getName() + count);
			DiffractionTableData d = new DiffractionTableData();
			d.setImage(n);
			d.setName(fileName + ": " + count);
			d.setMetaData(DiffractionDefaultMetadata.getDiffractionMetadata(n.getShape()));
			d.getImage().setMetadata(d.getMetaData());
			d.setDistance(dist.getDouble(count++));
			model.add(d);
		}
		
		fireDiffractionDataListeners(new DiffractionDataChanged(data));

		return true;
	}

	private boolean setUpImage(String path, String datasetName, DiffractionTableData data){
			
		ILazyDataset ld = null;
		try {
			if (datasetName != null) {
				ld = LocalServiceManager.getLoaderService().getData(path, null).getLazyDataset(datasetName);
			} else {
				ld = LocalServiceManager.getLoaderService().getData(path, null).getLazyDataset(0);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (ld == null) return false;
		
		ld = ld.getSliceView();
		ld.squeezeEnds();
		
		
//		if (image == null){
//			model.remove(data);
//			fireDiffractionDataListeners(null);
//			return Status.CANCEL_STATUS;
//		}
		
		int j = path.lastIndexOf(File.separator);
		String fileName = j > 0 ? path.substring(j + 1) : null;
		ld.setName(fileName + ":" + ld.getName());
		data.setImage(ld.getSlice());
		String[] statusString = new String[1];
		data.setMetaData(DiffractionUtils.getDiffractionMetadata(ld, path, service, statusString));
		data.getImage().setMetadata(data.getMetaData());
		
		fireDiffractionDataListeners(new DiffractionDataChanged(data));

		return true;
	}

	public void dispose() {
		if (model!=null) model.clear(); // Helps garbage collector.
	}

	public Dataset getDistances() {
		
		if (!isValidModel()) return null; // Or raise exception?
		
		double[] deltaDistance = new double[getSize()];
		
		for (int i = 0; i < model.size(); i++) deltaDistance[i] = model.get(i).getDistance();
		
		return new DoubleDataset(deltaDistance, new int[]{deltaDistance.length});
	}
	
	public void clear(IDetectorPropertyListener listener) {
		if (!isValidModel()) return;
		if (listener!=null) for (DiffractionTableData d : iterable()) {
			d.getMetaData().getDetector2DProperties().removeDetectorPropertyListener(listener);
		}
		model.clear();
	}


}
