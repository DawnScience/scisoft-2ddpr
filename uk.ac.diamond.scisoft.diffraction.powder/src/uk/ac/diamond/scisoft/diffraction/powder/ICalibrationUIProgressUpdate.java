package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.january.dataset.IDataset;

public interface ICalibrationUIProgressUpdate {

	public void updatePlotData(IDataset data);
	
	public void drawFoundRing(IROI roi);
	
	public void removeRings();
	
	public void completed();
}
