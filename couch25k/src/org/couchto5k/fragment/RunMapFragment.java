package org.couchto5k.fragment;

import org.couchto5k.data.Run;
import org.couchto5k.fragment.activity.InternalRunMapActivity;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class RunMapFragment extends Fragment {

	private static final String KEY_STATE_BUNDLE = "localActivityManagerState";

	private LocalActivityManager mLocalActivityManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle state = null;
		if (savedInstanceState != null) {
			state = savedInstanceState.getBundle(KEY_STATE_BUNDLE);
		}

		mLocalActivityManager = new LocalActivityManager(getActivity(), true);
		mLocalActivityManager.dispatchCreate(state);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// This is where you specify you activity class
		Intent intent = new Intent(getActivity(), InternalRunMapActivity.class);
		intent.putExtra(Run.ID_PROPERTY, getActivity().getIntent().getExtras()
				.getString(Run.ID_PROPERTY));
		Window window = mLocalActivityManager.startActivity("tag", intent);
		View currentView = window.getDecorView();
		currentView.setVisibility(View.VISIBLE);
		currentView.setFocusableInTouchMode(true);
		((ViewGroup) currentView)
				.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		return currentView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle(KEY_STATE_BUNDLE,
				mLocalActivityManager.saveInstanceState());
	}

	@Override
	public void onResume() {
		super.onResume();
		mLocalActivityManager.dispatchResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mLocalActivityManager.dispatchPause(getActivity().isFinishing());
	}

	@Override
	public void onStop() {
		super.onStop();
		mLocalActivityManager.dispatchStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocalActivityManager.dispatchDestroy(getActivity().isFinishing());
	}
}
