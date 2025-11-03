package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

import com.aspose.cells.Workbook;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.axonivy.utils.axonivypdf.service.PdfFactory;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private static final String PDF_EXTENSION = ".pdf";
	private static final String DOT = ".";
	private UploadedFile uploadedFile;
	private UploadedFiles uploadedFiles;
	private DefaultStreamedContent mergedFile;
	private DefaultStreamedContent convertedPdfFile;

	public void uploadMultiple() {
		if (uploadedFiles != null) {
			try {
				PDFMergerUtility merger = new PDFMergerUtility();
				for (UploadedFile uf : uploadedFiles.getFiles()) {
					File temp = File.createTempFile("upload-", ".pdf");
					temp.deleteOnExit();
					try (InputStream in = uf.getInputStream(); OutputStream out = new FileOutputStream(temp)) {
						in.transferTo(out);
					}
					merger.addSource(temp);
				}

				File merged = File.createTempFile("merged-", ".pdf");
				merged.deleteOnExit();

				merger.setDestinationFileName(merged.getAbsolutePath());
				merger.mergeDocuments(null);

				setMergedFile(DefaultStreamedContent.builder().name("combined.pdf").contentType("application/pdf")
						.stream(() -> {
							try {
								return new FileInputStream(merged);
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						}).build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void convertToPdf() {
		if (uploadedFile == null) {
			return;
		}

		String orginalFileName = uploadedFile.getFileName().toLowerCase();
		String pdfFileName = updateFileWithPdfExtension();

		if (orginalFileName.endsWith(".doc") || orginalFileName.endsWith(".docx")) {
			setConvertedPdfFile(buildFileStream(
					PdfFactory.documentConvert().from(uploadedFile.getContent()).toPdf().asBytes(), pdfFileName));
		} else if (orginalFileName.endsWith(".xls") || orginalFileName.endsWith(".xlsx")) {
			setConvertedPdfFile(buildFileStream(
					PdfFactory.spreadsheetConvert().from(uploadedFile.getContent()).toPdf().asBytes(), pdfFileName));
		} else {
			throw new IllegalArgumentException("Unsupported file type");
		}
	}

	private String updateFileWithPdfExtension() {
		String originalName = uploadedFile.getFileName();
		String baseName = StringUtils.isNotBlank(originalName) ? StringUtils.substringBeforeLast(originalName, DOT)
				: "workbook";
		return baseName + PDF_EXTENSION;
	}

	private DefaultStreamedContent buildFileStream(byte[] byteContent, String fileName) {
		return DefaultStreamedContent.builder().name(fileName).contentType("application/pdf")
				.stream(() -> new ByteArrayInputStream(byteContent)).build();
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public UploadedFiles getUploadedFiles() {
		return uploadedFiles;
	}

	public void setUploadedFiles(UploadedFiles uploadedFiles) {
		this.uploadedFiles = uploadedFiles;
	}

	public void setMergedFile(DefaultStreamedContent mergedFile) {
		this.mergedFile = mergedFile;
	}

	public DefaultStreamedContent getMergedFile() {
		return mergedFile;
	}

	public DefaultStreamedContent getConvertedPdfFile() {
		return convertedPdfFile;
	}

	public void setConvertedPdfFile(DefaultStreamedContent convertedPdfFile) {
		this.convertedPdfFile = convertedPdfFile;
	}
}
