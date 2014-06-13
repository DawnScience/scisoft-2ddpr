package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.IPowderCalibrationInfo;

public class PowderCalibrationInfoImpl implements IPowderCalibrationInfo {
	
	private String calibrantName;
	private String imagePath;
	private String detectorName;
	private String description;
	private IDataset dSpace;
	private IDataset indices;
	private double residual;
	private String[] citation;
	
	public PowderCalibrationInfoImpl() {
		this.calibrantName = "Not supplied";
		this.imagePath = "Not supplied";
		this.detectorName = "detector";
	}
	
	public PowderCalibrationInfoImpl(String calibrantName, String imagePath, String detectorName) {
		
		this.calibrantName = calibrantName;
		this.imagePath = imagePath;
		this.detectorName = detectorName;
		
	}
	
	public void setPostCalibrationInformation(String description, IDataset dSpace, IDataset indices, double residual) {
		this.description = description;
		this.dSpace = dSpace;
		this.indices = indices;
		this.residual = residual;
	}
	
	@Override
	public String getCalibrantName() {
		return calibrantName;
	}

	@Override
	public String getCalibrationImagePath() {
		return imagePath;
	}

	@Override
	public String getDetectorName() {
		return detectorName;
	}

	@Override
	public String getMethodDescription() {
		return description;
	}

	@Override
	public IDataset getCalibrantDSpaceValues() {
		return dSpace;
	}

	@Override
	public IDataset getUsedDSpaceIndexValues() {
		return indices;
	}

	@Override
	public double getResidual() {
		return residual;
	}

	@Override
	public String[] getCitationInformation() {
		return citation;
	}
	
	public void setCitationInformation(String[] cite) {
		citation = cite;
	}


}
