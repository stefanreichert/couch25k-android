package org.couchto5k.data;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.location.Location;

public class Run implements Comparable<Run> {

	public static final String ID_PROPERTY = "id";
	public static final String TITLE_PROPERTY = "title";
	public static final String USER_PROPERTY = "user";
	public static final String TRACK_POINTS_PROPERTY = "trackPoints";

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
			this);

	private String id;
	private String title;
	private String user;
	private TreeSet<TrackPoint> trackPoints = new TreeSet<TrackPoint>();
	private long distance = 0;
	private TrackPoint distanceTrackPoint;

	public void addListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public synchronized long getDistance() {
		// check whether we have to compute the distance again
		if (trackPoints.isEmpty() || trackPoints.last() == null
				|| trackPoints.last().equals(distanceTrackPoint)) {
			// if we do not have trackpoints yet or there are no new
			// trackpoints, return the computed value
			return distance;
		}
		// otherwise check whether processed data is still valid...
		if (distanceTrackPoint != null
				&& !trackPoints.contains(distanceTrackPoint)) {
			// we'll something is seriously wrong here, reset!
			distance = 0;
			distanceTrackPoint = null;
		}
		if (!trackPoints.isEmpty()) {
			SortedSet<TrackPoint> relevantTrackPoints = trackPoints;
			if (distanceTrackPoint != null) {
				// if we have a trackpoint (i.e. a computed distance), we only
				// need to care about elements that were added later
				relevantTrackPoints = trackPoints.tailSet(distanceTrackPoint);
				distanceTrackPoint = null;
			}
			for (TrackPoint trackPoint : relevantTrackPoints) {
				if (distanceTrackPoint != null) {
					float[] results = new float[1];
					Location.distanceBetween(distanceTrackPoint.getLat(),
							distanceTrackPoint.getLon(), trackPoint.getLat(),
							trackPoint.getLon(), results);
					distance = distance + new Float(results[0]).longValue();
				}
				distanceTrackPoint = trackPoint;
			}
		}
		return distance;
	}

	public BigDecimal getAverageSpeed() {
		if (getDistance() > 0 && getTime() > 0) {
			double timeInSeconds = getTime() / 1000;
			double speed = (getDistance() / timeInSeconds) * 3.6;
			return new BigDecimal(speed);
		}
		return new BigDecimal(0);
	}

	public long getAverageTimePerKilometer() {
		if (getTime() > 0 && getDistance() > 0) {
			return (getTime() / getDistance()) * 1000;
		}
		return 0;
	}

	public long getTime() {
		if (trackPoints.isEmpty()) {
			return 0;
		}
		return trackPoints.last().getTime().getTime()
				- trackPoints.first().getTime().getTime();
	}

	public Set<TrackPoint> getTrackPoints() {
		return trackPoints;
	}

	public void setTrackPoints(Set<TrackPoint> trackPoints) {
		Set<TrackPoint> oldTrackPoints = this.trackPoints;
		this.trackPoints = new TreeSet<TrackPoint>(trackPoints);
		propertyChangeSupport.firePropertyChange(TRACK_POINTS_PROPERTY,
				oldTrackPoints, trackPoints);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		String oldTitle = this.title;
		this.title = title;
		propertyChangeSupport.firePropertyChange(TITLE_PROPERTY, oldTitle,
				title);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		String oldId = this.id;
		this.id = id;
		propertyChangeSupport.firePropertyChange(ID_PROPERTY, oldId, id);
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		String oldUser = user;
		this.user = user;
		propertyChangeSupport.firePropertyChange(USER_PROPERTY, oldUser, id);
	}

	@Override
	public int compareTo(Run another) {
		return title.compareTo(another.getTitle());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Run other = (Run) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
