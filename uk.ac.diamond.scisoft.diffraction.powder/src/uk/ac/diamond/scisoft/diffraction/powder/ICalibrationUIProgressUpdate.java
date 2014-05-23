package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

public interface ICalibrationUIProgressUpdate {

	public void updatePlotData(IDataset data);
	
	public void drawFoundRing(IROI roi);
	
	public void removeRings();
}
