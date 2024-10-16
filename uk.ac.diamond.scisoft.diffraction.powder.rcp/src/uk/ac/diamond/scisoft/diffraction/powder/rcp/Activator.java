package uk.ac.diamond.scisoft.diffraction.powder.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.ac.diamond.scisoft.diffraction.powder.rcp"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private static BundleContext bundleContext;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		bundleContext = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Get image descriptor from given path
	 * @param path plugin relative path of image file
	 * @return image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Get image from given path. The caller should dispose of it
	 * @param path plugin relative path of image file
	 * @return image
	 */
	public static Image getImage(String path) {
		return getImageDescriptor(path).createImage();
	}

	/**
	 * Get image from given path and add dispose listener so caller does not need to dispose
	 * @param w widget
	 * @param path plugin relative path of image file
	 * @return image
	 */
	public static Image getImageAndAddDisposeListener(Widget w, String path) {
		Image i = getImageDescriptor(path).createImage();
		w.addDisposeListener(e -> i.dispose());
		return i;
	}

	public static <T> T getService(final Class<T> serviceClass) {
		if (plugin == null) return null;
		ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
		if (ref == null) return null;
		return bundleContext.getService(ref);
	}
}
