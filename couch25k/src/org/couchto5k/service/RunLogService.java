package org.couchto5k.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.couchto5k.data.Run;
import org.couchto5k.data.TrackPoint;
import org.couchto5k.data.repository.TrackPointRepository;
import org.ektorp.CouchDbConnector;
import org.ektorp.ReplicationCommand;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import com.couchbase.touchdb.TDDatabase;
import com.couchbase.touchdb.TDServer;
import com.couchbase.touchdb.ektorp.TouchDBHttpClient;
import com.couchbase.touchdb.router.TDURLStreamHandlerFactory;

public class RunLogService extends Service {

	{
		TDURLStreamHandlerFactory.registerSelfIgnoreError();
	}

//	private static final String COUCH25K_REMOTE_DB = "http://peterfriese.iriscouch.com:5984/couch25k";
	private static final String COUCH25K_REMOTE_DB = "http://stefanreichert.iriscouch.com:5984/couch25k";
	private static final String COUCH25K_DB = "couch25k";
	private static final String TAG = "RunLogService";

	private final ServiceBinder binder = new ServiceBinder();

	private final Map<Run, Observer> observerMap;
	private final Map<Run, Tracer> tracerMap;
	private final Map<String, Run> runMap = new ConcurrentHashMap<String, Run>();

	private TrackPointRepository trackPointRepository;
	private StdCouchDbInstance couchDBInstance;

	public RunLogService() {
		observerMap = new ConcurrentHashMap<Run, RunLogService.Observer>();
		tracerMap = new ConcurrentHashMap<Run, RunLogService.Tracer>();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		TDServer touchDBServer = null;
		String filesDir = getFilesDir().getAbsolutePath();
		try {
			touchDBServer = new TDServer(filesDir);
			TDDatabase touchDBInstance = touchDBServer.getDatabaseNamed(
					COUCH25K_DB, true);

			HttpClient httpClient = new TouchDBHttpClient(touchDBServer);
			couchDBInstance = new StdCouchDbInstance(httpClient);

			CouchDbConnector couchDBConnector = couchDBInstance
					.createConnector(COUCH25K_DB, false);
			trackPointRepository = new TrackPointRepository(couchDBConnector);
			trackPointRepository.createViews(touchDBInstance);

			initializeReplication();
		} catch (IOException e) {
			Log.e(TAG, "Error starting TDServer", e);
		}
	}

	@Override
	public void onDestroy() {
		stopReplication();
		super.onDestroy();
	}

	private void initializeReplication() {
		ReplicationCommand replicationCommandPull = new ReplicationCommand.Builder()
				.source(COUCH25K_REMOTE_DB).target(COUCH25K_DB)
				.continuous(true).build();
		try {
			couchDBInstance.replicate(replicationCommandPull);
		} catch (Exception exception) {
			Log.e(TAG, exception.getMessage(), exception);
		}
		ReplicationCommand replicationCommandPush = new ReplicationCommand.Builder()
				.source(COUCH25K_DB).target(COUCH25K_REMOTE_DB)
				.continuous(true).build();
		try {
			couchDBInstance.replicate(replicationCommandPush);
		} catch (Exception exception) {
			Log.e(TAG, exception.getMessage(), exception);
		}
	}

