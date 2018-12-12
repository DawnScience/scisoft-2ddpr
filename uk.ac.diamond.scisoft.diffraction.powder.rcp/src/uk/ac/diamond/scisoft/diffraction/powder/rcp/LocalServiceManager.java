package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.dawnsci.datavis.api.IRecentPlaces;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.osgi.service.event.EventAdmin;

public class LocalServiceManager {

	private static ILoaderService lservice;
	private static EventAdmin eventAdmin;
	private static IPersistenceService pservice;
	private static IRecentPlaces recentPlaces;

	public void setLoaderService(ILoaderService s) {
		lservice = s;
	}
	
	public static ILoaderService getLoaderService() {
		return lservice;
	}
	
	public static EventAdmin getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(EventAdmin eAdmin) {
		eventAdmin = eAdmin;
	}
	
	public void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}
	
	public static IPersistenceService getPersistenceService() {
		return pservice;
	}
	
	public void setRecentPlaces(IRecentPlaces rp) {
		recentPlaces = rp;
	}
	
	public static IRecentPlaces getRecentPlaces() {
		return recentPlaces;
	}
	
}