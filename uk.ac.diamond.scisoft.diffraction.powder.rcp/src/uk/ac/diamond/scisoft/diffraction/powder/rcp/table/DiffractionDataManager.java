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

import org.dawnsci.datavis.api.IRecentPlaces;
import org.dawnsci.plotting.tools.diffraction.DiffractionDefaultMetadata;
import org.dawnsci.plotting.tools.diffraction.DiffractionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.diffraction.IDetectorPropertyListener;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.dataset.slicer.SliceViewIterator;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.ShapeUtils;
import org.eclipse.january.metadata.IMetadata;
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

import uk.ac.diamond.osgi.services.ServiceProvider;
import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;

public class DiffractionDataManager {
	
	private List<DiffractionImageData> model;
	private DiffractionImageData currentData;
	private ILoaderService service;
	
	// Logger
	private final static Logger logger = LoggerFactory.getLogger(DiffractionDataManager.class);
	
	private HashSet<IDiffractionDataListener> listeners;
	
	public DiffractionDataManager() {
		this(new ArrayList<DiffractionImageData>(7));
	}
	public DiffractionDataManager(List<DiffractionImageData> model) {
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
	
	public void setModel(List<DiffractionImageData> model) {
		this.model = model;
	}
	
	public void clear(){
		model.clear();
	}
	
	public boolean isEmpty() {
		return model.isEmpty();
	}
	
	public void setCurrentData(DiffractionImageData data) {
		this.currentData = data;
	}
	
	public DiffractionImageData getCurrentData() {
		return currentData;
	}
	
	public void setWavelength(double wavelength) {
		for (DiffractionImageData data : model) {
			data.getMetaData().getDiffractionCrystalEnvironment().setWavelength(wavelength);
		}
	}
	
	public DiffractionImageData[] toArray() {
		return model.toArray(new DiffractionImageData[model.size()]);
	}

	public Iterable<DiffractionImageData> iterable() {
		return model;
	}
	
	public List<DiffractionImageData> getDataList(){
		return new ArrayList<>(model);
	}
	
	public int getSize() {
		return model.size();
	}
	
	public boolean remove(DiffractionImageData selectedData) {
		boolean remove = model.remove(selectedData); 
		
		if (remove) fireDiffractionDataListeners(null);
		
		return remove;
	}

	public boolean isValidModel() {
		return model!=null && getSize()>0;
	}
	
	public DiffractionImageData getLast() {
		return isValidModel() ? model.get(model.size()-1) : null;
	}
    
	/**
	 * Resets the meta data
	 */
	public void reset() {
		for (DiffractionImageData model : iterable()) {
			// Restore original metadata
			DetectorProperties originalProps = model.getMetaData().getOriginalDetector2DProperties();
			DiffractionCrystalEnvironment originalEnvironment =model.getMetaData().getOriginalDiffractionCrystalEnvironment();
			model.getMetaData().getDetector2DProperties().restore(originalProps);
			model.getMetaData().getDiffractionCrystalEnvironment().restore(originalEnvironment);
		}		
	}

	public void loadData(String filePath, String dataFullName) {
		loadData(filePath, dataFullName, true);
	}
	
	public void loadData(String filePath, String dataFullName, boolean async) {
		if (filePath == null) return;

		for (DiffractionImageData d : model) {
			if (filePath.equals(d.getPath())) {
				return;
			}
		}
		
		DiffractionImageData data = new DiffractionImageData();
		data.setPath(filePath);
		int j = filePath.lastIndexOf(File.separator);
		String fileName = j > 0 ? filePath.substring(j + 1) : "file";
		data.setName(fileName);
		model.add(data);
		PowderFileLoaderJob job = new PowderFileLoaderJob(filePath, dataFullName, data);
		if (async) {
			job.schedule();
		} else {
			job.run(null);
		}

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
		private final DiffractionImageData data;
		
		public PowderFileLoaderJob(String filePath, String dataFullName, DiffractionImageData data) {
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
				dh = ServiceProvider.getService(ILoaderService.class).getData(path, null);

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
					IMetadata metaData = ServiceProvider.getService(ILoaderService.class).getMetadata(path, null);
					final Map<String, int[]> dataShapes = metaData.getDataShapes();
					final List<String> dataNames = new ArrayList<>();
					for (String name : dataShapes.keySet()) {
						int[] shape = dataShapes.get(name);
						if (shape == null) continue;
						int[] ss = ShapeUtils.squeezeShape(shape, false);
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
							
							int[] ss = ShapeUtils.squeezeShape(shape,false);
							
							if (ss.length != 2) {
								if (ss.length == 3) {
									int size = ss[0];
									final List<String> dataNames = new ArrayList<String>();
									for (String name : dataShapes.keySet()) {
										shape = dataShapes.get(name);
										if (shape == null) continue;
										ss = ShapeUtils.squeezeShape(shape, false);
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
					logger.error("Error loading data",e);
				}
			}

			ServiceProvider.getService(IRecentPlaces.class).addFiles(path);

			return setUpImage(path, outName, data) ? Status.OK_STATUS : Status.CANCEL_STATUS;

		}
	}
	
	private boolean setUpImage(String path, String[] datasetNames, DiffractionImageData data){
		if (datasetNames == null) return false;
		
		if (datasetNames[1] == null) {
			return setUpImage(path,datasetNames[0],data);
		}
		
		ILazyDataset ld = null;
		IDataset dist = null;
		try {
			final ILoaderService ldr = ServiceProvider.getService(ILoaderService.class);
			ld = ldr.getData(path, null).getLazyDataset(datasetNames[0]);
			dist = ldr.getData(path, null).getLazyDataset(datasetNames[1]).getSlice();
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
		int count = 0;
		if (it.hasNext()) {
			try {
				IDataset next = it.next().getSlice().squeeze();
				next.setName(fileName + ":" + next.getName());
				data.setImage(next);
				data.setName(fileName + ": " + count);
				data.setMetaData(DiffractionDefaultMetadata.getDiffractionMetadata(next.getShape()));
				data.getImage().setMetadata(data.getMetaData());
				data.setDistance(dist.getDouble(count++));
			} catch (DatasetException e) {
				logger.error("Could not get data from lazy dataset", e);
			}
		}	
		while (it.hasNext()) {
			IDataset n;
			try {
				n = it.next().getSlice().squeeze();
			} catch (DatasetException e) {
				logger.error("Could not get data from lazy dataset", e);
				continue;
			}
			IDiffractionMetadata md = DiffractionDefaultMetadata.getDiffractionMetadata(n.getShape());
			n.setMetadata(md);
			n.setName(fileName + ":" + n.getName() + count);
			DiffractionImageData d = new DiffractionImageData();
			d.setImage(n);
			d.setName(fileName + ": " + count);
			d.setMetaData(md);
			d.setDistance(dist.getDouble(count++));
			model.add(d);
		}
		
		fireDiffractionDataListeners(new DiffractionDataChanged(data));

		return true;
	}

	private boolean setUpImage(String path, String datasetName, DiffractionImageData data){
		ILazyDataset ld = null;
		try {
			IDataHolder dataHolder = ServiceProvider.getService(ILoaderService.class).getData(path, null);
			ld = datasetName == null ? dataHolder.getLazyDataset(0) : dataHolder.getLazyDataset(datasetName);
		} catch (Exception e) {
			logger.error("Could not get dataset ''{}'' at path ''{}''", datasetName, path, e);
			return false;
		}
		
		if (ld == null) return false;
		
		IDataset d;
		try {
			d = ld.getSlice().squeeze();
		} catch (DatasetException e) {
			logger.error("Could not get data from lazy dataset", e);
			return false;
		}
		
//		if (image == null){
//			model.remove(data);
//			fireDiffractionDataListeners(null);
//			return Status.CANCEL_STATUS;
//		}
		
		int j = path.lastIndexOf(File.separator);
		String fileName = j > 0 ? path.substring(j + 1) : null;
		String[] statusString = new String[1];
		IDiffractionMetadata md = DiffractionUtils.getDiffractionMetadata(d, path, service, statusString);
		
		ld.setName(fileName + ":" + ld.getName());
		d.setMetadata(md);
		ld.setMetadata(md);
		data.setImage(d);
		data.setMetaData(md);
		
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
		
		return DatasetFactory.createFromObject(deltaDistance);
	}
	
	public void clear(IDetectorPropertyListener listener) {
		if (!isValidModel()) return;
		if (listener!=null) for (DiffractionImageData d : iterable()) {
			d.getMetaData().getDetector2DProperties().removeDetectorPropertyListener(listener);
		}
		model.clear();
	}

	public void setDiffractionMetadataForAll(IDiffractionMetadata metadata) {
		for (DiffractionImageData data : model) {
			data.setMetaData(metadata.clone());
		}
		fireDiffractionDataListeners(new DiffractionDataChanged(currentData));
	}

}
