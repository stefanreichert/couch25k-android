package org.couchto5k;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.couchto5k.data.Run;
import org.couchto5k.fragment.RunFragment;
import org.couchto5k.fragment.RunLogFragment;
import org.couchto5k.fragment.RunMapFragment;
import org.couchto5k.fragment.listener.IRunSelectionListener;
import org.couchto5k.service.RunLogService;
import org.couchto5k.service.RunLogServiceConnection;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RunLogActivity extends FragmentActivity implements
		IRunSelectionListener {

	public static final int USER_DIALOG = 42;
	public static final int NEWRUN_DIALOG = 43;
	public static final int LOADING_PROGRESS_DIALOG = 44;
	public static final String USERNAME_PREFERENCE = "couch25k.user";

	private RunLogServiceConnection serviceConnection = new RunLogServiceConnection();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.runlog_fragment);
		RunLogFragment runLogFragment = (RunLogFragment) getSupportFragmentManager()
				.findFragmentById(R.id.runlog_fragment);
		runLogFragment.addListener(this);
		startService(new Intent(this, RunLogService.class));
	}

	@Override
	public void onDestroy() {
		stopService(new Intent(this, RunLogService.class));
		super.onDestroy();
	}

	@Override
	public void onStart() {
		bindService(new Intent(this, RunLogService.class), serviceConnection, 0);
		super.onStart();
	}

	@Override
	public void onStop() {
		unbindService(serviceConnection);
		super.onStop();
	}

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

	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == USER_DIALOG) {
			EditText textName = (EditText) dialog
					.findViewById(R.id.user_textName);
			textName.setText(getUserName());
			textName.setSelection(textName.getText().length());
		} else if (id == NEWRUN_DIALOG) {
			EditText textTitle = (EditText) dialog
					.findViewById(R.id.newrun_textTitle);
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yy/dd/MM-HH:mm:ss");
			textTitle.setText(getUserName() + "-"
					+ dateFormat.format(new Date()));
			textTitle.setSelection(textTitle.getText().length());
		} else {
			super.onPrepareDialog(id, dialog);
		}
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
				Run run = serviceConnection.getRunLogService().addRun(
						textTitle.getText().toString(), getUserName());
				dismissDialog(NEWRUN_DIALOG);
				handleRunSelected(run.getId());
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

	private void setUserName(String name) {
		SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(USERNAME_PREFERENCE, name);
		editor.commit();
	}

	private String getUserName() {
		SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		return preferences.getString(USERNAME_PREFERENCE, "anonymous");
	}

	@Override
	public void handleRunSelected(String runId) {
		// check run map
		RunMapFragment runMapFragment = (RunMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.runmap_fragment);
		if (runMapFragment != null && runMapFragment.isInLayout()) {
			// TODO update map fragment manually
		}
		// check run details
		RunFragment runFragment = (RunFragment) getSupportFragmentManager()
				.findFragmentById(R.id.run_fragment);
		if (runFragment != null && runFragment.isInLayout()) {
			runFragment.refreshUI(serviceConnection.getRunLogService().loadRun(
					runId));
		} else {
			Intent intent = new Intent(this, RunActivity.class);
			if (runId != null) {
				intent.putExtra(Run.ID_PROPERTY, runId);
			}
			startActivityForResult(intent, 42);
		}
	}
}
