package org.couchto5k.adapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.couchto5k.data.Run;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TwoLineListItem;

public class RunListAdapter extends ArrayAdapter<Run> {

	private List<Run> runs;

	public RunListAdapter(Context context, List<Run> runs) {
		super(context, android.R.layout.simple_list_item_2, runs);
		this.runs = runs;
	}

	@Override
	public void sort(Comparator<? super Run> comparator) {
		super.sort(comparator);
		Collections.sort(runs, comparator);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TwoLineListItem row;
		Run run = runs.get(position);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = (TwoLineListItem) inflater.inflate(
					android.R.layout.simple_list_item_2, null);
		} else {
			row = (TwoLineListItem) convertView;
		}
		row.getText1().setText(run.getTitle());
		row.getText2().setText(run.getUser());
		return row;
	}

}
