package org.couchto5k;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.couchto5k.adapter.RunListAdapter;
import org.couchto5k.data.Run;
import org.couchto5k.service.IRunLogService;
import org.couchto5k.service.RunLogService;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RunLogActivity extends Activity {

	private static final int USER_DIALOG = 42;
	private static final int NEWRUN_DIALOG = 43;
	private static final int LOADING_PROGRESS_DIALOG = 44;
	private static final String USERNAME_PREFERENCE = "couch25k.user";

	private Collection<Run> runs;
	private IRunLogService runLogService;

	final Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			dismissDialog(LOADING_PROGRESS_DIALOG);
			updateWidgets();
		};
	};
	
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// intentionally do nothing
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			runLogService = (IRunLogService) service;
			Thread loadRunsThread = new Thread("run log worker") {

				@Override
				public void run() {
					runs = runLogService.getRuns();
					handler.sendMessage(Message.obtain());
				}
			};
			loadRunsThread.start();
			showDialog(LOADING_PROGRESS_DIALOG);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.runlog);
		ListView runLogList = (ListView) findViewById(R.id.runLog_list);
		runLogList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView listView = (ListView) parent;
				Run selectedRun = (Run) listView.getItemAtPosition(position);
				launchRunActivity(selectedRun.getId());
			}
		});
		startService(new Intent(this, RunLogService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.runlog_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (R.id.runLog_menu_user == item.getItemId()) {
			showDialog(USER_DIALOG);
			return true;
		}
		if (R.id.runLog_menu_run == item.getItemId()) {
			showDialog(NEWRUN_DIALOG);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onDestroy() {
		stopService(new Intent(this, RunLogService.class));
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		bindService(new Intent(this, RunLogService.class), serviceConnection, 0);
		super.onStart();
	}

	@Override
	protected void onStop() {
		unbindService(serviceConnection);
		super.onStop();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == USER_DIALOG) {
			return createUserNameDialog();
		}
		if (id == NEWRUN_DIALOG) {
			return createNewRunDialog();
		}
		if (id == LOADING_PROGRESS_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(R.string.progress_title);
			progressDialog.setMessage(getResources().getText(
					R.string.progress_loading));
			progressDialog.setCancelable(false);
			return progressDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == USER_DIALOG) {
			TextView textName = (TextView) dialog
					.findViewById(R.id.user_textName);
			textName.setText(getUserName());
		} else if (id == NEWRUN_DIALOG) {
			TextView textTitle = (TextView) dialog
					.findViewById(R.id.newrun_textTitle);
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yy/dd/MM-HH:mm:ss");
			textTitle.setText(getUserName() + "-"
					+ dateFormat.format(new Date()));
		} else {
			super.onPrepareDialog(id, dialog);
		}
	}

	private void updateWidgets() {
		List<Run> runsToBeAdapted = new ArrayList<Run>(runs);
		// update the list
		ListView runLogList = (ListView) findViewById(R.id.runLog_list);
		RunListAdapter runListAdapter = new RunListAdapter(RunLogActivity.this,
				runsToBeAdapted);
		runListAdapter.sort(new Comparator<Run>() {
			@Override
			public int compare(Run lhs, Run rhs) {
				return lhs.compareTo(rhs);
			}
		});
		runLogList.setAdapter(runListAdapter);
		TextView runLogListHeader = (TextView) findViewById(R.id.runLog_listLabel);
		String label = getResources().getText(R.string.runlog_list_label)
				.toString();
		runLogListHeader.setText(String.format(label, runsToBeAdapted.size()));
	}

	private String getUserName() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		return preferences.getString(USERNAME_PREFERENCE, "anonymous");
	}

	private void setUserName(String name) {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(USERNAME_PREFERENCE, name);
		editor.commit();
	}

	private void launchRunActivity(String id) {
		// When clicked, show the details
		Intent intent = new Intent(this, RunActivity.class);
		if (id != null) {
			intent.putExtra(Run.ID_PROPERTY, id);
		}
		startActivityForResult(intent, 42);
	}

	private Dialog createNewRunDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.dialog_newrun);
		dialog.setTitle(R.string.newrun_title);
		Button buttonProceed = (Button) dialog
				.findViewById(R.id.newrun_buttonProceed);
		buttonProceed.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				TextView textTitle = (TextView) dialog
						.findViewById(R.id.newrun_textTitle);
				Run run = runLogService.addRun(textTitle.getText().toString(),
						getUserName());
				dismissDialog(NEWRUN_DIALOG);
				launchRunActivity(run.getId());
			}
		});
		return dialog;
	}

	private Dialog createUserNameDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.dialog_user);
		dialog.setTitle(R.string.user_title);
		Button buttonProceed = (Button) dialog
				.findViewById(R.id.user_buttonProceed);
		buttonProceed.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				TextView textUser = (TextView) dialog
						.findViewById(R.id.user_textName);
				setUserName(textUser.getText().toString());
				dismissDialog(USER_DIALOG);
			}
		});
		return dialog;
	}

}