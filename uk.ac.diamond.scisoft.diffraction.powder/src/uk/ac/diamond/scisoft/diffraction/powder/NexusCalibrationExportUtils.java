package uk.ac.diamond.scisoft.diffraction.powder;

import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentNodeFactory;
import org.eclipse.dawnsci.analysis.api.tree.GroupNode;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.january.dataset.IDataset;

public class NexusCalibrationExportUtils {


	public static void saveToNexusFile(IDataset image, IDiffractionMetadata meta, IPowderCalibrationInfo info, String filepath) throws Exception {

		if (!hasExtension(filepath)) {
			filepath += ".nxs";
		}

		INexusFileFactory nff = Activator.getService(INexusFileFactory.class);
		IPersistenceService service = Activator.getService(IPersistenceService.class);


		try (NexusFile nexusFile = nff.newNexusFile(filepath, false)) {

			IPersistentNodeFactory pnf = service.getPersistentNodeFactory();
			GroupNode n = pnf.writePowderCalibrationToFile(meta,image, info);
			nexusFile.createAndOpenToWrite();
			nexusFile.addNode("/entry1", n);

		}
	}

	private static boolean hasExtension(String filePath) {
		int lastIndexOf = filePath.lastIndexOf('.');
		return lastIndexOf >= 0;
	}
}
