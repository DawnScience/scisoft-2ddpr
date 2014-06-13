package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.diffraction.IPowderCalibrationInfo;

public class CalibrationOutput {
	
	private double wavelength;
	private AbstractDataset beamCentreX;
	private AbstractDataset beamCentreY;
	private AbstractDataset tilt;
	private AbstractDataset tiltAngle;
	private AbstractDataset distance;
	private double residual;
	private IPowderCalibrationInfo calibrationInfo[];
	
	public CalibrationOutput(double wavelength, AbstractDataset beamCentreX, AbstractDataset beamCentreY,
			AbstractDataset tilt, AbstractDataset tiltAngle, AbstractDataset distance, double residual) {
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

	public AbstractDataset getBeamCentreX() {
		return beamCentreX;
	}

	public AbstractDataset getBeamCentreY() {
		return beamCentreY;
	}

	public AbstractDataset getTilt() {
		return tilt;
	}

	public AbstractDataset getTiltAngle() {
		return tiltAngle;
	}

	public AbstractDataset getDistance() {
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
