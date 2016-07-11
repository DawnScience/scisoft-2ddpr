package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.roi.IROI;

public interface ICalibrationUIProgressUpdate {

	public void updatePlotData(IDataset data);
	
	public void drawFoundRing(IROI roi);
	
	public void removeRings();
	
	public void completed();
}
