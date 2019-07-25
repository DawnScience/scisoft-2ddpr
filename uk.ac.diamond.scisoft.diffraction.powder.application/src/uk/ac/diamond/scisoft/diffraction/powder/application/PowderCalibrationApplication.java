package uk.ac.diamond.scisoft.diffraction.powder.application;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import uk.ac.diamond.scisoft.diffraction.powder.PowderCalibrationConfig;

public class PowderCalibrationApplication implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		// Parse out the configuration.
		final Map      args          = context.getArguments();
		final String[] configuration = (String[])args.get("application.args");

		Map<String, String> conf = new HashMap<String, String>(7);
		for (int i = 0; i < configuration.length; i++) {
			final String pkey = configuration[i];
			if (pkey.startsWith("-")) {
				conf.put(pkey.substring(1), configuration[i+1]);
				i++;
			}
		}
		
		PowderCalibrationConfig config = loadCalibrationConfig(conf);
		
		CalibrationExecution runner = new CalibrationExecution(config);
		
		runner.run();
		
		return IApplication.EXIT_OK;
	}

	private PowderCalibrationConfig loadCalibrationConfig(Map<String, String> conf) throws Exception {
		final String path   = conf.get("path");
		
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
