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
	private Drawable trackpointDefault;
	private Drawable trackpointStart;
	private Drawable trackpointFinish;

	public RunOverlay(Drawable trackpointDefault, Drawable trackpointStart,
			Drawable trackpointFinish) {
		super(boundCenterBottom(trackpointDefault));
		this.trackpointDefault = trackpointDefault;
		this.trackpointStart = boundCenterBottom(trackpointStart);
		this.trackpointFinish = boundCenterBottom(trackpointFinish);
	}

	public void addOverlays(List<TrackPointOverlayItem> overlayItems) {
		for (TrackPointOverlayItem overlayItem : overlayItems) {
			if (!overlays.contains(overlayItem)) {
				overlays.add(overlayItem);
			}
		}
		populate();
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
			paint.setColor(Color.BLACK);
			Point pointTwo = new Point();
			projection.toPixels(itemTwo.getPoint(), pointTwo);
			paint.setStrokeWidth(5);
			canvas.drawLine(pointOne.x, pointOne.y, pointTwo.x, pointTwo.y,
					paint);
		}
		// draw point
		if (overlays.size() == 1) {
			drawtrackPoint(canvas, mapView, shadow, 0, trackpointDefault);
		}
		// draw start and finish
		if (overlays.size() > 1) {
			drawtrackPoint(canvas, mapView, shadow, 0, trackpointStart);
			drawtrackPoint(canvas, mapView, shadow, overlays.size() - 1,
					trackpointFinish);
		}
	}

	private void drawtrackPoint(Canvas canvas, MapView mapView, boolean shadow,
			int overlayIndex, Drawable drawable) {
		TrackPointOverlayItem item = overlays.get(overlayIndex);
		Point point = new Point();
		Projection projection = mapView.getProjection();
		projection.toPixels(item.getPoint(), point);
		int drawableOffset = (drawable.getBounds().width() / 10) * 4;
		drawAt(canvas, drawable, point.x + drawableOffset, point.y, shadow);
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