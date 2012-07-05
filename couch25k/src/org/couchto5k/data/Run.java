package org.couchto5k.data;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Set;
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

	public void addListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public long getDistance() {
		long distance = 0;
		if (!trackPoints.isEmpty()) {
			TrackPoint predecessor = null;
			for (TrackPoint trackPoint : trackPoints) {
				if (predecessor != null) {
					float[] results = new float[1];
					Location.distanceBetween(predecessor.getLat(),
							predecessor.getLon(), trackPoint.getLat(),
							trackPoint.getLon(), results);
					distance = distance + new Float(results[0]).longValue();
				}
				predecessor = trackPoint;
			}
		}
		return distance;
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
