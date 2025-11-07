package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

import com.aspose.cells.Workbook;
import com.aspose.pdf.Color;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.FontStyles;
import com.aspose.pdf.HorizontalAlignment;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rotation;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextStamp;
import com.aspose.pdf.VerticalAlignment;
import com.aspose.pdf.facades.PdfFileEditor;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.PageSetup;
import com.aspose.words.RelativeHorizontalPosition;
import com.aspose.words.RelativeVerticalPosition;
import com.aspose.words.SaveFormat;
import com.aspose.words.WrapType;
import com.axonivy.utils.axonivypdf.demo.enums.FileExtension;
import com.axonivy.utils.axonivypdf.demo.enums.SplitOption;
import com.axonivy.utils.axonivypdf.service.PdfFactory;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private static final String PDF_EXTENSION = ".pdf";
	private static final String SAMPLE_WATERMARK = "ASPOSE_WATERMARK";
	private static final String ZIP_EXTENSION = ".zip";
	private static final String TIMES_NEW_ROMAN_FONT = "Times New Roman";
	private static final String SPLIT_PAGE_NAME_PATTERN = "%s_page_%d" + PDF_EXTENSION;
	private static final String DOT = ".";
	private SplitOption splitOption = SplitOption.ALL;
	private UploadedFile uploadedFile;
	private UploadedFiles uploadedFiles;
	private DefaultStreamedContent mergedFile;
	private DefaultStreamedContent convertedPdfFile;
	private DefaultStreamedContent fileForDownload;
	private DefaultStreamedContent splitFilesZip;
	private Integer startPage;
	private Integer endPage;

	@PostConstruct
	public void init() {
		PdfFactory.loadLicense();
	}

	public void onSplitOptionChange() {
		if (SplitOption.RANGE.equals(splitOption)) {
			initPageRange();
		}
	}

	public void initPageRange() {
		if (uploadedFile == null) {
			return;
		}
		if (SplitOption.RANGE.equals(splitOption)) {
			try (InputStream input = uploadedFile.getInputStream()) {
				com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);
				setStartPage(1);
				setEndPage(pdfDocument.getPages().size());
				pdfDocument.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void splitFileByRange(int pageSize, com.aspose.pdf.Document pdfDocument) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		int sp = (startPage == null) ? 1 : Math.max(1, startPage);
		int ep = (endPage == null) ? pageSize : Math.min(pageSize, endPage);
		if (sp > ep) {
			return;
		}
		com.aspose.pdf.Document newDoc = new com.aspose.pdf.Document();
		for (int i = sp; i <= ep; i++) {
			newDoc.getPages().add(pdfDocument.getPages().get_Item(i));
			newDoc.save(output);
		}
		newDoc.close();
		setFileForDownload(buildFileStream(output.toByteArray(), "split_pages.zip"));
	}

	public void splitFileByRange(InputStream input) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);
			int pageSize = pdfDocument.getPages().size();
			int sp = (startPage == null) ? 1 : Math.max(1, startPage);
			int ep = (endPage == null) ? pageSize : Math.min(pageSize, endPage);
			if (sp > ep) {
				pdfDocument.close();
				return;
			}
			com.aspose.pdf.Document newDoc = new com.aspose.pdf.Document();
			for (int i = sp; i <= ep; i++) {

				newDoc.getPages().add(pdfDocument.getPages().get_Item(i));
				newDoc.save(output);
				newDoc.close();
			}
			pdfDocument.close();
			setFileForDownload(buildFileStream(output.toByteArray(), "split_pages.zip"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void splitAndDownloadZipPdf() {
		if (uploadedFile == null) {
			return;
		}
		String baseName = StringUtils.substringBeforeLast(uploadedFile.getFileName(), DOT);

		try (InputStream input = uploadedFile.getInputStream()) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			if (SplitOption.ALL.equals(splitOption)) {
				splitFilesZip = null;
				Path tempDir = Files.createTempDirectory("split_pages_");
				int pageCount = 1;

				for (Page pdfPage : pdfDocument.getPages()) {
					com.aspose.pdf.Document newDoc = new com.aspose.pdf.Document();
					newDoc.getPages().add(pdfPage);

//					Path pageFile = tempDir.resolve(baseName + "_page_" + pageCount + PDF_EXTENSION);
					Path pageFile = tempDir.resolve(String.format(SPLIT_PAGE_NAME_PATTERN, baseName, pageCount));
					newDoc.save(pageFile.toString());
					newDoc.close();
					pageCount++;
				}

				Path zipPath = Files.createTempFile("split_pages_", ZIP_EXTENSION);
				try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
						ZipOutputStream zos = new ZipOutputStream(fos)) {

					Files.list(tempDir).forEach(path -> {
						try (InputStream fis = Files.newInputStream(path)) {
							ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
							zos.putNextEntry(zipEntry);

							byte[] buffer = new byte[1024];
							int length;
							while ((length = fis.read(buffer)) > 0) {
								zos.write(buffer, 0, length);
							}

							zos.closeEntry();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				}
				setFileForDownload(buildFileStream(Files.newInputStream(zipPath).readAllBytes(), "split_pages.zip"));
			} else {
				int pageSize = pdfDocument.getPages().size();
				if (isInputInvalid(getStartPage(), getEndPage(), pageSize)) {
					pdfDocument.close();
					return;
				}

				try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
					com.aspose.pdf.Document newDoc = new com.aspose.pdf.Document();
					for (int i = getStartPage(); i <= getEndPage(); i++) {
						Page pdfPage = pdfDocument.getPages().get_Item(i);
						newDoc.getPages().add(pdfPage);
					}
					newDoc.save(output);
					newDoc.close();
					setFileForDownload(buildFileStream(output.toByteArray(), "converted.pdf"));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			pdfDocument.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isInputInvalid(int startPage, int endPage, int originalDocPageSize) {
		boolean isInvalid = false;

		if (startPage < 0 || endPage < 0) {
			isInvalid = true;
		}

		if (startPage > endPage) {
			isInvalid = true;
		}

		if (endPage > originalDocPageSize || startPage > originalDocPageSize) {
			isInvalid = true;
		}

		return isInvalid;
	}

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
				return;
			}

			byte[] mergedBytes = output.toByteArray();
			setMergedFile(DefaultStreamedContent.builder().name("merged.pdf").contentType("application/pdf")
					.stream(() -> new ByteArrayInputStream(mergedBytes)).build());
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

			if (fileName.endsWith(FileExtension.DOC.getExtension())
					|| fileName.endsWith(FileExtension.DOCX.getExtension())
					|| fileName.endsWith(FileExtension.ODT.getExtension())
					|| fileName.endsWith(FileExtension.TXT.getExtension())
					|| fileName.endsWith(FileExtension.MD.getExtension())) {
				Document doc = new Document(input);
				doc.save(output, SaveFormat.PDF);
			} else if (fileName.endsWith(FileExtension.XLS.getExtension())
					|| fileName.endsWith(FileExtension.XLSX.getExtension())) {
				Workbook workbook = new Workbook(input);
				workbook.save(output, com.aspose.cells.SaveFormat.PDF);
			} else if (fileName.endsWith(FileExtension.HTML.getExtension())) {
				String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document();
				Page page = pdfDoc.getPages().add();
				TextFragment text = new TextFragment(html);
				text.getTextState().setFontSize(12);
				text.getTextState().setFont(FontRepository.findFont(TIMES_NEW_ROMAN_FONT));
				page.getParagraphs().add(text);
				pdfDoc.save(output);
				pdfDoc.close();
			} else if (fileName.endsWith(FileExtension.PDF.getExtension())) {
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

	public void addWatermark() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream inputStream = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(inputStream);

			TextStamp stamp = new TextStamp(SAMPLE_WATERMARK);
			stamp.setBackground(true);
			stamp.setHorizontalAlignment(HorizontalAlignment.Center);
			stamp.setVerticalAlignment(VerticalAlignment.Center);
			stamp.setRotate(Rotation.None);
			stamp.getTextState().setFont(FontRepository.findFont(TIMES_NEW_ROMAN_FONT));
			stamp.getTextState().setFontSize(40);
			stamp.getTextState().setFontStyle(FontStyles.Bold);
			stamp.getTextState().setForegroundColor(Color.getLightGray());
			stamp.setOpacity(0.3);

			for (Page page : pdfDocument.getPages()) {
				page.addStamp(stamp);
			}
			pdfDocument.save(output);
			pdfDocument.close();
			setFileForDownload(buildFileStream(output.toByteArray(), "converted.pdf"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	private String updateFileWithPdfExtension(String orginalFileName) {
//		String baseName = StringUtils.isNotBlank(orginalFileName)
//				? StringUtils.substringBeforeLast(orginalFileName, DOT)
//				: "workbook";
//		return baseName + PDF_EXTENSION;
//	}

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

	public SplitOption getSplitOption() {
		return splitOption;
	}

	public void setSplitOption(SplitOption splitOption) {
		this.splitOption = splitOption;
	}

	public Integer getStartPage() {
		return startPage;
	}

	public void setStartPage(Integer startPage) {
		this.startPage = startPage;
	}

	public Integer getEndPage() {
		return endPage;
	}

	public void setEndPage(Integer endPage) {
		this.endPage = endPage;
	}

	public DefaultStreamedContent getSplitFilesZip() {
		return splitFilesZip;
	}

	public void setSplitFilesZip(DefaultStreamedContent splitFilesZip) {
		this.splitFilesZip = splitFilesZip;
	}

	public DefaultStreamedContent getFileForDownload() {
		return fileForDownload;
	}

	public void setFileForDownload(DefaultStreamedContent fileForDownload) {
		this.fileForDownload = fileForDownload;
	}
}
