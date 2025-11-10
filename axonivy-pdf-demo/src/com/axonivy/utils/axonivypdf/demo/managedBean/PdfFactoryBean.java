package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
import com.aspose.pdf.Annotation;
import com.aspose.pdf.Color;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.FontStyles;
import com.aspose.pdf.HighlightAnnotation;
import com.aspose.pdf.HorizontalAlignment;
import com.aspose.pdf.ImageFormat;
import com.aspose.pdf.ImagePlacement;
import com.aspose.pdf.ImagePlacementAbsorber;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rotation;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentCollection;
import com.aspose.pdf.TextStamp;
import com.aspose.pdf.VerticalAlignment;
import com.aspose.pdf.XImage;
import com.aspose.pdf.devices.JpegDevice;
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
import com.axonivy.utils.axonivypdf.demo.enums.TextExtractedType;
import com.axonivy.utils.axonivypdf.service.PdfFactory;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private static final String DOT = ".";
	private static final float DEFAULT_FONT_SIZE = 12;
	private static final double WATERMARK_OPACITY = 0.3;
	private static final float DEFAULT_WATERMARK_FONT_SIZE = 40;
	private static final String TEMP_ZIP_FILE_NAME = "split_pages";
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	private static final String SAMPLE_WATERMARK = "ASPOSE_WATERMARK";
	private static final String SPLIT_PAGE_NAME_PATTERN = "%s_page_%d";
	private static final String TIMES_NEW_ROMAN_FONT = "Times New Roman";
	private static final String MERGED_DOCUMENT_NAME = "merged_document" + FileExtension.PDF.getExtension();
	private static final String IMAGE_NAME_PATTERN = "%s_page_%d_image_%d" + FileExtension.PNG.getExtension();
	private static final String IMAGE_ZIP_NAME_PATTERN = "%s_images_zipped" + FileExtension.ZIP.getExtension();
	private static final String SPLIT_PAGE_ZIP_NAME_PATTERN = "%s_split_zipped" + FileExtension.ZIP.getExtension();
	private static final String RANGE_SPLIT_FILE_NAME_PATTERN = "%s_page_%d_to_%d" + FileExtension.PDF.getExtension();
	private SplitOption splitOption = SplitOption.ALL;
	private TextExtractedType textExtractedType = TextExtractedType.ALL;
	private UploadedFile uploadedFile;
	private UploadedFiles uploadedFiles;
	private DefaultStreamedContent fileForDownload;
	private Integer startPage;
	private Integer endPage;
	private List<FileExtension> otherDocumentTypes = Arrays.asList(FileExtension.DOCX, FileExtension.XLSX,
			FileExtension.PPTX, FileExtension.JPG, FileExtension.JPEG);
	private FileExtension selectedFileExtension = FileExtension.DOCX;

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

	public void extractHighlightedText() {
		Ivy.log().error("Extracted type: " + getTextExtractedType());
		if (uploadedFile == null) {
			return;
		}

		String originalName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream textStream = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(textStream, StandardCharsets.UTF_8)) {

			// Load the PDF document
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			StringBuilder highlightedText = new StringBuilder();

			// Loop through all pages
			for (Page page : pdfDocument.getPages()) {
				for (Annotation annotation : page.getAnnotations()) {
					// Filter only HighlightAnnotation
					if (annotation instanceof HighlightAnnotation) {
						HighlightAnnotation highlight = (HighlightAnnotation) annotation;

						// Get all marked text fragments
						TextFragmentCollection fragments = highlight.getMarkedTextFragments();
						for (TextFragment tf : fragments) {
							highlightedText.append(tf.getText()).append(System.lineSeparator());
						}
					}
				}
			}

			// Write all highlighted text to stream
			writer.write(highlightedText.toString());
			writer.flush();

			// Close PDF
			pdfDocument.close();

			// Prepare file for download
			setFileForDownload(
					buildFileStream(textStream.toByteArray(), replaceFileExtension(originalName, "_highlighted.txt")));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void extractTextFromPdf() {
		if (uploadedFile == null) {
			return; // No file uploaded
		}

		String originalName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream textStream = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(textStream, StandardCharsets.UTF_8)) {

			// Load the uploaded PDF
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			// Create a TextAbsorber to extract text from all pages
			com.aspose.pdf.TextAbsorber textAbsorber = new com.aspose.pdf.TextAbsorber();

			// Accept the absorber for all pages
			pdfDocument.getPages().accept(textAbsorber);

			// Get extracted text
			String extractedText = textAbsorber.getText();

			// Write extracted text into the stream
			writer.write(extractedText);
			writer.flush();

			// Close PDF document
			pdfDocument.close();

			// Prepare the file for download (your existing JSF utility)
			setFileForDownload(buildFileStream(textStream.toByteArray(), replaceFileExtension(originalName, ".txt")));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String replaceFileExtension(String fileName, String newExtension) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return fileName + newExtension;
		}
		return fileName.substring(0, dotIndex) + newExtension;
	}

	public void extractImagesFromPdf() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();) {
			String originalName = uploadedFile.getFileName();
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);
			Path tempDir = Files.createTempDirectory(TEMP_ZIP_FILE_NAME);
			int imageCount = 1;
			int pageCount = 1;

			for (Page page : pdfDocument.getPages()) {
				ImagePlacementAbsorber imageAbsorber = new ImagePlacementAbsorber();
				page.accept(imageAbsorber);

				for (ImagePlacement ip : imageAbsorber.getImagePlacements()) {
					XImage image = ip.getImage();

					try (ByteArrayOutputStream imageStream = new ByteArrayOutputStream()) {
						image.save(imageStream, ImageFormat.Png);
						Path imageFile = tempDir.resolve(String.format(IMAGE_NAME_PATTERN,
								StringUtils.substringBeforeLast(originalName, DOT), pageCount, imageCount));
						Files.write(imageFile, imageStream.toByteArray());
						imageCount++;
					}
				}
				pageCount++;
			}

			byte[] zipBytes = Files.readAllBytes(zipDirectory(tempDir, TEMP_ZIP_FILE_NAME));
			setFileForDownload(buildFileStream(zipBytes, updateImageZipName(originalName)));

			pdfDocument.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void convertPdfToImagesZip(com.aspose.pdf.Document pdfDocument, String originalFileName, String extention)
			throws IOException {
		Path tempDir = Files.createTempDirectory(TEMP_ZIP_FILE_NAME);

		int pageCount = 1;
		for (Page pdfPage : pdfDocument.getPages()) {
			JpegDevice jpegDevice = new JpegDevice();

			try (ByteArrayOutputStream imageStream = new ByteArrayOutputStream()) {
				jpegDevice.process(pdfPage, imageStream);

				Path imageFile = tempDir.resolve(String.format(SPLIT_PAGE_NAME_PATTERN + extention,
						StringUtils.substringBeforeLast(originalFileName, DOT), pageCount));
				Files.write(imageFile, imageStream.toByteArray());
			}

			pageCount++;
		}
		byte[] zipBytes = Files.readAllBytes(zipDirectory(tempDir, TEMP_ZIP_FILE_NAME));
		setFileForDownload(buildFileStream(zipBytes, updateImageZipName(originalFileName)));
		pdfDocument.close();
	}

	public void convertPdfToOtherDocumentTypes() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			String orginalFileName = uploadedFile.getFileName();
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			if (FileExtension.DOCX == getSelectedFileExtension()) {
				pdfDocument.save(output, com.aspose.pdf.SaveFormat.DocX);
			} else if (FileExtension.XLSX == getSelectedFileExtension()) {
				pdfDocument.save(output, com.aspose.pdf.SaveFormat.Excel);
			} else if (FileExtension.PPTX == getSelectedFileExtension()) {
				pdfDocument.save(output, com.aspose.pdf.SaveFormat.Pptx);
			} else if (FileExtension.JPG == getSelectedFileExtension()) {
				convertPdfToImagesZip(pdfDocument, orginalFileName, FileExtension.JPG.getExtension());
				return;
			} else if (FileExtension.JPEG == getSelectedFileExtension()) {
				convertPdfToImagesZip(pdfDocument, orginalFileName, FileExtension.JPEG.getExtension());
				return;
			}
			pdfDocument.close();
			setFileForDownload(buildFileStream(output.toByteArray(),
					updateFileWithNewExtension(orginalFileName, getSelectedFileExtension())));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void splitAndDownloadZipPdf() {
		if (uploadedFile == null) {
			return;
		}
		String originalName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream()) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			if (SplitOption.ALL.equals(splitOption)) {
				Path tempDir = Files.createTempDirectory(TEMP_ZIP_FILE_NAME);
				int pageCount = 1;

				for (Page pdfPage : pdfDocument.getPages()) {
					com.aspose.pdf.Document newDoc = new com.aspose.pdf.Document();
					newDoc.getPages().add(pdfPage);

					Path pageFile = tempDir
							.resolve(String.format(SPLIT_PAGE_NAME_PATTERN + FileExtension.PDF.getExtension(),
									StringUtils.substringBeforeLast(originalName, DOT), pageCount));
					newDoc.save(pageFile.toString());
					newDoc.close();
					pageCount++;
				}
				setFileForDownload(buildFileStream(Files.readAllBytes(zipDirectory(tempDir, TEMP_ZIP_FILE_NAME)),
						updateFileWithZipExtension(originalName)));
			} else {
				handleSplitByRange(pdfDocument, originalName);
			}
			pdfDocument.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleSplitByRange(com.aspose.pdf.Document pdfDocument, String originalName) throws IOException {
		int pageSize = pdfDocument.getPages().size();
		if (isInputInvalid(getStartPage(), getEndPage(), pageSize)) {
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
			setFileForDownload(buildFileStream(output.toByteArray(),
					updateRangeSplitFileWithZipExtension(originalName, getStartPage(), getEndPage())));
		}
	}

	private Path zipDirectory(Path directory, String prefix) throws IOException {
		Path zipPath = Files.createTempFile(prefix, FileExtension.ZIP.getExtension());

		try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
				ZipOutputStream zos = new ZipOutputStream(fos)) {

			Files.list(directory).forEach(path -> {
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
		return zipPath;
	}

	public void convertImageToPdf() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			String originalName = uploadedFile.getFileName();
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
			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithPdfExtension(originalName)));
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
			setFileForDownload(buildFileStream(output.toByteArray(), MERGED_DOCUMENT_NAME));
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
				text.getTextState().setFontSize(DEFAULT_FONT_SIZE);
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
			setFileForDownload(
					buildFileStream(output.toByteArray(), updateFileWithPdfExtension(uploadedFile.getFileName())));
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
			stamp.getTextState().setFontSize(DEFAULT_WATERMARK_FONT_SIZE);
			stamp.getTextState().setFontStyle(FontStyles.Bold);
			stamp.getTextState().setForegroundColor(Color.getLightGray());
			stamp.setOpacity(WATERMARK_OPACITY);

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

	private String updateFileWithPdfExtension(String originalFileName) {
		String baseName = StringUtils.isNotBlank(originalFileName)
				? StringUtils.substringBeforeLast(originalFileName, DOT)
				: "PDF";
		return baseName + FileExtension.PDF.getExtension();
	}

	private String updateFileWithZipExtension(String originalFileName) {
		String baseName = StringUtils.isNotBlank(originalFileName)
				? StringUtils.substringBeforeLast(originalFileName, DOT)
				: "zipped";
		return String.format(SPLIT_PAGE_ZIP_NAME_PATTERN, baseName);
	}

	private String updateRangeSplitFileWithZipExtension(String originalFileName, int startPage, int endPage) {
		String baseName = StringUtils.isNotBlank(originalFileName)
				? StringUtils.substringBeforeLast(originalFileName, DOT)
				: "split_zipped";
		return String.format(RANGE_SPLIT_FILE_NAME_PATTERN, baseName, startPage, endPage);
	}

	private String updateImageZipName(String originalFileName) {
		String baseName = StringUtils.isNotBlank(originalFileName)
				? StringUtils.substringBeforeLast(originalFileName, DOT)
				: "converted";
		return String.format(IMAGE_ZIP_NAME_PATTERN, baseName);
	}

	private String updateFileWithNewExtension(String originalFileName, FileExtension fileExtension) {
		String baseName = StringUtils.isNotBlank(originalFileName)
				? StringUtils.substringBeforeLast(originalFileName, DOT)
				: "converted";
		return baseName + fileExtension.getExtension();
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

	private DefaultStreamedContent buildFileStream(byte[] byteContent, String fileName) {
		return DefaultStreamedContent.builder().name(fileName).contentType(PDF_CONTENT_TYPE)
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

	public DefaultStreamedContent getFileForDownload() {
		return fileForDownload;
	}

	public void setFileForDownload(DefaultStreamedContent fileForDownload) {
		this.fileForDownload = fileForDownload;
	}

	public List<FileExtension> getOtherDocumentTypes() {
		return otherDocumentTypes;
	}

	public void setOtherDocumentTypes(List<FileExtension> otherDocumentTypes) {
		this.otherDocumentTypes = otherDocumentTypes;
	}

	public FileExtension getSelectedFileExtension() {
		return selectedFileExtension;
	}

	public void setSelectedFileExtension(FileExtension selectedFileExtension) {
		this.selectedFileExtension = selectedFileExtension;
	}

	public TextExtractedType getTextExtractedType() {
		return textExtractedType;
	}

	public void setTextExtractedType(TextExtractedType textExtractedType) {
		this.textExtractedType = textExtractedType;
	}
}
