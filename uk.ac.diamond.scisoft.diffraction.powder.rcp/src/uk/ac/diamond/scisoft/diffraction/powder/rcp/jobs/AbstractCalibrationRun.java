package uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs;

import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.DiffractionImageData;
import uk.ac.diamond.scisoft.diffraction.powder.ICalibrationUIProgressUpdate;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibration;
import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationInfoImpl;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public abstract class AbstractCalibrationRun implements IRunnableWithProgress {

	DiffractionDataManager manager;
	DiffractionImageData currentData;
	SimpleCalibrationParameterModel params;
	ICalibrationUIProgressUpdate uiUpdater;
	
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
	
	public AbstractCalibrationRun(DiffractionDataManager manager,
			SimpleCalibrationParameterModel params,
			ICalibrationUIProgressUpdate uiUpdater) {
		

		this.manager = manager;
		this.params = params;
		this.currentData = manager.getCurrentData();
		this.uiUpdater = uiUpdater;
	}
	
	protected void updateOnFinish(final CalibrationOutput output) {

		if (output == null) return;
		int i = 0;
		for (DiffractionImageData data : manager.iterable()) {
			updateMetaData(data.getMetaData(), output, i);
			data.setCalibrationInfo(output.getCalibrationInfo()[i]);
			i++;
		}

		uiUpdater.removeRings();
	}

	protected void updateMetaData(final IDiffractionMetadata md , final CalibrationOutput output, final int i) {

		Double roll = null;
		if (params.isFixDetectorRoll()) {
			roll = md.getDetector2DProperties().getNormalAnglesInDegrees()[2];
		}

		PowderCalibration.updateMetadataFromOutput(md, output, i, roll);

	}
	
	protected PowderCalibrationInfoImpl createPowderCalibrationInfo(DiffractionImageData data, boolean ellipse) {
		PowderCalibrationInfoImpl info = new PowderCalibrationInfoImpl(CalibrationFactory.getCalibrationStandards().getSelectedCalibrant(),
				data.getPath() + data.getName(), "detector");
		
		if (!ellipse) return info;
		
		info.setCitationInformation(new String[]{descEllipse,doiEllipse,endnoteEllipse,bibtexEllipse});
		return info;
	}

}
