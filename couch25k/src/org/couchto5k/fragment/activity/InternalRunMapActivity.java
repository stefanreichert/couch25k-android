package org.couchto5k.fragment.activity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.couchto5k.R;
import org.couchto5k.data.Run;
import org.couchto5k.data.TrackPoint;
import org.couchto5k.map.RunOverlay;
import org.couchto5k.map.TrackPointOverlayItem;
import org.couchto5k.service.RunLogService;
import org.couchto5k.service.RunLogServiceConnection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class InternalRunMapActivity extends MapActivity {

	private RunOverlay runOverlay;
	private Run run;
	private MapView mapView;

	private RunLogServiceConnection serviceConnection = new RunLogServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			run.removeListener(propertyChangeListener);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			// get the runs from the database
			if (getIntent().hasExtra(Run.ID_PROPERTY)) {
				run = getRunLogService().loadRun(
						getIntent().getStringExtra(Run.ID_PROPERTY));
				updateUI();
				run.addListener(propertyChangeListener);
			} else {
				// close this activity as the item details cannot be retrieved
				setResult(Activity.RESULT_CANCELED);
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

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.runmap);
		mapView = (MapView) findViewById(R.id.mapview);
		Drawable trackPointDefault = getResources().getDrawable(
				R.drawable.trackpoint);
		Drawable trackPointStart = getResources().getDrawable(
				R.drawable.trackpoint_start);
		Drawable trackPointFinish = getResources().getDrawable(
				R.drawable.trackpoint_finish);
		runOverlay = new RunOverlay(trackPointDefault, trackPointStart,
				trackPointFinish);
		mapView.getOverlays().add(runOverlay);
		mapView.setKeepScreenOn(true);
	};

	private void updateUI() {
		List<TrackPointOverlayItem> overlayItems = new ArrayList<TrackPointOverlayItem>();
		for (TrackPoint trackPoint : run.getTrackPoints()) {
			overlayItems.add(new TrackPointOverlayItem(trackPoint));
		}
		runOverlay.addOverlays(overlayItems);
		mapView.invalidate();
		// center last trackpoint
		if (!overlayItems.isEmpty()) {
			TrackPointOverlayItem item = overlayItems
					.get(overlayItems.size() - 1);
			mapView.getController().animateTo(item.getPoint());
		}
	}

	@Override
	public void onStart() {
		bindService(new Intent(this, RunLogService.class), serviceConnection, 0);
		super.onResume();
	}

	@Override
	public void onStop() {
		unbindService(serviceConnection);
		super.onPause();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
