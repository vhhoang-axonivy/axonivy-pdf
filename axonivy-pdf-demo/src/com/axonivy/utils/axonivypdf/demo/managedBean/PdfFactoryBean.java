package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

import com.aspose.cells.Workbook;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.facades.PdfFileEditor;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.PageSetup;
import com.aspose.words.RelativeHorizontalPosition;
import com.aspose.words.RelativeVerticalPosition;
import com.aspose.words.SaveFormat;
import com.aspose.words.WrapType;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private static final String PDF_EXTENSION = ".pdf";
	private static final String TIMES_NEW_ROMAN_FONT = "Times New Roman";
	private static final String DOT = ".";
	private UploadedFile uploadedFile;
	private UploadedFiles uploadedFiles;
	private DefaultStreamedContent mergedFile;
	private DefaultStreamedContent convertedPdfFile;
	private int fromPage;
	private int toPage;
	private DefaultStreamedContent splitFile;

	public void convertImageToPdf() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Document doc = new Document();
			DocumentBuilder builder = new DocumentBuilder(doc);

			com.aspose.words.Shape image = builder.insertImage(input);

			image.setRelativeHorizontalPosition(RelativeHorizontalPosition.PAGE);
			image.setRelativeVerticalPosition(RelativeVerticalPosition.PAGE);
			image.setLeft(0);
			image.setTop(0);
			image.setWrapType(WrapType.NONE);

			PageSetup ps = builder.getPageSetup();
			ps.setPageWidth(image.getWidth());
			ps.setPageHeight(image.getHeight());

			doc.save(output, SaveFormat.PDF);
			setConvertedPdfFile(buildFileStream(output.toByteArray(), "converted.pdf"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void convertToPdf() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			String fileName = uploadedFile.getFileName().toLowerCase();

			if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".odt")
					|| fileName.endsWith(".txt") || fileName.endsWith(".md")) {
				Document doc = new Document(input);
				doc.save(output, SaveFormat.PDF);
			} else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
				Workbook workbook = new Workbook(input);
				workbook.save(output, com.aspose.cells.SaveFormat.PDF);
			} else if (fileName.endsWith(".html")) {
				String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document();
				Page page = pdfDoc.getPages().add();
				TextFragment text = new TextFragment(html);
				text.getTextState().setFontSize(12);
				text.getTextState().setFont(FontRepository.findFont(TIMES_NEW_ROMAN_FONT));
				page.getParagraphs().add(text);
				pdfDoc.save(output);
				pdfDoc.close();
			} else if (fileName.endsWith(".pdf")) {
				com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document(input);
				pdfDoc.save(output);
				pdfDoc.close();
			} else {
				throw new IllegalArgumentException("Unsupported file type: " + fileName);
			}
			setConvertedPdfFile(buildFileStream(output.toByteArray(), "converted.pdf"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void merge() {
		if (uploadedFiles == null || uploadedFiles.getFiles().isEmpty()) {
			return;
		}

		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			int uploadedFilesSize = uploadedFiles.getFiles().size();
			InputStream[] inputStreams = new InputStream[uploadedFilesSize];

			for (int i = 0; i < uploadedFilesSize; i++) {
				inputStreams[i] = uploadedFiles.getFiles().get(i).getInputStream();
			}

			PdfFileEditor editor = new PdfFileEditor();

			boolean result = editor.concatenate(inputStreams, output);
			if (!result) {
				System.out.println("Merging failed");
				return;
			}

			byte[] mergedBytes = output.toByteArray();

			setMergedFile(DefaultStreamedContent.builder().name("merged.pdf").contentType("application/pdf")
					.stream(() -> new ByteArrayInputStream(mergedBytes)).build());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	public void splitAndPrepareDownload() {
//		if (uploadedFile == null) {
//			return;
//		}
//
//		try (InputStream input = uploadedFile.getInputStream()) {
//
//			// Choose memory setting:
//			// - MemoryUsageSetting.setupMainMemoryOnly() -> keep everything in RAM
//			// - MemoryUsageSetting.setupTempFileOnly() -> always use temp files
//			// - MemoryUsageSetting.setupMixed(...) -> hybrid
//			MemoryUsageSetting mem = MemoryUsageSetting.setupMixed(10 * 1024 * 1024); // 10MB threshold
//
//			// Load document with Loader (PDFBox 3.x)
//			try (PDDocument srcDoc = Loader.loadFDF(input);
//					PDDocument newDoc = new PDDocument();
//					ByteArrayOutputStream output = new ByteArrayOutputStream()) {
//
//				int totalPages = srcDoc.getNumberOfPages();
//				int start = Math.max(1, fromPage);
//				int end = Math.min(toPage, totalPages);
//				if (start > end) {
//					// handle invalid range
//					start = 1;
//					end = Math.min(1, totalPages);
//				}
//
//				for (int i = start; i <= end; i++) {
//					newDoc.addPage(srcDoc.getPage(i - 1));
//				}
//
//				newDoc.save(output);
//				byte[] bytes = output.toByteArray();
//				splitFile = DefaultStreamedContent.builder().name("split_pages_" + start + "_to_" + end + ".pdf")
//						.contentType("application/pdf").stream(() -> new ByteArrayInputStream(bytes)).build();
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	private String updateFileWithPdfExtension(String orginalFileName) {
		String baseName = StringUtils.isNotBlank(orginalFileName)
				? StringUtils.substringBeforeLast(orginalFileName, DOT)
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
