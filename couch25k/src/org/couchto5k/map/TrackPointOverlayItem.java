package org.couchto5k.map;

import org.couchto5k.data.TrackPoint;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class TrackPointOverlayItem extends OverlayItem {

	private final TrackPoint trackPoint;

	public TrackPointOverlayItem(TrackPoint trackPoint) {
		super(new GeoPoint((int) (trackPoint.getLat() * 1E6),
				(int) (trackPoint.getLon() * 1E6)), "Trackpoint", "Trackpoint");
		this.trackPoint = trackPoint;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((trackPoint == null) ? 0 : trackPoint.hashCode());
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
		TrackPointOverlayItem other = (TrackPointOverlayItem) obj;
		if (trackPoint == null) {
			if (other.trackPoint != null)
				return false;
		} else if (!trackPoint.equals(other.trackPoint))
			return false;
		return true;
	}

}