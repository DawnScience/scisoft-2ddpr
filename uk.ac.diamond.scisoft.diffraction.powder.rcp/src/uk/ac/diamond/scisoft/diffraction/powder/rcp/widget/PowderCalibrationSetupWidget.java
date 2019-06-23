package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.HashSet;
import java.util.Set;

import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationFactory;
import uk.ac.diamond.scisoft.analysis.crystallography.CalibrationStandards;
import uk.ac.diamond.scisoft.diffraction.powder.SimpleCalibrationParameterModel;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.Activator;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.calibration.CalibrantPositioningWidget;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.CalibrationUIProgressUpdateImpl;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.jobs.POIFindingRun;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.preferences.DiffractionCalibrationConstants;
import uk.ac.diamond.scisoft.diffraction.powder.rcp.table.DiffractionDataManager;

public class PowderCalibrationSetupWidget {
	
	private Set<ICalibrationStateListener> listeners;
	
	private DiffractionDataManager manager;
	private SimpleCalibrationParameterModel model;
	private DiffractionImageAugmenter augmenter;
	private IPlottingSystem<?> system;
	private IRunner runner;
	private POIFindingRun poiFindingRun = null;

	private boolean showSteering = false;
	private boolean showRings = true;

	private CalibrantPositioningWidget cpw;
	private CalibrationOptionsGroup options;
	private Button usePoints;

	public PowderCalibrationSetupWidget(DiffractionDataManager manager, 
			SimpleCalibrationParameterModel model, DiffractionImageAugmenter augmenter,
			IPlottingSystem<?> system, IRunner runner){
		this.manager = manager;
		this.model = model;
		this.augmenter = augmenter;
		this.system = system;
		this.runner = runner;
		listeners = new HashSet<>();
	}
	
	
	public void createControl(final Composite left) {
		
	 CTabFolder folder = new CTabFolder(left, SWT.NONE);
	 folder.setLayout(new GridLayout());
	 folder.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());
		
	 CTabItem tab1 = new CTabItem(folder, SWT.NONE);
	    tab1.setText("Calibrant");
	    
	    Composite calComp = new Composite(folder, SWT.NONE);
	    calComp.setLayoutData(GridDataFactory.fillDefaults().create());
	    calComp.setLayout(new GridLayout());
		
		final CalibrantSelectionGroup group = new CalibrantSelectionGroup(calComp);
//		tab1.setControl(group.getControl());
		
		group.addDisplaySelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showRings = group.getShowRings();
				showCalibrantAndBeamCentre(group.getShowRings());
			}
		});
		
		final RingSelectionGroup ringSelection = new RingSelectionGroup(calComp, CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size(), model);
		ringSelection.addRingNumberSpinnerListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ringNumber = ringSelection.getRingSpinnerSelection();
				model.setNumberOfRings(ringNumber);
				augmenter.setMaxCalibrantRings(ringNumber);
				augmenter.setRingSet(ringSelection.getRingSelectionText().getUniqueRingNumbers());
				//force redraw
				if (augmenter.isActive()) augmenter.activate();
