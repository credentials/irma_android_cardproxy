package org.irmacard.androidcardproxy;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {
	private String TAG = "CardProxyMainActivity";

	private CardProxyConnection mCardProxyConnection;
	private Handler mUpdateHandler;
	
	NsdHelper mNsdHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	    mUpdateHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            String chatLine = msg.getData().getString("msg");
	            Log.i(TAG,chatLine);
	        }
	    };
		mCardProxyConnection = new CardProxyConnection(mUpdateHandler);
		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		broadcast();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mNsdHelper.tearDown();
		mCardProxyConnection.tearDown();
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void broadcast() {
		if (mCardProxyConnection.getLocalPort() > -1) {
			mNsdHelper.registerService(mCardProxyConnection.getLocalPort());
		} else {
			Log.d(TAG, "ServerSocket isn't bound.");
		}
	}

}
