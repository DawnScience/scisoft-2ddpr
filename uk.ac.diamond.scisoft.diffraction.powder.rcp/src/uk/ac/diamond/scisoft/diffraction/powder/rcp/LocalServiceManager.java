package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.osgi.service.event.EventAdmin;

public class LocalServiceManager {

	private static ILoaderService lservice;
	private static EventAdmin eventAdmin;

	public static void setLoaderService(ILoaderService s) {
		lservice = s;
	}
	
	public static ILoaderService getLoaderService() {
		return lservice;
	}
	
	public static EventAdmin getEventAdmin() {
		return eventAdmin;
	}

	public static void setEventAdmin(EventAdmin eAdmin) {
		eventAdmin = eAdmin;
	}
	
}