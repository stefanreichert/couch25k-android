package org.couchto5k.fragment.handler;

import org.couchto5k.RunLogActivity;
import org.couchto5k.fragment.RunLogFragment;

public class RunLogFragmentHandler extends android.os.Handler {

	private final RunLogFragment fragment;

	public RunLogFragmentHandler(RunLogFragment fragment) {
		this.fragment = fragment;
	}

	public void handleMessage(android.os.Message msg) {
		fragment.getActivity().dismissDialog(
				RunLogActivity.LOADING_PROGRESS_DIALOG);
		fragment.updateWidgets();
	};
}