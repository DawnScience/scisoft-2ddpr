package uk.ac.diamond.scisoft.diffraction.powder.rcp.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.dataset.mask.MaskCircularBuffer;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.january.MetadataException;
import org.eclipse.january.dataset.BooleanDataset;
import org.eclipse.january.dataset.Comparisons;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ShapeUtils;
import org.eclipse.january.metadata.IMetadata;
import org.eclipse.january.metadata.MaskMetadata;
import org.eclipse.january.metadata.MetadataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;

import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.LocalServiceManager;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.MaskChangedEvent;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.MaskingOptionsGroup.MaskState;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.widget.MaskingOptionsGroup;

public class CalibrationMaskingManager {

	private MaskState currentMaskType = MaskState.NOMASK;
	private IPlottingSystem<?> plottingSystem;
	private SimpleCalibrationParameterModel model;
	private DiffractionDataManager manager;
	private MaskingOptionsGroup maskOptions;
	private Button applyMaskButton;
	private final static Logger logger = LoggerFactory.getLogger(CalibrationMaskingManager.class);
	

	public CalibrationMaskingManager(IPlottingSystem<?> plot, DiffractionDataManager manager,
			SimpleCalibrationParameterModel model, MaskingOptionsGroup maskOptions) {
		this.plottingSystem = plot;
		this.manager = manager;
		this.model = model;
		this.maskOptions = maskOptions;
		this.applyMaskButton = maskOptions.getMaskApplicationButton();

		applyMaskButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean maskApplied = associateMaskToImageData();
				model.setIsMasked(maskApplied);

			}
		});
		maskOptions.addCalibrationMaskChangedListener((MaskChangedEvent event) -> {
			MaskState newMaskType = event.getMaskType();
			model.setIsMasked(false); // the mask needs to be associated with the dataset before he update is
										// reflected in the model
			if (newMaskType == MaskState.NOMASK)
				removeMaskFromData();
			applyMaskButton.setEnabled((newMaskType == MaskState.FROMTOOL || newMaskType == MaskState.INTERNAL));
			currentMaskType = newMaskType;
		});
	}

	private IDataset getMaskFromPlottingSystem() {
		Collection<ITrace> traces = plottingSystem.getTraces();
		IImageTrace imageTrace = (IImageTrace) traces.iterator().next();
		return (imageTrace != null) ? imageTrace.getMask() : null;
	}
	
	/**
	 * get existing masks from the MaskMetadata stored with the current DiffractionMetadata Image and the existing metadata from the plotting system 
	 * @param data
	 * @return
	 */
	private IDataset getExistingMasks(DiffractionImageData data) {
		IDataset existingMask = null;
		List<MaskMetadata> existingMaskMetadatas = new ArrayList<>();
		
		try {
			List<MaskMetadata> existingMasks = data.getImage().getMetadata(MaskMetadata.class);
			if (existingMasks!=null) existingMaskMetadatas.addAll(existingMasks);
		} catch (MetadataException e) {
			// do nothing
		}
		
		IDataset plotMask = getMaskFromPlottingSystem();
		if (plotMask!=null) {
			try {
				existingMaskMetadatas.add(MetadataFactory.createMetadata(MaskMetadata.class, plotMask));
			}catch (MetadataException e) {
					//do nothing
				}
		}
		
		if (!existingMaskMetadatas.isEmpty()) {

			Iterator<MaskMetadata> it = existingMaskMetadatas.stream().filter(mm -> (mm.getMask()!=null)).iterator();

			while (it.hasNext()) {
				MaskMetadata mm = it.next();
				existingMask = (existingMask != null) ? Comparisons.logicalAnd(mm.getMask(), existingMask): mm.getMask();
			}
		}

		return existingMask;
	}

	/**
	 * create a new mask accounting for any pre-existing masks over the data into a
	 * new single MaskMetadata
	 * 
	 * @return boolean flag indicating if mask update was successful
	 */
	public boolean associateMaskToImageData() {
		boolean maskApplied = false;
		if (currentMaskType != MaskState.NOMASK) {
			DiffractionImageData data = manager.getCurrentData();

			IDataset existingMask = getExistingMasks(data);
			IDataset newMask = null;

			if (currentMaskType == MaskState.INTERNAL) {
				MaskLoaderJob job = new MaskLoaderJob(data.getPath(), data.getImage().getShape());
				job.run(null);
				newMask = Comparisons.logicalNot(job.getData());
				
			}

			IDataset compoundMask = null;
			if (newMask != null && existingMask != null) {
				compoundMask = Comparisons.logicalAnd(existingMask, newMask);
			} else {
				compoundMask = (existingMask == null) ? newMask : existingMask;
			}
			
			MaskUpdateJob job = new MaskUpdateJob(plottingSystem, compoundMask, data);
			job.run(null);
			maskOptions.updateMaskSummaryInfo(job.getMaskSummaryInfo());
			maskApplied = job.isMasked();
			manager.setCurrentData(job.getData());
			
		}
		return maskApplied;
	}

	private void removeMaskFromData() {
		applyMaskButton.setEnabled(false);
		DiffractionImageData data = manager.getCurrentData();
		MaskUpdateJob job = new MaskUpdateJob(plottingSystem, null, data);
		job.run(null);
		manager.setCurrentData(job.getData());
		maskOptions.updateMaskSummaryInfo(job.getMaskSummaryInfo());
	}

	
	
	private class MaskUpdateJob extends Job{
		
		private IDataset mask;
		private boolean maskApplied;
		private DiffractionImageData data;
		private String info;
		private IPlottingSystem<?> plottingSystem;
		private static final String NOMASKINFO = "No mask on data";
		
		public MaskUpdateJob(IPlottingSystem<?> plottingSystem, IDataset mask, DiffractionImageData data ){
			super("Updating mask info");
			this.mask = mask;
			this.data = data;
			this.plottingSystem = plottingSystem;
			this.info = NOMASKINFO; //initialise to a default
			this.maskApplied=false;
		}
		
		public IStatus run(IProgressMonitor monitor) {
			if (mask!=null) {
				MaskMetadata mm = null;
				try {
					mm = MetadataFactory.createMetadata(MaskMetadata.class, mask);
				} catch (MetadataException e) {
					return Status.CANCEL_STATUS;
				}
				data.getImage().setMetadata(mm);
				maskApplied = true;
				Collection<ITrace> traces = plottingSystem.getTraces();
				IImageTrace imageTrace = (IImageTrace) traces.iterator().next();
				imageTrace.setMask(mask);
				
				BooleanDataset invMask = Comparisons.logicalNot(DatasetUtils.cast(BooleanDataset.class, mask));
				int nPxMasked = (int) invMask.sum(true);
				info = String.format("Number of pixels masked:%n%,d", nPxMasked);
				return Status.OK_STATUS;
			}
			data.getImage().clearMetadata(MaskMetadata.class); // N.B. this will clear all associated mask metadata
			Collection<ITrace> traces = plottingSystem.getTraces();
			IImageTrace imageTrace = (IImageTrace) traces.iterator().next();
			imageTrace.setMask(null);
			info = NOMASKINFO;
			return Status.OK_STATUS;
		}
		
		public DiffractionImageData getData() {
			return data;
		}
		
		public boolean isMasked() {
			return maskApplied;
		}
		
		public String getMaskSummaryInfo() {
			// Summarise the important statistics on how many pixels are masked
			return info;
			
		}
	}

	private class MaskLoaderJob extends Job {

		private final String path;
		private final int[] imageShape;
		private IDataset data;
		private MaskCircularBuffer buffer;

		public MaskLoaderJob(String filePath, int[] imageShape) {
			super("Load mask from internal");
			this.buffer = new MaskCircularBuffer(imageShape);
			this.path = filePath;
			this.imageShape = imageShape;
			this.data = null;
			
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			IDataHolder dh = null;
			try {
				dh = LocalServiceManager.getLoaderService().getData(path, null);
			} catch (Exception e1) {
				return Status.CANCEL_STATUS;
			}

			final String[] outName = new String[1];

			try {
				IMetadata metaData = LocalServiceManager.getLoaderService().getMetadata(path, null);
				final Map<String, int[]> dataShapes = metaData.getDataShapes();
				final List<String> dataNames = new ArrayList<String>();
				for (String name : dataShapes.keySet()) {
					int[] shape = dataShapes.get(name);
					if (shape == null)
						continue;
					int[] ss = ShapeUtils.squeezeShape(shape, false);
					if (ss.length == 2 && Arrays.equals(ss, imageShape)) {
						dataNames.add(name);
					}
				}

				if (dataNames.isEmpty()) {
					logger.info("No compatible datasets found in file!");
					return Status.CANCEL_STATUS;

				}

				Display.getDefault().syncExec(new Runnable() {

					@Override
					public void run() {
						ListDialog dia = new ListDialog(Display.getDefault().getActiveShell());
						dia.setTitle("Choose a viable dataset for mask");
						dia.setMessage("Select dataset:");
						dia.setContentProvider(new ArrayContentProvider());
						dia.setLabelProvider(new LabelProvider());
						dia.setInput(dataNames);
						if (dia.open() == ListDialog.OK) {
							outName[0] = dia.getResult()[0].toString();
						}
					}

				});
				buffer.merge(DatasetUtils.sliceAndConvertLazyDataset(dh.getLazyDataset(outName[0])));
				data = buffer.getMask();
				return Status.OK_STATUS;

			} catch (Exception e) {
				logger.error("Error loading mask. No mask applied");
				return Status.CANCEL_STATUS;
			}

		}
		
		public IDataset getData() {
			return this.data;
		}

	}
}
