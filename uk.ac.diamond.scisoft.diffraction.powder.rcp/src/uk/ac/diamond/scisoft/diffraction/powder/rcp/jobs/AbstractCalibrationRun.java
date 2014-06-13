package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import org.dawb.workbench.ui.diffraction.table.DiffractionDataManager;
import org.dawb.workbench.ui.diffraction.table.DiffractionTableData;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;

public abstract class AbstractCalibrationRun implements IRunnableWithProgress {

	Display display;
	IPlottingSystem plottingSystem;
	DiffractionDataManager manager;
	DiffractionTableData currentData;
	SimpleCalibrationParameterModel params;
	
	private static final String descEllipse = "Reference for ellipse parameter calibration routine";
	
	private static final String doiEllipse ="10.1107/S0021889813022437";
	
	private static final String bibtexEllipse = "@article{hart2013complete, "+
		 "title={Complete elliptical ring geometry provides energy and instrument calibration for synchrotron-based two-dimensional X-ray diffraction},"+
		  "author={Hart, Michael L and Drakopoulos, Michael and Reinhard, Christina and Connolley, Thomas},"+
		 "journal={Journal of applied crystallography},"+
		  "volume={46},"+
		  "number={5},"+
		  "pages={1249--1260},"+
		  "year={2013},"+
		  "publisher={International Union of Crystallography}"+
		"}";
	
	private static final String endnoteEllipse = "%0 Journal Article" +
			"%T Complete elliptical ring geometry provides energy and instrument calibration for synchrotron-based two-dimensional X-ray diffraction"+
			"%A Hart, Michael L"+
			"%A Drakopoulos, Michael"+
			"%A Reinhard, Christina"+
			"%A Connolley, Thomas"+
			"%J Journal of applied crystallography"+
			"%V 46"+
			"%N 5"+
			"%P 1249-1260"+
			"%@ 0021-8898"+
			"%D 2013"+
			"%I International Union of Crystallography";
	
	
	private static String REGION_PREFIX = "Pixel peaks";
	
	public AbstractCalibrationRun(Display display,
			IPlottingSystem plottingSystem,
			DiffractionDataManager manager,
			DiffractionTableData currentData,
			SimpleCalibrationParameterModel params) {
		
		this.display = display;
		this.plottingSystem = plottingSystem;
		this.manager = manager;
		this.params = params;
		this.currentData = currentData;
	}
	
	/**
	 * 
	 * @param plottingSystem
	 */
	public void removeFoundRings(IPlottingSystem plottingSystem) {
		for (IRegion r : plottingSystem.getRegions()) {
			String n = r.getName();
			if (n.startsWith(REGION_PREFIX)) {
				plottingSystem.removeRegion(r);
				}
		}
	}
	
	
	protected void updateOnFinish(final CalibrationOutput output) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				int i = 0;
				for (DiffractionTableData data : manager.iterable()) {
					updateMetaData(data.getMetaData(), output, i);
					data.setCalibrationInfo(output.getCalibrationInfo()[i]);
					i++;
				}

				removeFoundRings(plottingSystem);
			}
		});
	}
	
	protected void updateMetaData(final IDiffractionMetadata md , final CalibrationOutput output, final int i) {
		
		if (display.getThread()!=Thread.currentThread()) {
			display.syncExec(new Runnable() {

				@Override
				public void run() {
					updateMetaData(md, output, i);

				}
			});
		} else {
			PowderCalibration.updateMetadataFromOutput(md, output, i);
		}
		
	}
	
	protected PowderCalibrationInfoImpl createPowderCalibrationInfo(DiffractionTableData data, boolean ellipse) {
		PowderCalibrationInfoImpl info = new PowderCalibrationInfoImpl(CalibrationFactory.getCalibrationStandards().getSelectedCalibrant(),
				data.getPath() + data.getName(), "detector");
		
		if (!ellipse) return info;
		
		info.setCitationInformation(new String[]{descEllipse,doiEllipse,endnoteEllipse,bibtexEllipse});
		return info;
	}

}
