package com.example.prot.domain;

import java.util.List;

public interface IDataReadyListener {

	void processNewData(List<FeedItem> items, String newTitle);
	
	void notifyDataSetChanged();

	void setProgressPercent(Integer percent);
}
