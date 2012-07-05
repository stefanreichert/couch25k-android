package org.couchto5k.service;

import java.util.Collection;
import java.util.Set;

import org.couchto5k.data.Run;
import org.couchto5k.data.TrackPoint;

public interface IRunLogService {

	Collection<Run> getRuns();

	Run loadRun(String runId);

	Run addRun(String title, String user);

	void saveTrackPoint(TrackPoint trackPoint);

	Set<TrackPoint> loadTrackPoints(Run run);

	void observerRun(Run run);

	void traceRun(Run run);

	void stopObservingRun(Run run);

	void stopTracingRun(Run run);

	boolean isRunTraced(Run run);
}
