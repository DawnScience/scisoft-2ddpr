package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.Objects;

import javax.vecmath.Vector3d;

import org.eclipse.dawnsci.analysis.api.diffraction.DetectorProperties;
import org.eclipse.dawnsci.analysis.api.diffraction.DiffractionCrystalEnvironment;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;

import uk.ac.diamond.scisoft.analysis.io.DiffractionMetadata;

public class MovingBeamCalibrationParameterModel extends SimpleCalibrationParameterModel {
	
	private boolean isOffsetCalibration = false;
	private GeometryCalibrationOptions geomCalibrationOptions = new GeometryCalibrationOptions();
	private OffsetCalibrationOptions offsetCalibrationOptions = new OffsetCalibrationOptions();
		
	public MovingBeamCalibrationParameterModel() {
		setAutomaticCalibration(false);
		setIsPointCalibration(true);
		
	}
	public MovingBeamCalibrationParameterModel(SimpleCalibrationParameterModel toCopy) {
		super(toCopy);
		setAutomaticCalibration(false);
		setIsPointCalibration(true);
	}
	
	public void setOffsetCalibration(boolean isOffsetCal) {
		this.isOffsetCalibration = isOffsetCal;
		//this.isGeometryCalibration = !isOffsetCal;
	}
	
	@Override 
	public void setFloatEnergy(boolean floatEnergy) {
		super.setFloatEnergy(floatEnergy);
		geomCalibrationOptions.floatEnergy = floatEnergy;
	}
	
	@Override
	public void setFloatBeamCentre(boolean floatBeamCentre) {
		super.setFloatBeamCentre(floatBeamCentre);
		geomCalibrationOptions.floatBeamCentre = floatBeamCentre;
	}
	
	@Override
	public void setFloatTilt(boolean floatTilt) {
		super.setFloatTilt(floatTilt);
		geomCalibrationOptions.floatTilt = floatTilt;
	}
	
	@Override
	public void setFloatDistance(boolean floatDistance) {
		super.setFloatDistance(floatDistance);
		geomCalibrationOptions.floatDistance = floatDistance;
	}
	
	public void setFloatOffsetX(boolean floatXOffset) {
		offsetCalibrationOptions.floatX = floatXOffset;
	}
	
	public void setFloatOffsetY(boolean floatYOffset) {
		offsetCalibrationOptions.floatY = floatYOffset;
	}
	
	public void setFloatOffsetZ(boolean floatZOffset) {
		offsetCalibrationOptions.floatZ = floatZOffset;
	}
	
	
	@Override
	public boolean isPointCalibration() {
		return true;
	}
	
	public boolean isGeometryCalibration() {
		return !isOffsetCalibration;
	}
	
	public boolean isOffsetCalibration() {
		return isOffsetCalibration;
	}
	
	
//	public void setIsPointCalibration(boolean optimise) {
//		isPointCalibration = optimise;
//	} no other option for now
	
	public IDiffractionMetadata getMetadata(double[] params, IDiffractionMetadata md) {
		//this needs to code for either a movement refinement or an offset refinement
		return (!isOffsetCalibration) ? geomCalibrationOptions.getMetadata(params,md): offsetCalibrationOptions.getMetadata(params, md);  
	}
	
	@Override
	public double[] getInitialParams(IDiffractionMetadata md) {
		//curently the offsetCalibrationOptions refinement runs slightly differently to the geomCalibrationOptionsGroup
		return (!isOffsetCalibration)? geomCalibrationOptions.getInitialParams(md):null; 
	}
	
	public double[] getInitialOffsets(double[] guessOffsets) {
	  return (isOffsetCalibration)? offsetCalibrationOptions.getInitialOffsets(guessOffsets):null;
	}
	
	@Override
	public MovingBeamCalibrationErrorOutput getErrorOutput(double[] errors) {
		return (!isOffsetCalibration) ? geomCalibrationOptions.getErrorOutput(errors): offsetCalibrationOptions.getErrorOutput(errors);
		
	}
	
	@Override
	public int getNumberOfFloatingParameters() {
		return (!isOffsetCalibration) ? geomCalibrationOptions.getNumberOfFloatingParameters():offsetCalibrationOptions.getNumberOfFloatingParameters();
		
	}
	
	/* 
	 * Get the DiffractionMetadata to compensate for a source movement of x,y,z of the reference system 
	 */
	public static IDiffractionMetadata getOffsetMetadata(final IDiffractionMetadata originalMeta, double sourceX, double sourceY, double sourceZ){
		
//		IDiffractionMetadata newMeta = originalMeta.clone();
		DetectorProperties dp = originalMeta.getDetector2DProperties().clone();
		DiffractionCrystalEnvironment ce = originalMeta.getDiffractionCrystalEnvironment().clone();
		Vector3d origin = dp.getOrigin();
		Vector3d offset = new Vector3d(new double[] {-sourceX,-sourceY,-sourceZ});
		offset.add(origin);
		dp.setOrigin(offset);
		return new DiffractionMetadata("", dp,ce);
		
	}
	
