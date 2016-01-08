package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;

public class CalibrationOutput {
	
	private double wavelength;
	private Dataset beamCentreX;
	private Dataset beamCentreY;
	private Dataset tilt;
	private Dataset tiltAngle;
	private Dataset distance;
	private double residual;
	private IPowderCalibrationInfo calibrationInfo[];
	private double[] errors;
	
	public CalibrationOutput(double wavelength, Dataset beamCentreX, Dataset beamCentreY,
			Dataset tilt, Dataset tiltAngle, Dataset distance, double residual, double[] errors) {
		this.wavelength = wavelength;
		this.beamCentreX = beamCentreX;
		this.beamCentreY = beamCentreY;
		this.tilt = tilt;
		this.tiltAngle = tiltAngle;
		this.distance = distance;
		this.residual = residual;
		this.errors = errors;
	}
	
	public CalibrationOutput(double wavelength, double beamCentreX, double beamCentreY,
			double tilt, double tiltAngle, double distance, double residual, double[] errors) {
		this(wavelength,
				new DoubleDataset(new double[]{beamCentreX}, new int[]{1}),
				new DoubleDataset(new double[]{beamCentreY}, new int[]{1}),
				new DoubleDataset(new double[]{tilt}, new int[]{1}),
				new DoubleDataset(new double[]{tiltAngle}, new int[]{1}),
				new DoubleDataset(new double[]{distance}, new int[]{1}),
				residual, errors);
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

	public double[] getErrors() {
		return errors;
	}

}
