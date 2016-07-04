package uk.ac.diamond.scisoft.diffraction.powder.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.junit.Test;

import uk.ac.diamond.scisoft.diffraction.powder.CalibrateEllipses;
import uk.ac.diamond.scisoft.diffraction.powder.CalibrationOutput;
import uk.ac.diamond.scisoft.diffraction.powder.CentreFitter;
import uk.ac.diamond.scisoft.diffraction.powder.ImageFitter;
import uk.ac.diamond.scisoft.diffraction.powder.LambdaFitter;

/**
 * Testing of powder diffraction calibration output against the I12 matlab values.
 * <p>
 * Ellipse parameters provided by I12
 * 
 */
public class PowderCalibrationTest {

	@Test
	public void CentreFitterTestRun(){
		
		double[] x1 = new double[]{ 1.051534146000000e+03,
			     1.051522594000000e+03,
			     1.051461949000000e+03,
			     1.051404403000000e+03,
			     1.051192974000000e+03,
			     1.051130796000000e+03};
		double[] y1 = new double[]{1.017426516000000e+03,
			     1.017416492000000e+03,
			     1.017346237000000e+03,
			     1.017426316000000e+03,
			     1.017000531000000e+03,
			     1.016915176000000e+03};
		
		double[] expected = new double[]{1.304139779445208e+00,-3.538840672325122e+02};
		
		Dataset xd = DatasetFactory.createFromObject(x1);
		Dataset yd = DatasetFactory.createFromObject(y1);
		
		double[] out = CentreFitter.fit(xd, yd);
		
		assertArrayEquals(expected, out, 0.1);
	}
	
	@Test
	public void ImageFitterTestRun(){
		
		double[] major = new double[]{ 1.978623226000000e+02,
			    2.289998490000000e+02,
			    3.269572920000000e+02,
			    3.863322507000000e+02,
			    5.891121037000000e+02,
			    6.296698940000000e+02,};
		double[] x1 = new double[]{ 1.051534146000000e+03,
			     1.051522594000000e+03,
			     1.051461949000000e+03,
			     1.051404403000000e+03,
			     1.051192974000000e+03,
			     1.051130796000000e+03};
		double[] y1 = new double[]{1.017426516000000e+03,
			     1.017416492000000e+03,
			     1.017346237000000e+03,
			     1.017426316000000e+03,
			     1.017000531000000e+03,
			     1.016915176000000e+03};
		
		double[] centre_line = new double[]{1.304139779445208e+00, -3.538840672325122e+02};
		
		double pixel = 0.2;
		
		double[] expected = new double[]{5.625050069043890e+05,1.051567822834576e+03};
		//min = 5.271676377107889e+03

		Dataset majord = DatasetFactory.createFromObject(major);
		Dataset xd = DatasetFactory.createFromObject(x1);
		Dataset yd = DatasetFactory.createFromObject(y1);
		
		double[] out = ImageFitter.fit(majord, xd, yd, centre_line, pixel);
		assertArrayEquals(expected, out, 0.1);
	}
	
