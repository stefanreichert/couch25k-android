package org.couchto5k.fragment;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.couchto5k.R;
import org.couchto5k.data.Run;
import org.couchto5k.fragment.listener.IRunListener;
import org.couchto5k.service.RunLogService;
import org.couchto5k.service.RunLogServiceConnection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class RunFragment extends Fragment {
	private static final int LOADING_PROGRESS_DIALOG = 42;
	private static final int WAIT_FOR_SIGNAL_PROGRESS_DIALOG = 43;
	private static final String TAG = "RunActivity";

	private Set<IRunListener> listeners = new HashSet<IRunListener>();
	private Run run;
	private RunLogServiceConnection serviceConnection = new RunLogServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			deactivateUI();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			// get the runs from the database
			if (getActivity().getIntent().hasExtra(Run.ID_PROPERTY)) {
				Thread loadRunsThread = new Thread(new Runnable() {
					private Handler handler = new Handler() {
						public void handleMessage(android.os.Message msg) {
							getActivity()
									.dismissDialog(LOADING_PROGRESS_DIALOG);
							activateUI();
						};
					};

					@Override
					public void run() {
						run = getRunLogService().loadRun(
								getActivity().getIntent().getStringExtra(
										Run.ID_PROPERTY));
						handler.sendMessage(Message.obtain());
					}
				}, "run log worker");
				loadRunsThread.start();
				getActivity().showDialog(LOADING_PROGRESS_DIALOG);
			} else {
				// close this activity as the item details cannot be retrieved
				getActivity().setResult(Activity.RESULT_CANCELED);
				getActivity().finish();
			}
		}

		private void activateUI() {
			// initilize UI
			updateTextTitle(run);
			updateTextDistance(run);
			updateTextAverageSpeed(run);
			updateTextAverageTimePerKm(run);
			updateTextTrackPointCount(run);
			updateChronometerTime(run);
			run.addListener(propertyChangeListener);
			getRunLogService().observerRun(run);
		}

		private void deactivateUI() {
			getRunLogService().stopObservingRun(run);
			run.removeListener(propertyChangeListener);
		}

	};
	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			Run run = (Run) message.obj;
			updateTextTrackPointCount(run);
			updateTextDistance(run);
			updateTextAverageSpeed(run);
			updateTextAverageTimePerKm(run);
			if (!serviceConnection.getRunLogService().isRunTraced(run)) {
				updateChronometerTime(run);
			}
		};
	};

	private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getSource() instanceof Run
					&& Run.TRACK_POINTS_PROPERTY
							.equals(event.getPropertyName())) {
				refreshUI((Run) event.getSource());
			}
		}
	};

	public void refreshUI(Run run) {
		Log.i(TAG, "run " + run.getTitle() + " changed");
		handler.sendMessage(Message.obtain(handler, 42, run));
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.run, container, false);
	};

	@Override
	public void onDestroy() {
		if (serviceConnection.getRunLogService() != null) {
			serviceConnection.getRunLogService().stopObservingRun(run);
			serviceConnection.getRunLogService().stopTracingRun(run);
		}
		super.onDestroy();
	}

	@Override
	public void onStart() {
		getActivity().bindService(
				new Intent(getActivity(), RunLogService.class),
				serviceConnection, 0);
		super.onResume();
	}

	@Override
	public void onStop() {
		getActivity().unbindService(serviceConnection);
		super.onPause();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.run_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (R.id.run_menu_start == item.getItemId()) {
			final Handler handler = new Handler() {
				public void handleMessage(android.os.Message msg) {
					getActivity()
							.dismissDialog(WAIT_FOR_SIGNAL_PROGRESS_DIALOG);
					if (!run.getTrackPoints().isEmpty()) {
						startTracing();
					}
				};
			};
			Thread checkTrackerThread = new Thread("run signal worker") {
				@Override
				public void run() {
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
			serviceConnection.getRunLogService().traceRun(run);
			checkTrackerThread.start();
			getActivity().showDialog(WAIT_FOR_SIGNAL_PROGRESS_DIALOG);
			return true;
		}
		if (R.id.run_menu_stop == item.getItemId()) {
			stopTracing();
			return true;
		}
		if (R.id.run_menu_runmap == item.getItemId()) {
			showMap();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	public void addListener(IRunListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(IRunListener listener) {
		this.listeners.remove(listener);
	}

	private void showMap() {
		for (IRunListener listener : listeners) {
			listener.showMap(run.getId());
		}
	}

	public boolean isTracing() {
		return serviceConnection.getRunLogService() != null
				&& serviceConnection.getRunLogService().isRunTraced(run);
	}

	public void stopTracing() {
		Toast.makeText(getActivity(), "Tracking stopped", Toast.LENGTH_LONG)
				.show();
		Chronometer chronometerTime = (Chronometer) getView().findViewById(
				R.id.run_chronometerTime);
		chronometerTime.stop();
		serviceConnection.getRunLogService().stopTracingRun(run);
	}

	private void startTracing() {
		Toast.makeText(getActivity(), "Tracking started", Toast.LENGTH_LONG)
				.show();
		Chronometer chronometerTime = (Chronometer) getView().findViewById(
				R.id.run_chronometerTime);
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
		TextView textTitle = (TextView) getView().findViewById(
				R.id.run_textTitle);
		textTitle.setText(run.getTitle());
	}

	private void updateTextTrackPointCount(Run run) {
		TextView textTrackpointCount = (TextView) getView().findViewById(
				R.id.run_textTrackpointCount);
		textTrackpointCount.setText(Integer.toString(run.getTrackPoints()
				.size()));
	}

	private void updateChronometerTime(Run run) {
		Chronometer chronometerTime = (Chronometer) getView().findViewById(
				R.id.run_chronometerTime);
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
		TextView textDistance = (TextView) getView().findViewById(
				R.id.run_textDistance);
		textDistance.setText(DecimalFormat.getNumberInstance().format(
				run.getDistance())
				+ " meters");
	}

	private void updateTextAverageSpeed(Run run) {
		TextView textAverageSpeed = (TextView) getView().findViewById(
				R.id.run_textAverageSpeed);
		NumberFormat numberFormat = DecimalFormat.getNumberInstance();
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setMinimumFractionDigits(2);
		textAverageSpeed.setText(numberFormat.format(run.getAverageSpeed()
				.doubleValue()) + " km/h");
	}

	private void updateTextAverageTimePerKm(Run run) {
		TextView textAverageTimePerKm = (TextView) getView().findViewById(
				R.id.run_textAverageTimePerKm);
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateText = dateFormat.format(new Date(run
				.getAverageTimePerKilometer()));
		if (dateText.startsWith("00:")) {
			dateFormat = new SimpleDateFormat("mm:ss");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateText = dateFormat.format(new Date(run
					.getAverageTimePerKilometer()));
		}
		textAverageTimePerKm.setText(dateText);
	}
}
