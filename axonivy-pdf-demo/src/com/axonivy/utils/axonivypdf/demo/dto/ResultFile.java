package com.axonivy.utils.axonivypdf.demo.dto;

import org.primefaces.model.DefaultStreamedContent;

public class ResultFile {
	private String name;
	private int pageNumber;
	
	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	private byte[] bytes;
	private DefaultStreamedContent defaultStreamedContent;

	public DefaultStreamedContent getDefaultStreamedContent() {
		return defaultStreamedContent;
	}

	public void setDefaultStreamedContent(DefaultStreamedContent defaultStreamedContent) {
		this.defaultStreamedContent = defaultStreamedContent;
	}

	// getters & setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
}