	@Test
	public void LambdaFitterTestRun(){

		double[] major = new double[]{1.978623226000000e+02,
			     2.664923407000000e+02,
			     3.349086362000000e+02,
			     4.032880611000000e+02,
			     4.718452089000000e+02,
			     2.289998490000000e+02,
			     3.084443199000000e+02,
			     3.876439970000000e+02,
			     4.668016189000000e+02,
			     5.461261239000000e+02,
			     3.269572920000000e+02,
			     4.403830670000000e+02,
			     5.534721177000000e+02,
			     6.665223527000001e+02,
			     7.798386047000000e+02,
			     3.863322507000000e+02,
			     5.201523241000000e+02,
			     6.537351122000000e+02,
			     7.872816047000000e+02,
			     9.211465724000000e+02,
			     5.891121037000000e+02,
			     7.936230341000000e+02,
			     6.296698940000000e+02,
			     8.482866401000000e+02};

		double[] dist = new double[]{ 4.000082859860544e+02,
				3.000062144895408e+02,
				2.000041429930272e+02,
				1.000020714965136e+02,
				0,
				4.000082859860544e+02,
				3.000062144895408e+02,
				2.000041429930272e+02,
				1.000020714965136e+02,
				0,
				4.000082859860544e+02,
				3.000062144895408e+02,
				2.000041429930272e+02,
				1.000020714965136e+02,
				0,
				4.000082859860544e+02,
				3.000062144895408e+02,
				2.000041429930272e+02,
				1.000020714965136e+02,
				0,
				4.000082859860544e+02,
				3.000062144895408e+02,
				4.000082859860544e+02,
				3.000062144895408e+02};

	double[] dspace = new double[] {3.124417584000000e+00,
			3.124417584000000e+00,
			3.124417584000000e+00,
			3.124417584000000e+00,
			3.124417584000000e+00,
			2.705825000000000e+00,
			2.705825000000000e+00,
			2.705825000000000e+00,
			2.705825000000000e+00,
			2.705825000000000e+00,
			1.913307206000000e+00,
			1.913307206000000e+00,
			1.913307206000000e+00,
			1.913307206000000e+00,
			1.913307206000000e+00,
			1.631673868000000e+00,
			1.631673868000000e+00,
			1.631673868000000e+00,
			1.631673868000000e+00,
			1.631673868000000e+00,
			1.104648431000000e+00,
			1.104648431000000e+00,
			1.041472528000000e+00,
			1.041472528000000e+00};
	
	double[] sint = new double[] {5.625050069043890e+05,
		     8.293984234111260e+05,
		     1.012256637470937e+06,
		     1.067427117768487e+06,
		     1.175891902012124e+06,
		     5.625050069043890e+05,
		     8.293984234111260e+05,
		     1.012256637470937e+06,
		     1.067427117768487e+06,
		     1.175891902012124e+06,
		     5.625050069043890e+05,
		     8.293984234111260e+05,
		     1.012256637470937e+06,
		     1.067427117768487e+06,
		     1.175891902012124e+06,
		     5.625050069043890e+05,
		     8.293984234111260e+05,
		     1.012256637470937e+06,
		     1.067427117768487e+06,
		     1.175891902012124e+06,
		     5.625050069043890e+05,
		     8.293984234111260e+05,
		     5.625050069043890e+05,
		     8.293984234111260e+05};
	
	double pixel = 0.2;
	
	Dataset majord = DatasetFactory.createFromObject(major);
	Dataset distd = DatasetFactory.createFromObject(dist);
	Dataset dspaced = DatasetFactory.createFromObject(dspace);
	Dataset sintd = DatasetFactory.createFromObject(sint);
	
	double[] expected = new double[]{6.890468342740828e+02, 4.250447566358648e-01};
	
	double[] out = LambdaFitter.fit(majord.imultiply(pixel), distd, dspaced, sintd.imultiply(pixel), 659, 0.14);
	
	assertArrayEquals(expected, out, 0.1);
}

	@Test
	public void CalibrateTestRun(){
		//EllipticalROI roi = new EllipticalROI()double major, double minor, double angle, double ptx, double pty;
		CalibrationStruct st = getCalibrationStruct(5);
		List<List<EllipticalROI>> ellipses =  st.ellipses;
		List<double[]> allDSpacings = st.dSpacings;
		
		double[] deltaDist = new double[]{259,
				   359,
				   459,
				   559,
				   659};
		
		Dataset deltad = DatasetFactory.createFromObject(deltaDist);
		
		CalibrationOutput output = CalibrateEllipses.run(ellipses, allDSpacings,deltad,0.2);
		
		assertEquals(4.250447566358648e-01, output.getWavelength(), 0.00001);
		assertEquals(1.050750806712749e+03, output.getBeamCentreX().getDouble(2), 0.00001);
		assertEquals(1.017214604396353e+03, output.getBeamCentreY().getDouble(2), 0.00001);
		assertEquals(1.384041774843228e-01, output.getTilt().getDouble(2), 0.000001);
		assertEquals(1.369538319263075e+02, output.getTiltAngle().getDouble(2), 0.001);
		assertEquals(4.890426912810556e+02, output.getDistance().getDouble(2), 0.01);
		
	}
	
