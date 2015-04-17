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

	private static DataStore _instance;

	public static DataStore getInstance() {
		if (_instance == null) {
			_instance = new DataStore();
		}
		return _instance;
	}

	private List<FeedItem> feedItems = new ArrayList<FeedItem>();
	private String feedTitle;
	private final String WEBSITE_URL = "https://dl.dropboxusercontent.com/u/746330/facts.json";
	private ConcurrentMap<String, Bitmap> bitmapMap = new ConcurrentHashMap<String, Bitmap>();
	private CopyOnWriteArrayList<String> urlToLoad = new CopyOnWriteArrayList<String>();

	// [[]] unit tests
	void getDataFromAssets(Context context, IDataReadyListener listener) {

		// Since data is static from a file, do not parse it again if already
		// done
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
			reader.setLenient(true);// [[]]
			parseData(reader);

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
				// [[]] peek affect perfo
				JsonToken peek = reader.peek();
				if (peek == JsonToken.NULL) {
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
			// Log.i(LOG_TAG, name);

			if ("description".equalsIgnoreCase(name)) {
				JsonToken peek = reader.peek();
				if (peek == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setDescription("");
				} else {
					feedItem.setDescription(reader.nextString());
				}
			} else if ("title".equalsIgnoreCase(name)) {
				JsonToken peek = reader.peek();
				if (peek == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setTitle("");
				} else {
					feedItem.setTitle(reader.nextString());
				}
			} else if ("imageHref".equalsIgnoreCase(name)) {
				JsonToken peek = reader.peek();
				if (peek == JsonToken.NULL) {
					reader.skipValue();
					feedItem.setUrlName("");
				} else {
					feedItem.setUrlName(reader.nextString());
				}

			} else {
				reader.skipValue();
			}
		}

		if (!feedItem.IsNullOrEmpty()) {
			feedItems.add(feedItem);
			Log.i(LOG_TAG, feedItem.toString());
		} else {
			Log.i(LOG_TAG, "Empty row -> discarded");
		}

		reader.endObject();
	}

	private class DownloadRenameTask extends AsyncTask<String, Integer, Void> {

		private IDataReadyListener listener;

		DownloadRenameTask(IDataReadyListener listener) {
			this.listener = listener;

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		}

		@Override
		protected void onPostExecute(Void result) {
			bitmapMap.clear();
			urlToLoad.clear();

			listener.processNewData(feedItems, feedTitle);
		}

		// [[]] use better thread pool
		@Override
		protected Void doInBackground(String... params) {
			String url = params[0];

			InputStream is = null;
			JsonReader reader = null;
			try {

				is = downloadUrl(url);

				// 2) Parse JSon
				reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
				reader.setLenient(true);// [[]]
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

	final int READ_TIMEOUT_MS = 10000;
	final int CONNECTION_TIMEOUT_MS = 10000;

	// [[]] change timeout to speed up
	protected InputStream downloadUrl(String urlString) throws IOException {
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
		urlToLoad.remove(urlName);
		// [[]]xp handle error
		DataStore.getInstance().bitmapMap.put(urlName, result);
		listener.notifyDataSetChanged();

	}

	public void refreshData(IDataReadyListener listener) {
		resetData();
		new DownloadRenameTask(listener).execute(WEBSITE_URL);
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		String urlName = null;// [[]]xp

		IDataReadyListener listener;

		public DownloadImageTask(IDataReadyListener listener) {
			this.listener = listener;
		}

		protected Bitmap doInBackground(String... urls) {
			urlName = urls[0];

			return getBitmapFromURL(urlName);
		}

		protected void onPostExecute(Bitmap bitmap) {
			System.out.println("Downloaded " + urlName);

			if (bitmap != null) {
				DataStore.getInstance().setResults(listener, urlName, bitmap);
			}
		}

		public Bitmap getBitmapFromURL(String src) {
			try {
				URL url = new URL(src);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setConnectTimeout(10000);
				connection.setReadTimeout(10000);
				connection.connect();
				InputStream input = connection.getInputStream();
				Bitmap myBitmap = BitmapFactory.decodeStream(input);
				return myBitmap;
			} catch (MalformedURLException e) {
				return null;
			} catch (IOException e) {
				// Log exception
				return null;
			}
		}

		private Bitmap getBitmapDownloaded(String url) {

			Bitmap bitmap = null;
			try {
				bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
				// bitmap = getResizedBitmap(bitmap, 150, 250);
				return bitmap;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		/** [[]] decodes image and scales it to reduce memory consumption **/
		public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
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

	public Bitmap lazyGetBitmap(IDataReadyListener listener, String urlName) {
		if (bitmapMap.containsKey(urlName)) {
			Bitmap b = DataStore.getInstance().bitmapMap.get(urlName);
			return b;
		} else if (urlToLoad.contains(urlName)) {

		} else {

			DataStore.getInstance().urlToLoad.add(urlName);// [[]] sync
			new DownloadImageTask(listener).execute(urlName);

		}
		return null;
	}

}
