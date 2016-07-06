package uk.ac.diamond.scisoft.diffraction.powder;

import java.text.DecimalFormat;

import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;

public class CalibrationOutput {
	
	private double wavelength;
	private Dataset beamCentreX;
	private Dataset beamCentreY;
	private Dataset tilt;
	private Dataset tiltAngle;
	private Dataset distance;
	private double residual;
	private IPowderCalibrationInfo calibrationInfo[];
	private CalibrationErrorOutput errors;
	
	public CalibrationOutput(double wavelength, Dataset beamCentreX, Dataset beamCentreY,
			Dataset tilt, Dataset tiltAngle, Dataset distance, double residual, CalibrationErrorOutput errors) {
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
			double tilt, double tiltAngle, double distance, double residual, CalibrationErrorOutput errors) {
		this(wavelength,
				DatasetFactory.createFromObject(new double[]{beamCentreX}),
				DatasetFactory.createFromObject(new double[]{beamCentreY}),
				DatasetFactory.createFromObject(new double[]{tilt}),
				DatasetFactory.createFromObject(new double[]{tiltAngle}),
				DatasetFactory.createFromObject(new double[]{distance}),
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

	public String getCalibrationOutputDescription() {
		if (beamCentreX.getSize() > 1) return "Multi-Image Calibration,/nWavelength (Angstrom): " + wavelength + "/nResidual: " + residual;
		
		CalibrationErrorOutput e = errors == null ? new CalibrationErrorOutput() : errors;
		
		DecimalFormat df7 = new DecimalFormat("#.#######");
		DecimalFormat df4 = new DecimalFormat("#.####"); 
		
		StringBuilder sb = new StringBuilder();
		sb.append("Single Image Calibration:\n");
		sb.append("Wavelength (Angstrom):\t" + df7.format(wavelength));
		if (e.getWavelength() != null) sb.append(" (\u00B1" + df7.format(e.getWavelength()) + ")");
		sb.append("\n");
		sb.append("Distance (mm):\t\t" + df4.format(distance.getDouble(0)));
		if (e.getDistance() != null) sb.append(" (\u00B1" + df4.format(e.getDistance().doubleValue()) + ")");
		sb.append("\n");
		sb.append("Beam Centre X (pixel):\t" + df4.format(beamCentreX.getDouble(0)));
		if (e.getBeamCentreX() != null) sb.append(" (\u00B1" + df4.format(e.getBeamCentreX().doubleValue()) + ")");
		sb.append("\n");
		sb.append("Beam Centre Y (pixel):\t" + df4.format(beamCentreY.getDouble(0)));
		if (e.getBeamCentreY() != null) sb.append(" (\u00B1" + df4.format(e.getBeamCentreY().doubleValue()) + ")");
		sb.append("\n");
		sb.append("Tilt (degrees):\t\t" + df4.format(tilt.getDouble(0)));
		if (e.getTilt() != null) sb.append(" (\u00B1" + df4.format(e.getTilt().doubleValue()) + ")");
		sb.append("\n");
		sb.append("Tilt Angle (degrees):\t" + df4.format(tiltAngle.getDouble(0)));
		if (e.getTiltAngle() != null) sb.append(" (\u00B1" + df4.format(e.getTiltAngle().doubleValue()) + ")");
		sb.append("\n");
		sb.append("Residual:\t\t" + residual);
		
		return sb.toString();
	}

}