	@Test
	public void CalibrateKnownWavelength() {
		CalibrationStruct st = getCalibrationStruct(1);
		List<List<EllipticalROI>> ellipses =  st.ellipses;
		List<double[]> allDSpacings = st.dSpacings;

		CalibrationOutput output = CalibrateEllipses.runKnownWavelength(ellipses, allDSpacings,0.2,4.250447566358648e-01);
		
		assertEquals(4.250447566358648e-01, output.getWavelength(), 0.000001);
		assertEquals(1.051567822834576e+03, output.getBeamCentreX().getDouble(0), 0.00001);
		assertEquals(1.017507361310649e+03, output.getBeamCentreY().getDouble(0), 0.00001);
		assertEquals(1.472049755053897e-01, output.getTilt().getDouble(0), 0.0001);
		assertEquals(1.274805927288948e+02, output.getTiltAngle().getDouble(0), 0.001);
		assertEquals(289.0386307906849, output.getDistance().getDouble(0), 0.1);
	}
	
	@Test
	public void CalibrateKnownDistance() {
		CalibrationStruct st = getCalibrationStruct(1);
		List<List<EllipticalROI>> ellipses =  st.ellipses;
		List<double[]> allDSpacings = st.dSpacings;
		
		CalibrationOutput output = CalibrateEllipses.runKnownDistance(ellipses, allDSpacings,0.2,289.0386307906849);
		
		assertEquals(4.250447566358648e-01, output.getWavelength(), 0.001);
		assertEquals(1.051567822834576e+03, output.getBeamCentreX().getDouble(0), 0.00001);
		assertEquals(1.017507361310649e+03, output.getBeamCentreY().getDouble(0), 0.00001);
		assertEquals(1.472049755053897e-01, output.getTilt().getDouble(0), 0.0001);
		assertEquals(1.274805927288948e+02, output.getTiltAngle().getDouble(0), 0.001);
		assertEquals(289.0386307906849, output.getDistance().getDouble(0), 0.1);
	}
	
	
	@Test
	public void CalibrateTestRunTilted(){
		CalibrationStruct st = getCalibrationStructTilted();
		
		List<List<EllipticalROI>> ellipses =  st.ellipses;
		List<double[]> allDSpacings = st.dSpacings;
		
		double[] deltaDist = new double[]{580,780,980,1180,1380, 1580, 1780, 1980};
		
		Dataset deltad = DatasetFactory.createFromObject(deltaDist);
		
		CalibrationOutput output = CalibrateEllipses.run(ellipses, allDSpacings,deltad,0.148);
		
		assertEquals(1.408202018705987e-01, output.getWavelength(), 0.000001);
		assertEquals(1.439670961792122e+03, output.getBeamCentreX().getDouble(2), 0.00001);
		assertEquals(1.444618931598524e+03, output.getBeamCentreY().getDouble(2), 0.00001);
		assertEquals(6.345158939804580e+00, output.getTilt().getDouble(2), 0.0001);
		assertEquals(-4.060456750324394e-01, output.getTiltAngle().getDouble(2), 0.001);
		assertEquals(9.917933454838078e+02, output.getDistance().getDouble(2), 0.01);
	}
	
