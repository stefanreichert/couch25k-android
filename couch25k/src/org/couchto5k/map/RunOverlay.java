package org.couchto5k.map;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class RunOverlay extends ItemizedOverlay<OverlayItem> {

	private List<OverlayItem> overlays = new ArrayList<OverlayItem>();

	public RunOverlay(Drawable drawable) {
		super(boundCenterBottom(drawable));
	}

	public void addOverlays(List<OverlayItem> overlayItems) {
		for (OverlayItem overlayItem : overlayItems) {
			if (!overlays.contains(overlayItem)) {
				overlays.add(overlayItem);
			}
		}
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

}