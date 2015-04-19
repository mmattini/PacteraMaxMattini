package com.example.prot.test;

import java.util.List;

import android.test.AndroidTestCase;

import com.example.prot.domain.DataStore;
import com.example.prot.domain.FeedItem;
import com.example.prot.domain.IDataReadyListener;

public class TestJson extends AndroidTestCase {

	private DataReadyListener dataReadyListener = null;

	private class DataReadyListener implements IDataReadyListener {
		public List<FeedItem> items;
		public String title;
		public boolean dataReadyForTest = false;

		@Override
		public void processNewData(List<FeedItem> items, String newTitle) {
			dataReadyForTest = true;
			this.items = items;
			this.title = newTitle;
			synchronized (this) {

				notifyAll();
			}
		}

		@Override
		public void notifyImageDownloaded(String urlName) {
			//
		}

		@Override
		public void setProgressPercent(Integer percent) {
			//
		}

	}

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		dataReadyListener = new DataReadyListener();
		DataStore.getInstance().getDataFromAssets(getContext(), dataReadyListener);

		synchronized (dataReadyListener) {
			try {
				dataReadyListener.wait(2000);
			} catch (InterruptedException e) {

				assertNotNull("Timeout", null);
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();

		dataReadyListener = null;
	}

	public void testItemParsedCorrectly() {
		assertTrue(dataReadyListener.dataReadyForTest);
		assertNotNull(dataReadyListener.items);
		assertEquals("About Canada", dataReadyListener.title);
		assertEquals("Number of results", 15, dataReadyListener.items.size());
		assertEquals("Number of images", 10, DataStore.getInstance().getImageCount());
	}

}
