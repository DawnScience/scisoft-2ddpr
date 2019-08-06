package uk.ac.diamond.scisoft.diffraction.powder.application;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationConfig;

public class PowderCalibrationApplication implements IApplication {
	
	private static Logger logger = LoggerFactory.getLogger(PowderCalibrationApplication.class);
	private static final String PATH = "path";
	private static final String FIXED_W = "fixedWavelength";
	private static final String FIXED_D = "fixedDistance";
	

	@Override
	public Object start(IApplicationContext context) throws Exception {
		// Parse out the configuration.
		final Map      args          = context.getArguments();
		final String[] configuration = (String[])args.get("application.args");
		
		logger.info("Running calibration with configuration: {}", Arrays.toString(configuration));

		Map<String, String> conf = new HashMap<String, String>(7);
		for (int i = 0; i < configuration.length; i++) {
			final String pkey = configuration[i];
			if (pkey.startsWith("-" + PATH)) {
				conf.put(pkey.substring(1), configuration[i+1]);
				i++;
			} else if (pkey.startsWith("-" + FIXED_D)) {
				conf.put(pkey.substring(1), configuration[i+1]);
				i++;
			} else if (pkey.startsWith("-" + FIXED_W)) {
				conf.put(pkey.substring(1), configuration[i+1]);
				i++;
			}
		}
		
		if (!conf.containsKey(PATH)) {
			System.out.println("No path to config file");
			return IApplication.EXIT_OK;
		}
		
		if (conf.containsKey(FIXED_W) && conf.containsKey(FIXED_D)) {
			System.out.println("Cannot fix wavelength and distance");
			return IApplication.EXIT_OK;
		}
		
	    String path = conf.get(PATH);
		
		logger.info("Loading configuration from {}", path);
		
		PowderCalibrationConfig config = loadCalibrationConfig(path);
		
		CalibrationExecution runner = new CalibrationExecution(config);
		
		if (conf.containsKey(FIXED_W)) {
			String val = conf.get(FIXED_W);
			double wavelength = Double.parseDouble(val);
			config.getModel().setFloatEnergy(false);
			runner.setFixedValue(wavelength);
		}
		
		if (conf.containsKey(FIXED_D)) {
			String val = conf.get(FIXED_D);
			double distance = Double.parseDouble(val);
			config.getModel().setFloatDistance(false);
			runner.setFixedValue(distance);
		}
		
		runner.run();
		
		return IApplication.EXIT_OK;
	}

	private PowderCalibrationConfig loadCalibrationConfig(String path) throws Exception {
		
		String json = new String(Files.readAllBytes(new File(path).toPath()));
		
		BundleContext bundleContext =
                FrameworkUtil.
                getBundle(this.getClass()).
                getBundleContext();
		
		IMarshallerService m = bundleContext.getService(bundleContext.getServiceReference(IMarshallerService.class));
		
		
		PowderCalibrationConfig config = m.unmarshal(json, PowderCalibrationConfig.class);
		
		return config;
	}
	
	@Override
	public void stop() {
		//do nothing
	}

}
