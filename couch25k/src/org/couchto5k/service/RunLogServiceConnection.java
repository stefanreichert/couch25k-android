package org.couchto5k.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class RunLogServiceConnection implements ServiceConnection {

	/** The service. */
	private IRunLogService runLogService;

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		runLogService = (IRunLogService) service;
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

	}

	public IRunLogService getRunLogService() {
		return runLogService;
	}

}
