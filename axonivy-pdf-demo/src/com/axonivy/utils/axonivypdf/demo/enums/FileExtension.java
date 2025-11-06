package com.axonivy.utils.axonivypdf.demo.enums;

public enum FileExtension {
	DOC(".doc"), DOCX(".docx"), ODT(".odt"), TXT(".txt"), MD(".md"), XLS(".xls"), XLSX(".xlsx"), HTML(".html"),
	PDF(".pdf");

	private final String extension;

	private FileExtension(String extension) {
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}
}
