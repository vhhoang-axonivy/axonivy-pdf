package com.axonivy.utils.axonivypdf.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.aspose.words.DocSaveOptions;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.aspose.words.SaveOptions;
import com.axonivy.utils.docs.common.AbstractConverter;

public class DocumentConverter extends AbstractConverter<DocumentConverter, Document> {

	/**
	 * Creates a new DocumentConverter instance. Package-private constructor to
	 * ensure creation only through WordFactory.
	 */
	DocumentConverter() {
		super();
	}

	/**
	 * Loads a document from an InputStream.
	 * 
	 * @param inputStream the input stream to load from
	 * @return the loaded Document
	 * @throws Exception if loading fails
	 */
	@Override
	protected Document loadFromInputStream(InputStream inputStream) throws Exception {
		return new Document(inputStream);
	}

	/**
	 * Loads a document from a file path.
	 * 
	 * @param filePath the file path to load from
	 * @return the loaded Document
	 * @throws Exception if loading fails
	 */
	@Override
	protected Document loadFromFile(String filePath) throws Exception {
		return new Document(filePath);
	}

	/**
	 * Saves the document to an OutputStream.
	 * 
	 * @param document     the document to save
	 * @param outputStream the output stream to save to
	 * @param format       the format to save in
	 * @throws Exception if saving fails
	 */
	@Override
	protected void saveToStream(Document document, ByteArrayOutputStream outputStream, int format) throws Exception {
		SaveOptions options = DocSaveOptions.createSaveOptions(format);
		document.save(outputStream, options);
	}

	/**
	 * Saves the document to a file.
	 * 
	 * @param document   the document to save
	 * @param outputPath the file path to save to
	 * @param format     the format to save in
	 * @throws Exception if saving fails
	 */
	@Override
	protected void saveToFile(Document document, String outputPath, int format) throws Exception {
		SaveOptions options = DocSaveOptions.createSaveOptions(format);
		document.save(outputPath, options);
	}

	/**
	 * Gets the PDF format constant for Word documents.
	 * 
	 * @return the PDF format constant (SaveFormat.PDF)
	 */
	@Override
	protected int getPdfFormat() {
		return SaveFormat.PDF;
	}
}
