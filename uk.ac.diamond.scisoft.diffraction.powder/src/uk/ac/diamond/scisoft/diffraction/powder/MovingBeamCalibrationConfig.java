package uk.ac.diamond.scisoft.diffraction.powder;
public class MovingBeamCalibrationConfig extends PowderCalibrationConfig {

	private boolean floatEnergy = true;
	private double[] referenceAxesPositionXY;
	private String xReferenceAxisPath;
	private String yReferenceAxisPath;
	private String scanKeyPath = "/entry/diamond_scan/keys/";
	private int padWithZeros = 3;
	
	
	public void setFloatEnergy(boolean flag) {
		floatEnergy = flag;
	}
	
	public void setReferenceAxesPositionXY(double[] pos) {
		referenceAxesPositionXY = pos;
	}
	
	public void setXReferenceAxisPath(String name) {
		xReferenceAxisPath = name;
	}
	
	public void setYReferenceAxisPath(String name) {
		yReferenceAxisPath = name;
	}
	
	public void setScanKeyPath(String name) {
		scanKeyPath = name;
	}
	
	public void setPadWithZeros(int pad) {
		padWithZeros = pad;
	}
	
	public boolean getFloatEnergy() {
		return floatEnergy;
	}
	
	public double[] getReferenceAxesPositionXY() {
		return referenceAxesPositionXY;
	}
	
	public String getYReferenceAxisPath() {
		return yReferenceAxisPath;
	}
	
	public String getXReferenceAxisPath() {
		return xReferenceAxisPath;
	}
	
	public String getScanKeyPath() {
		return scanKeyPath;
	}
	
	public int getPadWithZeros() {
		return padWithZeros;
	}
	
	
	

}