	@Test
	public void CalibrateTestRunTiltedFixedWavelength(){
		CalibrationStruct st = getCalibrationStructTilted();
		
		List<List<EllipticalROI>> ellipses =  new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		ellipses.add(st.ellipses.get(0));
		allDSpacings.add(st.dSpacings.get(0));
		
		
		CalibrationOutput output = CalibrateEllipses.runKnownWavelength(ellipses, allDSpacings, 0.148, 1.408202018705987e-01);
		
		assertEquals(1437.456167355806, output.getBeamCentreX().getDouble(0), 0.00001);
		assertEquals(1443.6626304492486, output.getBeamCentreY().getDouble(0), 0.00001);
		assertEquals(6.345171501781432, output.getTilt().getDouble(0), 0.2);
		assertEquals(-0.5730278231886173, output.getTiltAngle().getDouble(0), 0.001);
		assertEquals(591.7905349403466, output.getDistance().getDouble(0), 0.05);
	}
	
	
	@Test
	public void CalibrateTestRunTiltedFixedWavelengthLast(){
		CalibrationStruct st = getCalibrationStructTilted();
		
		List<List<EllipticalROI>> ellipses =  new ArrayList<List<EllipticalROI>>();
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		ellipses.add(st.ellipses.get(7));
		allDSpacings.add(st.dSpacings.get(7));
		
		
		//CalibrationOutput output = CalibrateEllipses.run(ellipses, allDSpacings,deltad,0.148);
		CalibrationOutput output = CalibrateEllipses.runKnownWavelength(ellipses, allDSpacings, 0.148, 1.408202018705987e-01);
		
		assertEquals(1442.3815669173396, output.getBeamCentreX().getDouble(0), 0.00001);
		assertEquals(1445.6281483308721, output.getBeamCentreY().getDouble(0), 0.00001);
		assertEquals(6.37206298655499, output.getTilt().getDouble(0), 0.2);
		assertEquals(-0.2191573440183457, output.getTiltAngle().getDouble(0), 0.001);
		assertEquals(1991.800498735658, output.getDistance().getDouble(0), 0.05);
	}
	
	
	private CalibrationStruct getCalibrationStruct(int nRings) {
		
		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>(5);
		List<double[]> allDSpacings = new ArrayList<double[]>();
		
		
		try {
			for (int i = 0; i < nRings; i++) {
				List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();
				
				int max = 4;
				
				if (i < 2) max = 6;
				
				double[] dspacings = new double[max];
					
				for (int j = 1; j< 5; j++) {
					loadEllipse(i,j,j,dspacings,ellipses,".ellipse");
				}
				
				if (i < 2) {
					loadEllipse(i,9,5,dspacings,ellipses,".ellipse");
					loadEllipse(i,10,6,dspacings,ellipses,".ellipse");
				}
				
				allDSpacings.add(dspacings);
				allEllipses.add(ellipses);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CalibrationStruct st = new CalibrationStruct();
		st.ellipses = allEllipses;
		st.dSpacings = allDSpacings;
		return st;
	}
	
	private CalibrationStruct getCalibrationStructTilted() {

		List<List<EllipticalROI>> allEllipses = new ArrayList<List<EllipticalROI>>(5);
		List<double[]> allDSpacings = new ArrayList<double[]>();


		try {
			for (int i = 0; i < 8; i++) {
				List<EllipticalROI> ellipses = new ArrayList<EllipticalROI>();

				int max = 4;

				if (i < 5) max = 6;
				if (i == 5) max = 5;

				double[] dspacings = new double[max];

				for (int j = 1; j< 5; j++) {
					loadEllipse(i,j,j,dspacings,ellipses,".ellipse_t");
				}

				if (i < 5) {
					loadEllipse(i,9,5,dspacings,ellipses,".ellipse_t");
					loadEllipse(i,10,6,dspacings,ellipses,".ellipse_t");
				}

				if (i == 5) loadEllipse(i,9,5,dspacings,ellipses,".ellipse_t");

				allDSpacings.add(dspacings);
				allEllipses.add(ellipses);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CalibrationStruct st = new CalibrationStruct();
		st.ellipses = allEllipses;
		st.dSpacings = allDSpacings;
		return st;
	}

	private void loadEllipse(int i, int j, int j2, double[] dspacings,List<EllipticalROI> ellipses, String ext) throws Exception{
		File file = new File(getTestFilePath("0000" + String.valueOf(i)+"_"+ String.valueOf(j)+ext));
		BufferedReader br = new BufferedReader(new FileReader(file));
		String edata = br.readLine();
		String[] strVals = edata.split(",");
		strVals.toString();
		EllipticalROI roi = new EllipticalROI(Double.parseDouble(strVals[3]),
				Double.parseDouble(strVals[4])
				,Math.toRadians(Double.parseDouble(strVals[8])),
				Double.parseDouble(strVals[5]),Double.parseDouble(strVals[6]));
		dspacings[j2-1] = Double.parseDouble(strVals[7]);
		ellipses.add(roi);
		br.close();
	}
	
	private String getTestFilePath(String fileName) {
		final File test = new File("testfiles/"+fileName);
		return test.getAbsolutePath();
	}
	
	private class CalibrationStruct {
		public List<List<EllipticalROI>> ellipses;
		public List<double[]> dSpacings;
	}
	
	
}
