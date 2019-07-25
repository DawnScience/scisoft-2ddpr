package uk.ac.diamond.scisoft.diffraction.powder;

public class PowderCalibrationConfig {

	private String inputPath;
	private String outputPath;
	private String datasetPath;
	private String initialCalibration;
	private String standard;
	
	private SimpleCalibrationParameterModel model;
	
	public String getInputPath() {
		return inputPath;
	}

	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getDatasetPath() {
		return datasetPath;
	}

	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}

	public String getInitialCalibration() {
		return initialCalibration;
	}

	public void setInitialCalibration(String initialCalibration) {
		this.initialCalibration = initialCalibration;
	}

	public SimpleCalibrationParameterModel getModel() {
		return model;
	}

	public void setModel(SimpleCalibrationParameterModel model) {
		this.model = model;
	}
	
	public String getStandard() {
		return standard;
	}

	public void setStandard(String standard) {
		this.standard = standard;
	}


}
