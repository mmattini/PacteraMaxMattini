package com.example.prot.domain;

public class FeedItem {

	String title;
	String description;
	String urlName;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(String urlName) {
		this.urlName = urlName;
	}

	public FeedItem(String title, String description, String urlName) {
		this.description = description;
		this.title = title;
		this.urlName = urlName;
	}

	public FeedItem() {
		this(null, null, null);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-> ").append("\n title = ").append(title).append("\n description = ").append(description).append("\n image = ").append(urlName);
		return sb.toString();
	}

	public boolean IsNullOrEmpty() {

		return (title == null || title.isEmpty()) && (description == null || description.isEmpty()) && (urlName == null || urlName.isEmpty());
	}
}