//				augmenter.activate();
			}
		});
		
		tab1.setControl(calComp);
		
		group.addCalibrantSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (manager.getCurrentData() == null)
					return;
				String calibrantName = group.getCalibrant();
				// update the calibrant in diffraction tool
				CalibrationFactory.getCalibrationStandards().setSelectedCalibrant(calibrantName, true);
				// set the maximum number of rings
				int ringMaxNumber = CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size();
				augmenter.setMaxCalibrantRings(ringMaxNumber);
				ringSelection.setMaximumRingNumber(ringMaxNumber);
				ringSelection.setRingSpinnerSelection(ringMaxNumber);
				if (group.getShowRings()){
					augmenter.activate();
				}
			}
		});
		
		Composite routineComposite = new Composite(folder, SWT.NONE);
		routineComposite.setLayout(new GridLayout());
		routineComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		
		final Button autoRadio = new Button(routineComposite, SWT.RADIO);
		autoRadio.setText("Automatic");
		GridDataFactory.swtDefaults().applyTo(autoRadio);

		final Button manualRadio = new Button(routineComposite, SWT.RADIO);
		manualRadio.setText("Manual");
		GridDataFactory.swtDefaults().applyTo(manualRadio);
		manualRadio.setSelection(false);
		
		final Composite autoManStack = new Composite(routineComposite, SWT.NONE);
		final StackLayout stackLayout = new StackLayout();
		autoManStack.setLayout(stackLayout);
		final Label auto = new Label(autoManStack,SWT.WRAP);
		auto.setText("Automatic Calibration");
		GridDataFactory.swtDefaults().applyTo(autoManStack);
		
		
		final Composite manualComposite = new Composite(autoManStack, SWT.None);
		manualComposite.setLayout(new GridLayout());
		IDiffractionMetadata meta = null;
		if (manager.getCurrentData() != null) meta = manager.getCurrentData().getMetaData();
		if (showSteering) {
			cpw = new CalibrantPositioningWidget(manualComposite, meta);
		} else {
			Label l = new Label(manualComposite, SWT.WRAP);
			l.setText("Align rings to image");
			GridDataFactory.swtDefaults().applyTo(l);
		}
		
		final Button findRings = new Button(manualComposite, SWT.PUSH);
		findRings.setText("Find Rings");
		GridDataFactory.swtDefaults().applyTo(findRings);
		stackLayout.topControl = auto;
		autoManStack.layout();

		poiFindingRun = new POIFindingRun(new CalibrationUIProgressUpdateImpl(system, Display.getCurrent()){
			@Override
			public void completed() {
				fireListeners(new StateChangedEvent(this, canRun()));
			}
			
		}, manager.getCurrentData(), model);
		findRings.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setMaxSearchSize(Activator.getDefault().getPreferenceStore().getInt(DiffractionCalibrationConstants.MAX_SEARCH_SIZE));
				
				poiFindingRun.updateData(manager.getCurrentData());
					runner.run(poiFindingRun);
			}
		});
		
		model.setAutomaticCalibration(true);
		model.setIsPointCalibration(true);
		usePoints = new Button(routineComposite, SWT.CHECK);
		GridDataFactory.swtDefaults().applyTo(usePoints);
		usePoints.setText("Point calibration");
		usePoints.setSelection(true);
		CTabItem tab3 = new CTabItem(folder, SWT.NONE);
	    tab3.setText("Routine");
	    tab3.setControl(routineComposite);
		
		options = new CalibrationOptionsGroup(folder, model, true,true);
		
		CTabItem tab4 = new CTabItem(folder, SWT.NONE);
	    tab4.setText("Options");
	    tab4.setControl(options.getControl());
		
		usePoints.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean s = ((Button)e.getSource()).getSelection();
				model.setIsPointCalibration(s);
				boolean showPoint = s && !model.isAutomaticCalibration(); 
				options.showOptions(!showPoint, showPoint);
				autoManStack.layout();
				fireListeners(new StateChangedEvent(this, canRun()));
			}
		});
		
		
		
		autoRadio.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean s = ((Button)e.getSource()).getSelection();
				boolean showPoint = !s && model.isPointCalibration(); 
				if (manager.getSize() > 1){
					options.enable(false);
				} else {
					options.showOptions(!showPoint, showPoint);
				}		
				
				stackLayout.topControl = s ? auto : manualComposite;
				autoManStack.layout();
				model.setAutomaticCalibration(s);
				fireListeners(new StateChangedEvent(this, canRun()));
				
			}
		});
		
		autoRadio.setSelection(true);
		
		folder.setSelection(0);
	    folder.layout(true);
	}
	
	private void showCalibrantAndBeamCentre(boolean show) {
		if (augmenter == null) return;
		if (manager.isEmpty()) {
			augmenter.deactivate(false);
			return;
		}
		if (show) {
			augmenter.activate();
			augmenter.drawBeamCentre(true);
			CalibrationStandards standards = CalibrationFactory.getCalibrationStandards();
			augmenter.drawCalibrantRings(true, standards.getCalibrant());
		} else {
			augmenter.deactivate(false);
		}
	}
	
	public void update() {
		if (cpw != null) cpw.setDiffractionMeataData(manager.getCurrentData() != null ? manager.getCurrentData().getMetaData() : null);
		poiFindingRun.updateData(manager.getCurrentData());
		showCalibrantAndBeamCentre(showRings);
	}
	
	public void setShowSteering(boolean showSteering) {
		this.showSteering = showSteering;
	}
	
	private void fireListeners(StateChangedEvent event) {
		for (ICalibrationStateListener l : listeners) l.calibrationStateChanged(event);
	}
	
	public void addCalibrationStateListener(ICalibrationStateListener listener) {
		listeners.add(listener);
	}

	public void removeCalibrationStateListener(ICalibrationStateListener listener) {
		listeners.remove(listener);
	}
	
	private boolean canRun() {
		
		if (model.isAutomaticCalibration()) return true;
		
		if (manager.getCurrentData().getNonNullROISize() > 0) return true;
		
		return false;
	}
	
	public boolean isAutomatic() {
		return model.isAutomaticCalibration();
	}
	
	public void enableOptions(boolean enable){
		usePoints.setEnabled(enable);
		options.enable(enable);
	}

}
