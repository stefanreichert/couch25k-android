package org.couchto5k.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.couchto5k.R;
import org.couchto5k.RunLogActivity;
import org.couchto5k.adapter.RunListAdapter;
import org.couchto5k.data.Run;
import org.couchto5k.fragment.handler.RunLogFragmentHandler;
import org.couchto5k.fragment.listener.IRunSelectionListener;
import org.couchto5k.service.RunLogService;
import org.couchto5k.service.RunLogServiceConnection;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class RunLogFragment extends Fragment {

	private Collection<Run> runs;
	private Set<IRunSelectionListener> listeners = new HashSet<IRunSelectionListener>();
	private RunLogFragmentHandler handler = new RunLogFragmentHandler(this);
	private RunLogServiceConnection serviceConnection = new RunLogServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			refreshUI();
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.runlog, container, false);
		ListView runLogList = (ListView) view.findViewById(R.id.runLog_list);
		runLogList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView listView = (ListView) parent;
				Run selectedRun = (Run) listView.getItemAtPosition(position);
				fireRunSelected(selectedRun.getId());
			}
		});
		return view;
	};

	@Override
	public void onStart() {
		getActivity().bindService(
				new Intent(getActivity(), RunLogService.class),
				serviceConnection, 0);
		super.onStart();
	}

	@Override
	public void onStop() {
		getActivity().unbindService(serviceConnection);
		super.onStop();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.runlog_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (R.id.runLog_menu_user == item.getItemId()) {
			getActivity().showDialog(RunLogActivity.USER_DIALOG);
			return true;
		}
		if (R.id.runLog_menu_run == item.getItemId()) {
			getActivity().showDialog(RunLogActivity.NEWRUN_DIALOG);
			return true;
		}
		if (R.id.runLog_menu_refresh == item.getItemId()) {
			refreshUI();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void updateWidgets() {
		List<Run> runsToBeAdapted = new ArrayList<Run>(runs);
		// update the list
		ListView runLogList = (ListView) getView().findViewById(
				R.id.runLog_list);
		RunListAdapter runListAdapter = new RunListAdapter(getActivity(),
				runsToBeAdapted);
		runListAdapter.sort(new Comparator<Run>() {
			@Override
			public int compare(Run lhs, Run rhs) {
				return lhs.compareTo(rhs);
			}
		});
		runLogList.setAdapter(runListAdapter);
		TextView runLogListHeader = (TextView) getView().findViewById(
				R.id.runLog_listLabel);
		String label = getResources().getText(R.string.runlog_list_label)
				.toString();
		runLogListHeader.setText(String.format(label, runsToBeAdapted.size()));
	}

	public void addListener(IRunSelectionListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(IRunSelectionListener listener) {
		this.listeners.remove(listener);
	}

	private void refreshUI() {
		Thread loadRunsThread = new Thread("run log worker") {

			@Override
			public void run() {
				runs = serviceConnection.getRunLogService().getRuns();
				handler.sendMessage(Message.obtain());
			}
		};
		loadRunsThread.start();
		getActivity().showDialog(RunLogActivity.LOADING_PROGRESS_DIALOG);
	}

	private void fireRunSelected(String runId) {
		// When clicked, show the details
		for (IRunSelectionListener listener : listeners) {
			listener.handleRunSelected(runId);
		}
	}
}
