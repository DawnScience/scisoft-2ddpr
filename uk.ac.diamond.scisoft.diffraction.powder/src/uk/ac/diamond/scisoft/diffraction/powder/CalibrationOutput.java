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
		if (beamCentreX.getSize() > 1) return "Multi-Image Calibration,\nWavelength (Angstrom): " + wavelength + "\nResidual: " + residual;
		
		CalibrationErrorOutput e = errors == null ? new CalibrationErrorOutput() : errors;
		
		DecimalFormat df7 = new DecimalFormat("#.#######");
		DecimalFormat df4 = new DecimalFormat("#.####"); 
		
		StringBuilder sb = new StringBuilder();
		sb.append("Single Image Calibration:\n");
		sb.append("Wavelength (Angstrom):\t" + toStringDoubleWithError(wavelength, e.getWavelength(),df7));
		sb.append("\n");
		sb.append("Distance (mm):\t\t" + toStringDoubleWithError(distance.getDouble(0), e.getDistance(),df4));
		sb.append("\n");
		sb.append("Beam Centre X (pixel):\t" + toStringDoubleWithError(beamCentreX.getDouble(0), e.getBeamCentreX(),df4));
		sb.append("\n");
		sb.append("Beam Centre Y (pixel):\t" + toStringDoubleWithError(beamCentreY.getDouble(0), e.getBeamCentreY(),df4));
		sb.append("\n");
		sb.append("Tilt (degrees):\t\t" + toStringDoubleWithError(tilt.getDouble(0), e.getTilt(),df4));
		sb.append("\n");
		sb.append("Tilt Angle (degrees):\t" + toStringDoubleWithError(tiltAngle.getDouble(0), e.getTiltAngle(),df4));
		sb.append("\n");
		sb.append("Residual:\t\t" + residual);
		
		return sb.toString();
	}
	
	private String toStringDoubleWithError(double d, Double e, DecimalFormat df) {
		
		if (e == null) return df.format(d);
		
		int floor = (int)Math.floor(Math.log10(Math.abs(e)));
		DecimalFormat decimal = new DecimalFormat();
		decimal.setMaximumFractionDigits(1+(floor*-1));
		decimal.setMinimumFractionDigits(1+(floor*-1));
		String dString = decimal.format(d);
		
		double eDisplay = e*Math.pow(10, -1*floor+1);
		DecimalFormat dError = new DecimalFormat();
		dError.setMaximumFractionDigits(0);
		String eString = dError.format(eDisplay);
		
		
		return dString + "(" + eString + ")";
	}

}
