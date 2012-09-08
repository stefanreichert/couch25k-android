package org.couchto5k;

import org.couchto5k.data.Run;
import org.couchto5k.fragment.RunFragment;
import org.couchto5k.fragment.listener.IRunListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class RunActivity extends FragmentActivity implements IRunListener {

	private static final int LOADING_PROGRESS_DIALOG = 42;
	private static final int WAIT_FOR_SIGNAL_PROGRESS_DIALOG = 43;
	private static final int CONFIRM_STOP_TRACING_DIALOG = 44;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.run_fragment);
		RunFragment runFragment = (RunFragment) getSupportFragmentManager()
				.findFragmentById(R.id.run_fragment);
		runFragment.addListener(this);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == LOADING_PROGRESS_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(R.string.progress_title);
			progressDialog.setMessage(getResources().getText(
					R.string.progress_loading));
			progressDialog.setCancelable(false);
			return progressDialog;
		}
		if (id == WAIT_FOR_SIGNAL_PROGRESS_DIALOG) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(R.string.progress_title);
			progressDialog.setMessage(getResources().getText(
					R.string.progress_wait_for_signal));
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					RunFragment runFragment = (RunFragment) getSupportFragmentManager()
							.findFragmentById(R.id.run_fragment);
					runFragment.stopTracing();
					finish();
				}
			});
			return progressDialog;
		}
		if (id == CONFIRM_STOP_TRACING_DIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder
					.setCancelable(false)
					.setTitle(R.string.confirm_stop_tracing_title)
					.setMessage(R.string.confirm_stop_tracing)
					.setIcon(R.drawable.stop)
					.setPositiveButton(R.string.confirm_stop_tracing_yes,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									RunFragment runFragment = (RunFragment) getSupportFragmentManager()
											.findFragmentById(R.id.run_fragment);
									runFragment.stopTracing();
									finish();
								}
							})
					.setNegativeButton(R.string.confirm_stop_tracing_no,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// intentionally do nothing
								}
							}).create();
		}
		return super.onCreateDialog(id);
	}

	@Override
	public void onBackPressed() {
		RunFragment runFragment = (RunFragment) getSupportFragmentManager()
				.findFragmentById(R.id.run_fragment);
		if (runFragment.isTracing()) {
			showDialog(CONFIRM_STOP_TRACING_DIALOG);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void showMap(String runId) {
		Toast.makeText(RunActivity.this, "Show map", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(RunActivity.this, RunMapActivity.class);
		intent.putExtra(Run.ID_PROPERTY, runId);
		startActivityForResult(intent, 42);
	}
}
