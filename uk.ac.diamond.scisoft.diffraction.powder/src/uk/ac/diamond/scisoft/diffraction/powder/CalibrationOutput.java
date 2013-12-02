package uk.ac.diamond.scisoft.diffraction.powder;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;

public class CalibrationOutput {
	
	private double wavelength;
	private AbstractDataset beamCentreX;
	private AbstractDataset beamCentreY;
	private AbstractDataset tilt;
	private AbstractDataset tiltAngle;
	private AbstractDataset distance;
	private double residual;
	
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

}
