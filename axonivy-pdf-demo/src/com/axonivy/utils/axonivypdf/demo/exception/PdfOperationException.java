package com.axonivy.utils.axonivypdf.demo.exception;

public class PdfOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PdfOperationException(String message) {
		super(message);
	}

	public PdfOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdfOperationException(Throwable cause) {
		super(cause);
	}
}