	public static  IDiffractionMetadata getOffsetMetadata(IDiffractionMetadata originalMeta, double[] sourceOffsets) { 
		return getOffsetMetadata(originalMeta, sourceOffsets[0], sourceOffsets[1], sourceOffsets[2]);
		
	}
	
	/*
	 * get a detector that has the same calibrated q-space values but is rotated around the beam axis. The mathematics of this are based on the 
	 * angle axis rotation matrix formulation which generates the active rotation matrix that would transform a vector (and not the space). 
	 */
	public static IDiffractionMetadata getRotatedMetadata(IDiffractionMetadata originalMeta, double angleInDegrees){
		DetectorProperties dp = originalMeta.getDetector2DProperties().clone();
		DiffractionCrystalEnvironment ce = originalMeta.getDiffractionCrystalEnvironment().clone();
		dp.getNormalAnglesInDegrees();

		PowderCalibration.setDetectorFastAxisAngle(dp, -angleInDegrees); 
		
		dp.getNormalAnglesInDegrees();
		return new DiffractionMetadata(originalMeta.getFilePath()+"_rot",dp,ce);			
		
	}
	
	private class GeometryCalibrationOptions {
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = getOuterType().hashCode();
			result = prime * result + (floatDistance ? 1231 : 1237);
			result = prime * result + (floatEnergy ? 1231 : 1237);
			result = prime * result + (floatBeamCentre ? 1231 : 1237);
			result = prime * result + (floatTilt ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (getClass() != obj.getClass())
				return false;
			GeometryCalibrationOptions other = (GeometryCalibrationOptions) obj;
			if (floatBeamCentre != other.floatBeamCentre)
				return false;
			if (floatTilt != other.floatTilt)
				return false;
			return true;
		}
		public boolean floatDistance = true;
		public boolean floatEnergy = true;
		public boolean floatBeamCentre = true;
		public boolean floatTilt = true;
		
		public GeometryCalibrationOptions(){};

		public MovingBeamCalibrationErrorOutput getErrorOutput(double[] errors) {

			if (errors == null) return null;

			int count = 0;

			if (floatEnergy) count++;
			if (floatDistance) count++;
			if (floatBeamCentre) count+=2;
			if (floatTilt) count+=3;

			MovingBeamCalibrationErrorOutput ceo = new MovingBeamCalibrationErrorOutput(); 
			// this needs to be updated to allow for the additional specification of the full roll, pitch, and yaw. 

			if (errors.length != count) return null;

			count = 0;

			if (floatEnergy) ceo.setWavelength(errors[count++]);
			if (floatDistance) ceo.setDistance(errors[count++]);
			if (floatBeamCentre) {
				ceo.setBeamCentreX(errors[count++]);
				ceo.setBeamCentreY(errors[count++]);
			}
			if (floatTilt) {
				//TODO check if normal angles [2] is zero, if not calc better vals
				ceo.setYaw(errors[count++]);
				ceo.setPitch(errors[count++]);
				ceo.setRoll(errors[count++]);
			}



			return ceo;
		}

		public int getNumberOfFloatingParameters() {

			int count = 0;
			if (floatEnergy) count++;
			if (floatDistance) count++;
			if (floatBeamCentre) count+=2;
			if (floatTilt) count+=3;

			return count;

		}
	
		public double[] getInitialParams(IDiffractionMetadata md) {

			DiffractionCrystalEnvironment de = md.getDiffractionCrystalEnvironment();
			DetectorProperties detProp = md.getDetector2DProperties();

			int count = 0;

			if (floatEnergy) count++;
			if (floatDistance) count++;
			if (floatBeamCentre) count+=2;
			if (floatTilt) count+=3;

			double[] initParam = new double[count];

			count = 0;

			if (floatEnergy) initParam[count++] = de.getWavelength();
			if (floatDistance) initParam[count++] = detProp.getBeamCentreDistance();
			if (floatBeamCentre) {
				double[] bcc = detProp.getBeamCentreCoords();
				initParam[count++] = bcc[0];
				initParam[count++] = bcc[1];
			}
			if (floatTilt) {
				//TODO check if normal angles [2] is zero, if not calc better vals
				double[] na = detProp.getNormalAnglesInDegrees();
				initParam[count++] = na[0];
				initParam[count++] = na[1];
				initParam[count++] = na[2];
			}

			return initParam;

		}
		
