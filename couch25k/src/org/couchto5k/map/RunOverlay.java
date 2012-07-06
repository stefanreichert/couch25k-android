package org.couchto5k.map;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

public class RunOverlay extends ItemizedOverlay<TrackPointOverlayItem> {

	private List<TrackPointOverlayItem> overlays = new ArrayList<TrackPointOverlayItem>();

	public RunOverlay(Drawable drawable) {
		super(boundCenterBottom(drawable));

	}

	public void addOverlays(List<TrackPointOverlayItem> overlayItems) {
		for (TrackPointOverlayItem overlayItem : overlayItems) {
			if (!overlays.contains(overlayItem)) {
				overlays.add(overlayItem);
			}
		}
		populate();
		if (!overlays.isEmpty()) {
			setFocus(overlays.get(overlays.size() - 1));
		}
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		for (int index = 1; index < overlays.size(); index++) {
			TrackPointOverlayItem itemOne = overlays.get(index - 1);
			TrackPointOverlayItem itemTwo = overlays.get(index);
			Projection projection = mapView.getProjection();
			Paint paint = new Paint();
			Point pointOne = new Point();
			projection.toPixels(itemOne.getPoint(), pointOne);
			paint.setColor(Color.BLUE);
			Point pointTwo = new Point();
			projection.toPixels(itemTwo.getPoint(), pointTwo);
			paint.setStrokeWidth(5);
			canvas.drawLine(pointOne.x, pointOne.y, pointTwo.x, pointTwo.y,
					paint);
		}
	}

	@Override
	protected TrackPointOverlayItem createItem(int i) {
		return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

}