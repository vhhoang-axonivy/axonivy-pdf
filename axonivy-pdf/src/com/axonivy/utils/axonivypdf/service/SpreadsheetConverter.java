package com.axonivy.utils.axonivypdf.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.axonivy.utils.docs.common.AbstractConverter;

/**
 * Fluent API for spreadsheet conversion operations. Provides a chain of methods
 * to convert spreadsheets from one format to another.
 */
public class SpreadsheetConverter extends AbstractConverter<SpreadsheetConverter, Workbook> {
	/**
	 * Creates a new SpreadsheetConverter instance. Package-private constructor to
	 * ensure creation only through ExcelFactory.
	 */
	SpreadsheetConverter() {
		super();
	}

	@Override
	protected Workbook loadFromInputStream(InputStream inputStream) throws Exception {
		return new Workbook(inputStream);
	}

	@Override
	protected Workbook loadFromFile(String filePath) throws Exception {
		return new Workbook(filePath);
	}

	@Override
	protected void saveToStream(Workbook document, ByteArrayOutputStream outputStream, int format) throws Exception {
		document.save(outputStream, format);
	}

	@Override
	protected void saveToFile(Workbook document, String outputPath, int format) throws Exception {
		document.save(outputPath, format);
	}

	@Override
	protected int getPdfFormat() {
		return SaveFormat.PDF;
	}

	// Additional convenience methods specific to spreadsheets

	/**
	 * Converts the spreadsheet to XLSX format.
	 * 
	 * @return this converter instance for method chaining
	 */
	public SpreadsheetConverter toXlsx() {
		return to(SaveFormat.XLSX);
	}

	/**
	 * Converts the spreadsheet to XLS format.
	 * 
	 * @return this converter instance for method chaining
	 */
	public SpreadsheetConverter toXls() {
		return to(SaveFormat.EXCEL_97_TO_2003);
	}

	/**
	 * Converts the spreadsheet to CSV format.
	 * 
	 * @return this converter instance for method chaining
	 */
	public SpreadsheetConverter toCsv() {
		return to(SaveFormat.CSV);
	}

	/**
	 * Converts the spreadsheet to HTML format.
	 * 
	 * @return this converter instance for method chaining
	 */
	public SpreadsheetConverter toHtml() {
		return to(SaveFormat.HTML);
	}

	/**
	 * Converts the spreadsheet to ODS format.
	 * 
	 * @return this converter instance for method chaining
	 */
	public SpreadsheetConverter toOds() {
		return to(SaveFormat.ODS);
	}
}