		public IDiffractionMetadata getMetadata(double[] params, final IDiffractionMetadata md) {
			
			DetectorProperties odp = md.getDetector2DProperties();
			
			final int heightInPixels = odp.getPy();
			final int widthInPixels = odp.getPx();
			final double pixelHeightInMM = odp.getVPxSize();
			final double pixelWidthInMM = odp.getHPxSize();
			
			
			DiffractionCrystalEnvironment odce = md.getDiffractionCrystalEnvironment();
			DiffractionCrystalEnvironment ndce = new DiffractionCrystalEnvironment();
			
			int count = 0;
			
			if (floatEnergy) {
				ndce.setWavelength(params[count++]);
			} else {
				ndce.setWavelength(odce.getWavelength());
			}
			
			double distance = odp.getBeamCentreDistance();
			double[] beamCentre = odp.getBeamCentreCoords();
			double[] normAngle = odp.getNormalAnglesInDegrees();
			
			if (floatDistance) distance = params[count++];
			
			if (floatBeamCentre) {
				beamCentre[0] = params[count++];
				beamCentre[1] = params[count++];
			}
			
			if (floatTilt) {
				normAngle[0] = params[count++];
				normAngle[1] = params[count++];
				normAngle[2] = params[count++];
			}
			// By returning this new instance of a detector currently any settings in the original metadata such as beam vector direction are not currently persisted  
			DetectorProperties d = new DetectorProperties(distance,
					beamCentre[0]*pixelHeightInMM, beamCentre[1]*pixelWidthInMM, heightInPixels, widthInPixels, pixelHeightInMM, pixelWidthInMM);
			d.setNormalAnglesInDegrees(normAngle[0], normAngle[1], normAngle[2]);
			
			return new DiffractionMetadata(null, d, ndce);
		}

		private MovingBeamCalibrationParameterModel getOuterType() {
			return MovingBeamCalibrationParameterModel.this;
		}
		
	}
	
	private class OffsetCalibrationOptions {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = getOuterType().hashCode();
			result = prime * result + (floatX ? 1231 : 1237);
			result = prime * result + (floatY ? 1231 : 1237);
			result = prime * result + (floatZ ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (getClass() != obj.getClass())
				return false;
			OffsetCalibrationOptions other = (OffsetCalibrationOptions) obj;
			if (floatX != other.floatX || floatY != other.floatY ||floatZ != other.floatZ)
				return false;
			return true;
		}

		public boolean floatX = true;
		public boolean floatY = true;
		public boolean floatZ = false;

		public OffsetCalibrationOptions(){};

		public MovingBeamCalibrationErrorOutput getErrorOutput(double[] errors) {

			if (errors == null) return null;

			int count = 0;

			if (floatX) count++;
			if (floatY) count++;
			if (floatZ) count++;

			MovingBeamCalibrationErrorOutput ceo = new MovingBeamCalibrationErrorOutput(); 
			if (errors.length != count) return null;

			count = 0;
			if (floatX) ceo.setX(errors[count++]);
			if (floatY) ceo.setY(errors[count++]);
			if (floatZ) ceo.setZ(errors[count++]);
			return ceo;
		}

		public int getNumberOfFloatingParameters() {

			int count = 0;
			if (floatX) count++;
			if (floatY) count++;
			if (floatZ) count++;
			return count;

		}

		public double[] getInitialOffsets(double[] guessOffsets) {

			int npars = this.getNumberOfFloatingParameters();
			double[] offsets = new double[npars]; 
			int count = 0;
			if (floatX) offsets[count] = guessOffsets[count++];
			if (floatY) offsets[count] = guessOffsets[count++];
			if (floatZ) offsets[count] = guessOffsets[count++];

			return offsets;
		}

		public IDiffractionMetadata getMetadata(double[] params, IDiffractionMetadata md) {

			int count =0;
			double[] offsets = new double[3];
			if (floatX) offsets[0] = params[count++];
			if (floatY) offsets[1] = params[count++];
			if (floatZ) offsets[2] = params[count++];

			return MovingBeamCalibrationParameterModel.getOffsetMetadata(md, offsets);
		}

		private MovingBeamCalibrationParameterModel getOuterType() {
			return MovingBeamCalibrationParameterModel.this;
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(geomCalibrationOptions, isOffsetCalibration, offsetCalibrationOptions);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MovingBeamCalibrationParameterModel other = (MovingBeamCalibrationParameterModel) obj;
		return Objects.equals(geomCalibrationOptions, other.geomCalibrationOptions)
				&& isOffsetCalibration == other.isOffsetCalibration
				&& Objects.equals(offsetCalibrationOptions, other.offsetCalibrationOptions);
	}
	
}
