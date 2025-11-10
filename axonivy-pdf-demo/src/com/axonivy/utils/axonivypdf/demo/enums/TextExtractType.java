package com.axonivy.utils.axonivypdf.demo.enums;

public enum TextExtractType {
	ALL("all"), HIGHLIGHTED("highlighted");

	private final String type;

	private TextExtractType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
