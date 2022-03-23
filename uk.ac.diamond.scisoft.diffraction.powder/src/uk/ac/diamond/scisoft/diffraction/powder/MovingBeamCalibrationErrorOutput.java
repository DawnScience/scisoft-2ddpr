package uk.ac.diamond.scisoft.diffraction.powder;

public class MovingBeamCalibrationErrorOutput extends CalibrationErrorOutput {
		private Double z;
		private Double y;
		private Double x;
		private Double yaw;
		private Double pitch;
		private Double roll;
		
		public Double getX() {
			return x;
		}
		
		public void setX(Double xshift) {
			this.x = xshift;
		}
		
		public Double getY() {
			return y;
		}
		
		public void setY(Double yshift) {
			this.y = yshift;
		}
		
		public Double getZ() {
			return z;
		}
		
		public void setZ(Double zshift) {
			this.z = zshift;
		}
		
		public Double getYaw() {
			return yaw;
		}
		
		public void setYaw(Double yaw) {
			this.yaw = yaw;
		}
		
		public Double getPitch() {
			return pitch;
		}
		
		public void setPitch(Double pitch) {
			this.pitch = pitch;
		}
		
		public Double getRoll() {
			return roll;
		}
		
		public void setRoll(Double roll) {
			this.roll = roll; 
		}
		
	}