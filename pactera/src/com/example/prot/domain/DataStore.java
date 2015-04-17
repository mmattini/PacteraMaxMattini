package com.example.prot.domain;

import android.graphics.Bitmap;

public class DataStore {

	private static final String LOG_TAG = DataStore.class.getSimpleName();

	private static DataStore _instance;

	public static DataStore getInstance() {
		if (_instance == null) {
			_instance = new DataStore();
		}
		return _instance;
	}

	public Bitmap lazyGetBitmap(IDataReadyListener listener, String urlName) {
		// TODO Auto-generated method stub
		return null;
	}

	public void refreshData(IDataReadyListener listener) {
		// TODO Auto-generated method stub
		
	}

}
