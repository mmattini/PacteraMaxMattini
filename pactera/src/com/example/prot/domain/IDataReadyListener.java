package com.example.prot.domain;

import java.util.List;

public interface IDataReadyListener {

	/**
	 * Called when feed items and title are available
	 * @param items feed items
	 * @param title feed title
	 */
	void processNewData(List<FeedItem> items, String title);
	
	/**
	 * Called when image finished downloading
	 * @param imageUrl
	 */
	void notifyImageDownloaded(String imageUrl);

	/**
	 * Report overall image download  
	 * @param percent percent of images have been downloaded
	 */
	void setProgressPercent(Integer percent);
}
