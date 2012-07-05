package org.couchto5k;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.couchto5k.data.Run;
import org.couchto5k.data.TrackPoint;
import org.couchto5k.map.RunOverlay;
import org.couchto5k.map.TrackPointOverlayItem;
import org.couchto5k.service.IRunLogService;
import org.couchto5k.service.RunLogService;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class RunMapActivity extends MapActivity {

	static final String TAG = "RunMapActivity";

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			run.removeListener(propertyChangeListener);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			runLogService = (IRunLogService) service;
			// get the runs from the database
			if (getIntent().hasExtra(Run.ID_PROPERTY)) {
				run = runLogService.loadRun(getIntent().getStringExtra(
						Run.ID_PROPERTY));
				updateUI();
				run.addListener(propertyChangeListener);
			} else {
				// close this activity as the item details cannot be retrieved
				setResult(RESULT_CANCELED);
				finish();
			}
		}

	};

	private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

		private Handler handler = new Handler() {
			public void handleMessage(Message message) {
				updateUI();
			};
		};

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getSource() instanceof Run) {
				if (Run.TRACK_POINTS_PROPERTY.equals(event.getPropertyName())) {
					handler.sendMessage(Message.obtain(handler, 42, run));
				}
			}
		}
	};

	private RunOverlay runOverlay;
	private IRunLogService runLogService;
	private Run run;
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.runmap);
		mapView = (MapView) findViewById(R.id.run_mapview);
		runOverlay = new RunOverlay(getResources().getDrawable(
				R.drawable.trackpoint));
		mapView.getOverlays().add(runOverlay);
	}

	private void updateUI() {
		List<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
		for (TrackPoint trackPoint : run.getTrackPoints()) {
			overlayItems.add(new TrackPointOverlayItem(trackPoint));
		}
		runOverlay.addOverlays(overlayItems);
		mapView.invalidate();
	}

	@Override
	protected void onResume() {
		bindService(new Intent(this, RunLogService.class), serviceConnection, 0);
		super.onResume();
	}

	@Override
	protected void onPause() {
		unbindService(serviceConnection);
		super.onPause();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