	private void stopReplication() {
		ReplicationCommand replicationCommandStopPull = new ReplicationCommand.Builder()
				.source(COUCH25K_REMOTE_DB).target(COUCH25K_DB).cancel(true)
				.build();
		try {
			couchDBInstance.replicate(replicationCommandStopPull);
		} catch (Exception exception) {
			Log.e(TAG, exception.getMessage(), exception);
		}
		ReplicationCommand replicationCommandStopPush = new ReplicationCommand.Builder()
				.source(COUCH25K_DB).target(COUCH25K_REMOTE_DB).cancel(true)
				.build();
		try {
			couchDBInstance.replicate(replicationCommandStopPush);
		} catch (Exception exception) {
			Log.e(TAG, exception.getMessage(), exception);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private Run _addRun(String title, String user) {
		String id = title + "#" + user;
		if (!runMap.containsKey(id)) {
			Run run = new Run();
			run.setId(id);
			run.setTitle(title);
			run.setUser(user);
			runMap.put(id, run);
		}
		return runMap.get(id);
	}

	private void _saveTrackPoint(TrackPoint trackPoint) {
		trackPointRepository.add(trackPoint);
	}

	private Run _loadRun(String id) {
		return runMap.get(id);
	}

	private void _observeRun(Run run) {
		if (!observerMap.containsKey(run)) {
			Observer observer = new Observer(run);
			observerMap.put(run, observer);
			observer.active = true;
			observer.start();
		}
	}

	private void _stopObservingRun(Run run) {
		if (observerMap.containsKey(run)) {
			Observer observer = observerMap.remove(run);
			observer.active = false;
		}
	}

	private void _traceRun(Run run) {
		if (!tracerMap.containsKey(run)) {
			Tracer tracer = new Tracer(run);
			tracerMap.put(run, tracer);
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 60000, 30, tracer);
			} else {
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 60000, 30, tracer);
			}
		}
	}

	private void _stopTracingRun(Run run) {
		if (tracerMap.containsKey(run)) {
			Tracer tracer = tracerMap.remove(run);
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			locationManager.removeUpdates(tracer);
		}
	}

	private boolean _isRunTraced(Run run) {
		return tracerMap.containsKey(run);
	}

	private Set<TrackPoint> _loadTrackPoints(Run run) {
		return new HashSet<TrackPoint>(trackPointRepository.findByRun(run));
	}

	private Collection<Run> _getRuns() {
		List<String[]> runIds = trackPointRepository.findRuns();
		for (String[] runId : runIds) {
			_addRun(runId[0], runId[1]);
		}
		return runMap.values();
	}

	private class ServiceBinder extends Binder implements IRunLogService {

		@Override
		public void saveTrackPoint(final TrackPoint trackPoint) {
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					_saveTrackPoint(trackPoint);
				}
			};
			new Thread(runnable, "run log worker").start();
		}

		public Set<TrackPoint> loadTrackPoints(Run run) {
			return _loadTrackPoints(run);
		}

		@Override
		public Run loadRun(String id) {
			return _loadRun(id);
		}

		@Override
		public Collection<Run> getRuns() {
			return _getRuns();
		}

		@Override
		public void observerRun(Run run) {
			_observeRun(run);
		}

		@Override
		public void traceRun(Run run) {
			_traceRun(run);
		}

		@Override
		public void stopObservingRun(Run run) {
			_stopObservingRun(run);
		}

		@Override
		public void stopTracingRun(Run run) {
			_stopTracingRun(run);
		}

		@Override
		public boolean isRunTraced(Run run) {
			return _isRunTraced(run);
		}

		@Override
		public Run addRun(String title, String user) {
			return _addRun(title, user);
		}
	}

	private class Observer extends Thread {

		private boolean active = false;
		private Run run;

		public Observer(Run run) {
			super("run observer worker");
			this.run = run;
		}

		@Override
		public void run() {
			while (active) {
				Log.i(TAG, "refresh trackpoints for run " + run.getTitle());
				Set<TrackPoint> trackPoints = _loadTrackPoints(run);
				run.setTrackPoints(trackPoints);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException exception) {
					exception.printStackTrace();
				}
			}
		}
	};

	private class Tracer implements LocationListener {
		private Run run;

		public Tracer(Run run) {
			this.run = run;
		}

		@Override
		public void onLocationChanged(Location location) {
			TrackPoint trackPoint = new TrackPoint();
			trackPoint.setId(UUID.randomUUID().toString());
			trackPoint.setRun(run.getTitle());
			trackPoint.setUser(run.getUser());
			trackPoint.setLat(location.getLatitude());
			trackPoint.setLon(location.getLongitude());
			java.text.DateFormat dateFormat = DateFormat
					.getDateFormat(RunLogService.this);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			trackPoint.setTime(new Date());
			_saveTrackPoint(trackPoint);
		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}
	}

}
