package org.couchto5k;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.couchto5k.data.Run;
import org.couchto5k.service.IRunLogService;
import org.couchto5k.service.RunLogService;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class RunActivity extends Activity {

	private static final int LOADING_PROGRESS_DIALOG = 42;
	private static final int WAIT_FOR_SIGNAL_PROGRESS_DIALOG = 43;
	private static final String TAG = "RunActivity";
	private IRunLogService runLogService;
	private Run run;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			deactivateUI();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			runLogService = (IRunLogService) service;
			// get the runs from the database
			if (getIntent().hasExtra(Run.ID_PROPERTY)) {
				Thread loadRunsThread = new Thread(new Runnable() {
					private Handler handler = new Handler() {
						public void handleMessage(android.os.Message msg) {
							dismissDialog(LOADING_PROGRESS_DIALOG);
							activateUI();
						};
					};

					@Override
					public void run() {
						run = runLogService.loadRun(getIntent().getStringExtra(
								Run.ID_PROPERTY));
						handler.sendMessage(Message.obtain());
					}
				}, "run log worker");
				loadRunsThread.start();
				showDialog(LOADING_PROGRESS_DIALOG);
			} else {
				// close this activity as the item details cannot be retrieved
				setResult(RESULT_CANCELED);
				finish();
			}
		}

		private void activateUI() {
			// initilize UI
			updateTextTitle(run);
			updateTextDistance(run);
			updateTextTrackPointCount(run);
			updateChronometerTime(run);
			run.addListener(propertyChangeListener);
			runLogService.observerRun(run);
		}

		private void deactivateUI() {
			runLogService.stopObservingRun(run);
			run.removeListener(propertyChangeListener);
		}
	};

	private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

		private Handler handler = new Handler() {
			public void handleMessage(Message message) {
				Run run = (Run) message.obj;
				updateTextTrackPointCount(run);
				updateTextDistance(run);
				if (!runLogService.isRunTraced(run)) {
					updateChronometerTime(run);
				}
			};
		};

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getSource() instanceof Run) {
				Run run = (Run) event.getSource();
				Log.i(TAG, "run " + run.getTitle() + " changed");
				if (Run.TRACK_POINTS_PROPERTY.equals(event.getPropertyName())) {
					handler.sendMessage(Message.obtain(handler, 42, run));
				}
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.run);
	}

	@Override
	protected void onDestroy() {
		if (runLogService != null) {
			runLogService.stopObservingRun(run);
			runLogService.stopTracingRun(run);
		}
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		bindService(new Intent(this, RunLogService.class), serviceConnection, 0);
		super.onResume();
	}

	@Override
	protected void onStop() {
		unbindService(serviceConnection);
		super.onPause();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == LOADING_PROGRESS_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(R.string.progress_title);
			progressDialog.setMessage(getResources().getText(
					R.string.progress_loading));
			progressDialog.setCancelable(false);
			return progressDialog;
		}
		if (id == WAIT_FOR_SIGNAL_PROGRESS_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(R.string.progress_title);
			progressDialog.setMessage(getResources().getText(
					R.string.progress_wait_for_signal));
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					runLogService.stopTracingRun(run);
				}
			});
			return progressDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.run_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (R.id.run_menu_start == item.getItemId()) {
			final Handler handler = new Handler() {
				public void handleMessage(android.os.Message msg) {
					dismissDialog(WAIT_FOR_SIGNAL_PROGRESS_DIALOG);
					if (!run.getTrackPoints().isEmpty()) {
						startTracking();
					}
				};
			};
			Thread checkTrackerThread = new Thread("run signal worker") {
				@Override
				public void run() {
					Looper.prepare();
					runLogService.traceRun(run);
					while (run.getTrackPoints().isEmpty()) {
						try {
							sleep(500);
						} catch (InterruptedException exception) {
							exception.printStackTrace();
						}
					}
					handler.sendMessage(Message.obtain());
				}
			};
			checkTrackerThread.start();
			showDialog(WAIT_FOR_SIGNAL_PROGRESS_DIALOG);
			return true;
		}
		if (R.id.run_menu_stop == item.getItemId()) {
			stopTracking();
			return true;
		}
		if (R.id.run_menu_runmap == item.getItemId()) {
			showMap();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void showMap() {
		Toast.makeText(RunActivity.this, "Show map", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(RunActivity.this, RunMapActivity.class);
		if (run != null) {
			intent.putExtra(Run.ID_PROPERTY, run.getId());
		}
		startActivityForResult(intent, 42);
	}

	private void stopTracking() {
		Toast.makeText(RunActivity.this, "Tracking stopped", Toast.LENGTH_LONG)
				.show();
		Chronometer chronometerTime = (Chronometer) findViewById(R.id.run_chronometerTime);
		chronometerTime.stop();
		runLogService.stopTracingRun(run);
	}

	private void startTracking() {
		Toast.makeText(RunActivity.this, "Tracking started", Toast.LENGTH_LONG)
				.show();
		Chronometer chronometerTime = (Chronometer) findViewById(R.id.run_chronometerTime);
		int stoppedMilliseconds = 0;

		String chronoText = chronometerTime.getText().toString();
		String array[] = chronoText.split(":");
		if (array.length == 2) {
			stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 1000
					+ Integer.parseInt(array[1]) * 1000;
		} else if (array.length == 3) {
			stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60 * 1000
					+ Integer.parseInt(array[1]) * 60 * 1000
					+ Integer.parseInt(array[2]) * 1000;
		}

		chronometerTime.setBase(SystemClock.elapsedRealtime()
				- stoppedMilliseconds);
		chronometerTime.start();
	}

	private void updateTextTitle(Run run) {
		TextView textTitle = (TextView) findViewById(R.id.run_textTitle);
		textTitle.setText(run.getTitle());
	}

	private void updateTextTrackPointCount(Run run) {
		TextView textTrackpointCount = (TextView) findViewById(R.id.run_textTrackpointCount);
		textTrackpointCount.setText(Integer.toString(run.getTrackPoints()
				.size()));
	}

	private void updateChronometerTime(Run run) {
		Chronometer chronometerTime = (Chronometer) findViewById(R.id.run_chronometerTime);
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateText = dateFormat.format(new Date(run.getTime()));
		if (dateText.startsWith("00:")) {
			dateFormat = new SimpleDateFormat("mm:ss");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateText = dateFormat.format(new Date(run.getTime()));
		}
		chronometerTime.setText(dateText);
	}

	private void updateTextDistance(Run run) {
		TextView textDistance = (TextView) findViewById(R.id.run_textDistance);
		textDistance.setText(DecimalFormat.getNumberInstance().format(
				run.getDistance())
				+ " meters");
	}

}
