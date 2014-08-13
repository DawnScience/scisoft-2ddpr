package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.IPowderCalibrationInfo;

public class CalibrationOutput {
	
	private double wavelength;
	private Dataset beamCentreX;
	private Dataset beamCentreY;
	private Dataset tilt;
	private Dataset tiltAngle;
	private Dataset distance;
	private double residual;
	private IPowderCalibrationInfo calibrationInfo[];
	
	public CalibrationOutput(double wavelength, Dataset beamCentreX, Dataset beamCentreY,
			Dataset tilt, Dataset tiltAngle, Dataset distance, double residual) {
		this.wavelength = wavelength;
		this.beamCentreX = beamCentreX;
		this.beamCentreY = beamCentreY;
		this.tilt = tilt;
		this.tiltAngle = tiltAngle;
		this.distance = distance;
		this.residual = residual;
	}
	
	public CalibrationOutput(double wavelength, double beamCentreX, double beamCentreY,
			double tilt, double tiltAngle, double distance, double residual) {
		this(wavelength,
				new DoubleDataset(new double[]{beamCentreX}, new int[]{1}),
				new DoubleDataset(new double[]{beamCentreY}, new int[]{1}),
				new DoubleDataset(new double[]{tilt}, new int[]{1}),
				new DoubleDataset(new double[]{tiltAngle}, new int[]{1}),
				new DoubleDataset(new double[]{distance}, new int[]{1}),
				residual);
	}

	public double getWavelength() {
		return wavelength;
	}

	public Dataset getBeamCentreX() {
		return beamCentreX;
	}

	public Dataset getBeamCentreY() {
		return beamCentreY;
	}

	public Dataset getTilt() {
		return tilt;
	}

	public Dataset getTiltAngle() {
		return tiltAngle;
	}

	public Dataset getDistance() {
		return distance;
	}
	
	public double getResidual() {
		return residual;
	}

	public IPowderCalibrationInfo[] getCalibrationInfo() {
		return calibrationInfo;
	}

	public void setCalibrationInfo(IPowderCalibrationInfo[] calibrationInfo) {
		this.calibrationInfo = calibrationInfo;
	}

}
