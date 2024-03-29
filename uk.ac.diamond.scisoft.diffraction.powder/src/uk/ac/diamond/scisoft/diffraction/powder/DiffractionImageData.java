/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package uk.ac.diamond.scisoft.diffraction.powder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.diffraction.IPowderCalibrationInfo;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.january.dataset.IDataset;

/**
 * Data item used in the table viewer of the Diffraction calibration views
 */
public class DiffractionImageData {
	
	private String path;
	private String name;
	private IDiffractionMetadata metaData;
	private IDataset image;
	private double distance = 0;
	private boolean use = false;
	private IPowderCalibrationInfo calibrationInfo;
	private List<IROI> rois; 
	private int nrois = -1;
	
	public DiffractionImageData() {
		rois = new ArrayList<>();
	}

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public IDiffractionMetadata getMetaData() {
		return metaData;
	}
	
	public void setMetaData(IDiffractionMetadata md) {
		this.metaData = md;
		image.setMetadata(md);
	}
	
	public IDataset getImage() {
		return image;
	}
	public void setImage(IDataset image) {
		this.image = image;
	}
	
	public IROI getRoi(int i) {
		return rois.get(i);
	}
	
	public void addROI(IROI roi) {
		rois.add(roi);
	}
	
	public void clearROIs(){
		rois.clear();
	}
	
	
	public int getROISize(){
		return rois.size();
	}
	
	public int getNonNullROISize(){
		int totalNonNull = 0;
		
		for (IROI roi : rois) {
			if (roi != null) totalNonNull++;
		}
		
		return totalNonNull;
	}

	public double getDistance() {
		return distance;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public int getNrois() {
		return nrois;
	}
	
	public void setNrois(int nrois) {
		this.nrois = nrois;
	}
	
	public boolean isUse() {
		return use;
	}
	
	public void setUse(boolean use) {
		this.use = use;
	}
	
	public double getResidual() {
		if (calibrationInfo != null) return calibrationInfo.getResidual();
		else return 0;
	}

	public IPowderCalibrationInfo getCalibrationInfo() {
		return calibrationInfo;
	}
	public void setCalibrationInfo(IPowderCalibrationInfo calibrationInfo) {
		this.calibrationInfo = calibrationInfo;
	}
}
