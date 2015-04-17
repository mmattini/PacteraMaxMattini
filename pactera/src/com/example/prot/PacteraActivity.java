package com.example.prot;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.prot.domain.DataStore;
import com.example.prot.domain.FeedItem;
import com.example.prot.domain.IDataReadyListener;

public class PacteraActivity extends Activity implements IDataReadyListener {

	protected static final String LOG_TAG = PacteraActivity.class.getSimpleName();

	private MyAdapter adapter;
	private ActionBar actionBar;
	private ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_pactera);

		setActionBar(this, "Pactera");

		ListView listView = (ListView) findViewById(R.id.listView);

		adapter = new MyAdapter(this, this);
		listView.setAdapter(adapter);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		boolean initFromAssets = true;
		
		if (initFromAssets) {
			startProgress();
			DataStore.getInstance().getDataFromAssets(this, this);
		} else {
			refreshData();
		}
	}

	private void startProgress() {
		progressBar.setProgress(1);
		progressBar.setVisibility(View.VISIBLE);
	}

	public void setProgressPercent(Integer percent) {
		progressBar.setProgress(percent);
		if(percent >= 100){
			progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem realSize = menu.findItem(R.id.action_image_size_real);
		MenuItem sameSize = menu.findItem(R.id.action_image_same_size);

		boolean makeSameImageSize = DataStore.getInstance().GetMakeSameImageSize();

		sameSize.setVisible(!makeSameImageSize);
		realSize.setVisible(makeSameImageSize);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_refresh:
			refreshData();
			return true;

		case R.id.action_image_size_real:
			DataStore.getInstance().makeSameImageSize(false);
			refreshData();
			this.invalidateOptionsMenu();
			return true;

		case R.id.action_image_same_size:
			DataStore.getInstance().makeSameImageSize(true);
			refreshData();
			this.invalidateOptionsMenu();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void refreshData() {
		startProgress();
		DataStore.getInstance().refreshData(this);
	}

	private void setActionBar(Activity activity, final String title) {

		actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(false);

		ColorDrawable colorDrawable = new ColorDrawable();
		colorDrawable.setColor(getResources().getColor(R.color.lightBlue));
		actionBar.setBackgroundDrawable(colorDrawable);
		actionBar.setTitle(title);
		actionBar.show();
	}

	private class MyAdapter extends BaseAdapter {

		private List<FeedItem> items = new ArrayList<FeedItem>();
		private Activity activity;
		private IDataReadyListener listener;

		public MyAdapter(Activity activity, IDataReadyListener listener) {
			super();

			this.activity = activity;
			this.listener = listener;
		}

		public void SetItems(List<FeedItem> theList) {

			List<FeedItem> newItems = new ArrayList<FeedItem>(theList.size());
			for (FeedItem item : theList) {
				newItems.add(item);
			}
			synchronized (this) {
				this.items = newItems;

			}
			notifyDataSetChanged();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {

			return true;
		}

		@Override
		public FeedItem getItem(int position) {

			return items.get(position);
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return (1);
		}

		@Override
		public int getItemViewType(int position) {

			return (0);
		}

		private class ItemViewHolder {
			TextView description = null;
			TextView title = null;
			ImageView image = null;

			ItemViewHolder(View row) {
				this.description = (TextView) row.findViewById(R.id.description);
				this.title = (TextView) row.findViewById(R.id.title);
				this.image = (ImageView) row.findViewById(R.id.image);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FeedItem item = getItem(position);

			View view = null;
			if (convertView == null) {

				LayoutInflater inflater = activity.getLayoutInflater();
				view = inflater.inflate(R.layout.feed_cell, null);
				final ItemViewHolder viewHolder = new ItemViewHolder(view);

				viewHolder.image.setVisibility(View.GONE);

				view.setTag(viewHolder);
			} else {

				view = convertView;
			}
			ItemViewHolder holder = (ItemViewHolder) view.getTag();

			String title = item.getTitle();
			holder.title.setText(title);

			String description = item.getDescription();
			holder.description.setText(description);

			String urlName = items.get(position).getUrlName();
			holder.image.setVisibility(View.GONE);
			if (!urlName.isEmpty()) {

				Bitmap image = DataStore.getInstance().lazyGetBitmap(listener, urlName);
				if (image != null) {
					holder.image.setImageBitmap(image);
					holder.image.setVisibility(View.VISIBLE);
				} else {
					// We could display a place holder image, as in code below,
					// but it is kind of ugly
					// so I removed it
					// holder.image.setImageDrawable(getResources().getDrawable(R.drawable.icon_btn_social));
					// holder.image.setVisibility(View.VISIBLE);
				}
			}
			return view;
		}
	}

	@Override
	public void processNewData(List<FeedItem> newItems, String newTitle) {

		actionBar.setTitle(newTitle);
		adapter.SetItems(newItems);
	}

	@Override
	public void notifyDataSetChanged() {
		adapter.notifyDataSetChanged();
	}

}
