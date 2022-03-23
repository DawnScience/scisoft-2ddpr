package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentNodeFactory;
import org.eclipse.dawnsci.analysis.api.tree.GroupNode;
import org.eclipse.dawnsci.analysis.api.tree.Node;
import org.eclipse.dawnsci.analysis.tree.TreeFactory;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusConstants;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;

import uk.ac.diamond.scisoft.analysis.io.NexusTreeUtils;

public class NexusCalibrationExportUtils {


	public static void saveToNexusFile(IDataset image, IDiffractionMetadata meta, IPowderCalibrationInfo info, String filepath) throws Exception {
		saveToNexusFile(image, null, meta, info, filepath);
	}

	private static boolean hasExtension(String filePath) {
		int lastIndexOf = filePath.lastIndexOf('.');
		return lastIndexOf >= 0;
	}


	public static void saveToNexusFile(IDataset image, IDataset position, IDiffractionMetadata meta,
			IPowderCalibrationInfo info, String filepath) throws Exception {
		if (!hasExtension(filepath)) {
			filepath += ".nxs";
		}
		INexusFileFactory nff = Activator.getService(INexusFileFactory.class);
		IPersistenceService service = Activator.getService(IPersistenceService.class);
		try (NexusFile nexusFile = nff.newNexusFile(filepath, false)) {
			IPersistentNodeFactory pnf = service.getPersistentNodeFactory();
			GroupNode n = pnf.writePowderCalibrationToFile(meta, image, info);
			if (position != null) {
				n.getGroupNode("calibration_data").addDataNode("position",
						NexusTreeUtils.createDataNode("", position, "mm"));
			}

			nexusFile.createAndOpenToWrite();
			nexusFile.addNode("/entry1", n);
		}
	}


	/**
	 * Save a calibrated frame using a format compatible with moving beam operations. This format results in a link being established for the calibration_data group 
	 * to the position at which the calibration was taken. 
	 * <p>
	 * For a moving beam, the xDatasetName and yDatasetName variables would be the motor position dataset paths 
	 * for the raw axis motors recording the source axis movements, e.g. /entry/diffraction/kb_x for xDatasetPath,  and /entry/diffraction/kb_y for yDatasetPath.   
	 * 
	 * @param image The calibrated frame to save in the file
	 * @param position dataset containing the position information for the frame
	 * @param meta Metadata containing the diffraction geometry
	 * @param info Information on the calibration 
	 * @param filepath Location to which the file will be saved 
	 * @param xDatasetPath Path in the nexus file to store the source position x information
	 * @param yDatasetPath Path in the nexus file to store the source position y information
	 * @throws Exception
	 */
	public static void saveToMovingBeamCompatibleNexusFile(IDataset image, IDataset position, IDiffractionMetadata meta,
			IPowderCalibrationInfo info, String filepath, String xDatasetPath, String yDatasetPath) throws Exception {
		if (!hasExtension(filepath)) {
			filepath += ".nxs";
		}
		final String D0NAME = "dimension_0";
		final String D1NAME = "dimension_1";
		INexusFileFactory nff = Activator.getService(INexusFileFactory.class);
		IPersistenceService service = Activator.getService(IPersistenceService.class);
		try (NexusFile nexusFile = nff.newNexusFile(filepath, false)) {
			IPersistentNodeFactory pnf = service.getPersistentNodeFactory();
			GroupNode n = pnf.writePowderCalibrationToFile(meta, image, info);
			GroupNode calibrationGroup = n.getGroupNode("calibration_data");
			calibrationGroup.addAttribute(TreeFactory.createAttribute(NexusConstants.DATA_AXES,
					new String[] { D0NAME, D1NAME, NexusConstants.DATA_AXESEMPTY, NexusConstants.DATA_AXESEMPTY }));
			calibrationGroup.addAttribute(TreeFactory.createAttribute(D0NAME + NexusConstants.DATA_INDICES_SUFFIX, 0));
			calibrationGroup.addAttribute(TreeFactory.createAttribute(D1NAME + NexusConstants.DATA_INDICES_SUFFIX, 1));

			//TODO set the reshape appropriately for the rank of the image/ scan 

			Dataset p0 = DatasetFactory.createFromObject(position.getDouble(0)).reshape(1, 1, 1, 1);
			Dataset p1 = DatasetFactory.createFromObject(position.getDouble(1)).reshape(1, 1, 1, 1);
			nexusFile.createAndOpenToWrite();
			nexusFile.addNode("/entry", n);

			String[] splitXName = splitDatasetNameAndGroupPath(xDatasetPath);
			String[] splitYName = splitDatasetNameAndGroupPath(yDatasetPath);
			nexusFile.createData(splitXName[0],splitXName[1], p0, true);
			nexusFile.createData(splitYName[0],splitYName[1], p1, true);
			nexusFile.link(xDatasetPath, "/entry/calibration_data/dimension_0");
			nexusFile.link(yDatasetPath, "/entry/calibration_data/dimension_1");
		}
	}


	public static String[] splitDatasetNameAndGroupPath(String fullPath) {
		String s1 = fullPath.substring(0,fullPath.lastIndexOf(Node.SEPARATOR) );
		String s2 = fullPath.substring(fullPath.lastIndexOf(Node.SEPARATOR)+1);
		return new String[] {s1,s2};

	}
}
