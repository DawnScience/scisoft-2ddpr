package uk.ac.diamond.scisoft.diffraction.powder.rcp.widget;

import java.util.HashSet;
import java.util.Set;

import org.dawnsci.plotting.tools.diffraction.DiffractionImageAugmenter;
import org.eclipse.dawnsci.analysis.api.metadata.IDiffractionMetadata;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

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
		final CalibrantSelectionGroup group = new CalibrantSelectionGroup(left);
		
		group.addDisplaySelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showRings = group.getShowRings();
				showCalibrantAndBeamCentre(group.getShowRings());
			}
		});
		
		final RingSelectionGroup ringSelection = new RingSelectionGroup(left, CalibrationFactory.getCalibrationStandards().getCalibrant().getHKLs().size(), model);
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
		
		final Button autoRadio = new Button(left, SWT.RADIO);
		autoRadio.setText("Automatic");
		autoRadio.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		final Button manualRadio = new Button(left, SWT.RADIO);
		manualRadio.setText("Manual");
		manualRadio.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		manualRadio.setSelection(false);
		
		final Composite autoManStack = new Composite(left, SWT.NONE);
		final StackLayout stackLayout = new StackLayout();
		autoManStack.setLayout(stackLayout);
		final Label auto = new Label(autoManStack,SWT.WRAP);
		auto.setText("Automatic Calibration");
		
		
		final Composite manualComposite = new Composite(autoManStack, SWT.None);
		manualComposite.setLayout(new GridLayout());
		IDiffractionMetadata meta = null;
		if (manager.getCurrentData() != null) meta = manager.getCurrentData().getMetaData();
		if (showSteering) {
			cpw = new CalibrantPositioningWidget(manualComposite, meta);
		} else {
			new Label(manualComposite, SWT.WRAP).setText("Align rings to image");
		}
		final Button findRings = new Button(manualComposite, SWT.PUSH);
		findRings.setText("Find Rings");
		findRings.setLayoutData(new GridData());
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
		usePoints = new Button(left, SWT.CHECK);
		usePoints.setText("Point calibration");
		usePoints.setSelection(true);
		
		options = new CalibrationOptionsGroup(left, model, true,true);
		
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
