package com.axonivy.utils.axonivypdf.demo.enums;

public enum TextExtractedType {
	ALL("all"), HIGHLIGHTED("highlighted");

	private final String type;

	private TextExtractedType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
