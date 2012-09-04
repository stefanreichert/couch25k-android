package org.couchto5k.data.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.couchto5k.data.Run;
import org.couchto5k.data.TrackPoint;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.CouchDbRepositorySupport;

import com.couchbase.touchdb.TDDatabase;
import com.couchbase.touchdb.TDView;
import com.couchbase.touchdb.TDViewMapBlock;
import com.couchbase.touchdb.TDViewMapEmitBlock;

public class TrackPointRepository extends CouchDbRepositorySupport<TrackPoint> {

	public static final String VIEW_TRACKPOINT_BY_RUN = "trackpoint_by_run";

	public TrackPointRepository(CouchDbConnector databaseConnector) {
		super(TrackPoint.class, databaseConnector);
		initStandardDesignDocument();
	}

	public void createViews(TDDatabase database) {
		createTrackPointsByRunView(database);
	}

	private void createTrackPointsByRunView(TDDatabase database) {
		TDView view = database.getViewNamed(String.format("%s/%s",
				TrackPoint.class.getSimpleName(), VIEW_TRACKPOINT_BY_RUN));
		view.setMapReduceBlocks(new TDViewMapBlock() {

			@Override
			public void map(Map<String, Object> document,
					TDViewMapEmitBlock emitter) {
				if (document.containsKey("run") && document.containsKey("user")) {
					String runId = document.get("run") + "#"
							+ document.get("user");
					emitter.emit(runId, document.get("_id"));
				}
			}
		}, null, "1.0");
	}

	public List<TrackPoint> findByRun(Run run) {
		String runId = run.getTitle() + "#" + run.getUser();
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/TrackPoint")
				.viewName(VIEW_TRACKPOINT_BY_RUN).key(runId);
		ViewResult result = db.queryView(viewQuery);
		List<TrackPoint> trackPoints = new ArrayList<TrackPoint>();
		for (Row row : result.getRows()) {
			TrackPoint trackPoint = get(row.getValue());
			trackPoints.add(trackPoint);
		}
		return trackPoints;
	}

	public List<String[]> findRuns() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/TrackPoint")
				.viewName(VIEW_TRACKPOINT_BY_RUN).group(true);
		ViewResult result = db.queryView(viewQuery);
		List<String[]> runIds = new ArrayList<String[]>();
		for (Row row : result.getRows()) {
			String[] runId = row.getKey().split("#");
			runIds.add(runId);
		}
		return runIds;
	}
}
