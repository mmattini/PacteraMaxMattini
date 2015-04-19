package com.example.prot.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

public class DataStore {

	private static final String LOG_TAG = DataStore.class.getSimpleName();

	// Some of the images (e.g
	// http://images.findicons.com/files/icons/662/world_flag/128/flag_of_canada.png)
	// will result in
	// connection timeout. For this reason, the timeout is set to 10 seconds
	// instead of default 30 seconds
	private static final int CONNECTION_TIMEOUT_MS = 10000;
	private static final int READ_TIMEOUT_MS = 10000;

	private static final String WEBSITE_URL = "https://dl.dropboxusercontent.com/u/746330/facts.json";
	private static DataStore _instance;
	private int progressCount = 0;
	private List<FeedItem> feedItems = new ArrayList<FeedItem>();
	private String feedTitle;
	private boolean makeSameImageSize = false;

	// Initially, all requests to download images are added to the imagesPending
	// list. As
	// images are being downloaded, they are put into the imagesCache.
	// Need to provide concurrency as different threads can access them.
	private ConcurrentMap<String, Bitmap> imagesCache = new ConcurrentHashMap<String, Bitmap>();
	private CopyOnWriteArrayList<String> imagesPending = new CopyOnWriteArrayList<String>();

	public static DataStore getInstance() {
		if (_instance == null) {
			_instance = new DataStore();
		}
		return _instance;
	}

	/**
	 * This method is used in Android Test Unit 
	 * @param context
	 * @param listener
	 */
	public void getDataFromAssets(Context context, IDataReadyListener listener) {

		if (!feedItems.isEmpty()) {
			return;
		}
		resetData();

		InputStream is = null;
		JsonReader reader = null;
		try {

			// 1) Read simulated data from a file in the assets directory
			is = context.getAssets().open("data.txt");

			// 2) Parse JSon
			reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
			reader.setLenient(true);
			parseData(reader);

			progressCount = 0;
			listener.processNewData(feedItems, feedTitle);

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					is.close();
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @return number of images (image url exists) in the feed
	 */
	public int getImageCount() {
		int count = 0;
		for (FeedItem item : feedItems) {
			if (item.HasImage()) {
				count++;
			}
		}
		return count;
	}

	private void resetData() {
		synchronized (this) {
			feedTitle = "";
			feedItems.clear();
		}
	}

	private void parseData(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("title".equalsIgnoreCase(name)) {

				if (reader.peek() == JsonToken.NULL) {
					reader.skipValue();
					feedTitle = "";
				} else {
					feedTitle = reader.nextString();
				}

			} else if ("rows".equalsIgnoreCase(name)) {
				parseRows(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}

	private void parseRows(JsonReader reader) throws IOException {
		reader.beginArray();

		while (reader.hasNext()) {
			parseRow(reader);
		}
		reader.endArray();

	}

	private void parseRow(JsonReader reader) throws IOException {

		FeedItem feedItem = new FeedItem();
		reader.beginObject();

		while (reader.hasNext()) {
			String name = reader.nextName();

			if ("description".equalsIgnoreCase(name)) {

				if (reader.peek() == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setDescription("");
				} else {
					feedItem.setDescription(reader.nextString());
				}
			} else if ("title".equalsIgnoreCase(name)) {

				if (reader.peek() == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setTitle("");
				} else {
					feedItem.setTitle(reader.nextString());
				}
			} else if ("imageHref".equalsIgnoreCase(name)) {

				if (reader.peek() == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setUrlName("");
				} else {
					feedItem.setUrlName(reader.nextString());
				}

			} else {
				reader.skipValue();
			}
		}

		// Remove bad items with no data in them
		if (!feedItem.IsNullOrEmpty()) {
			feedItems.add(feedItem);
			// Log.d(LOG_TAG, feedItem.toString());
		} else {
			Log.d(LOG_TAG, "Empty row -> discarded");
		}

		reader.endObject();
	}

	private class DownloadDataTask extends AsyncTask<String, Integer, Void> {

		private IDataReadyListener listener;

		DownloadDataTask(IDataReadyListener listener) {
			this.listener = listener;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		}

		@Override
		protected void onPostExecute(Void result) {
			imagesCache.clear();
			imagesPending.clear();

			progressCount = 0;
			listener.processNewData(feedItems, feedTitle);
		}

		
		@Override
		protected Void doInBackground(String... params) {
			String url = params[0];

			InputStream is = null;
			JsonReader reader = null;
			try {
				is = downloadUrl(url);

				// 2) Parse JSon
				reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
				reader.setLenient(true);
				parseData(reader);

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						is.close();
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;

		}
	}

	
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(READ_TIMEOUT_MS /* milliseconds */);
		conn.setConnectTimeout(CONNECTION_TIMEOUT_MS /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);

		conn.connect();
		InputStream in = conn.getInputStream();
		return in;
	}

	private void setResults(IDataReadyListener listener, String urlName, Bitmap result) {
		imagesPending.remove(urlName);
		
		DataStore.getInstance().imagesCache.put(urlName, result);
		listener.notifyImageDownloaded(urlName);

	}

	public void refreshData(IDataReadyListener listener) {
		resetData();
		new DownloadDataTask(listener).execute(WEBSITE_URL);
	}

	private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {

		String urlName = null;

		IDataReadyListener listener;

		public DownloadImageTask(IDataReadyListener listener) {
			this.listener = listener;
		}

		protected Bitmap doInBackground(String... urls) {
			urlName = urls[0];

			return getBitmapFromURL(urlName);
		}

		protected void onProgressUpdate(Integer... progress) {
			listener.setProgressPercent(progress[0]);
		}

		
		protected void onPostExecute(Bitmap bitmap) {

			if (bitmap != null) {
				Log.d(LOG_TAG, "Downloaded bitmap from " + urlName);
				DataStore.getInstance().setResults(listener, urlName, bitmap);
			}
		}

		public Bitmap getBitmapFromURL(String urlName) {
			try {
				URL url = new URL(urlName);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
				connection.setReadTimeout(READ_TIMEOUT_MS);
				connection.connect();
				InputStream input = connection.getInputStream();
				Bitmap myBitmap = BitmapFactory.decodeStream(input);

				if (makeSameImageSize) {
					myBitmap = getResizedBitmap(myBitmap, 240, 320);
				}
				return myBitmap;
			} catch (MalformedURLException e) {
				Log.e(LOG_TAG, "MalformedURLException error " + e.getMessage());
				return null;
			} catch (IOException e) {
				Log.e(LOG_TAG, "IOException error " + e.getMessage());
				return null;
			} finally {
				progressCount++;
				int imageCount = getImageCount();
				if (imageCount > 0) {
					int percent = (progressCount * 100) / imageCount;
					Log.d(LOG_TAG, "progress " + progressCount + " of " + imageCount);
					if (percent <= 2) {
						// Minimum progress is 2 % to make it easier for the
						// user to see
						percent = 2;
					}
					publishProgress(percent);
				}

			}
		}

		/* Decodes image and scales it to reduce memory consumption. Used only if setting requires all images to be of teh same size */
		private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
			int width = bm.getWidth();
			int height = bm.getHeight();
			float scaleWidth = ((float) newWidth) / width;
			float scaleHeight = ((float) newHeight) / height;
			// CREATE A MATRIX FOR THE MANIPULATION
			Matrix matrix = new Matrix();
			// RESIZE THE BIT MAP
			matrix.postScale(scaleWidth, scaleHeight);
			// RECREATE THE NEW BITMAP
			Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
			return resizedBitmap;
		}
	}

	/**
	 * Try to find image in cache. If not, try to download it. However, before
	 * downloading an image make sure it is not in the pending list as we don't
	 * want top download an image twice or more
	 * 
	 * @param listener listener will be called when the image is downloaded to refresh display
	 * @param urlName url of the image
	 * @return null if the image is not in the cache, otherwise return the bitmap image
	 */
	public Bitmap lazyGetBitmap(IDataReadyListener listener, String urlName) {
		if (imagesCache.containsKey(urlName)) {
			// Great the image has been downloaded

			return imagesCache.get(urlName);
		}

		if (imagesPending.contains(urlName)) {
			// Do nothing as the download is in progress
		} else {

			// Need to download the image: insert it in the pending images list.
			imagesPending.add(urlName);
			new DownloadImageTask(listener).execute(urlName);

		}
		return null;
	}

	public void makeSameImageSize(boolean b) {
		makeSameImageSize = b;

	}

	public boolean GetMakeSameImageSize() {

		return makeSameImageSize;
	}

}
