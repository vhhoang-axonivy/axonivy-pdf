package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.awt.image.BufferedImage;
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
import javax.imageio.ImageIO;

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
import com.aspose.pdf.Image;
import com.aspose.pdf.ImageFormat;
import com.aspose.pdf.ImagePlacement;
import com.aspose.pdf.ImagePlacementAbsorber;
import com.aspose.pdf.MarginInfo;
import com.aspose.pdf.Page;
import com.aspose.pdf.PageNumberStamp;
import com.aspose.pdf.Rotation;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentCollection;
import com.aspose.pdf.TextStamp;
import com.aspose.pdf.VerticalAlignment;
import com.aspose.pdf.WatermarkArtifact;
import com.aspose.pdf.XImage;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.facades.EncodingType;
import com.aspose.pdf.facades.FontStyle;
import com.aspose.pdf.facades.FormattedText;
import com.aspose.pdf.facades.PdfFileEditor;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.axonivy.utils.axonivypdf.demo.enums.FileExtension;
import com.axonivy.utils.axonivypdf.demo.enums.SplitOption;
import com.axonivy.utils.axonivypdf.demo.enums.TextExtractType;
import com.axonivy.utils.axonivypdf.service.PdfFactory;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private static final String DOT = ".";
	private static final float DEFAULT_FONT_SIZE = 12;
	private static final float DEFAULT_PAGE_NUMBER_FONT_SIZE = 14.0F;
	private static final float DEFAULT_WATERMARK_FONT_SIZE = 72.0F;
	private static final double DEFAULT_WATERMARK_OPACITY = 0.5;
	private static final double DEFAULT_WATERMARK_ROTATION = 45;
	private static final String EXTRACTED_TEXT = "extracted_text";
	private static final String EXTRACTED_HIGHLIGHTED_TEXT = "extracted_highlighted_text";
	private static final String TIMES_NEW_ROMAN_FONT = "Times New Roman";
	private static final String TEMP_ZIP_FILE_NAME = "split_pages";
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	private static final String SAMPLE_WATERMARK = "ASPOSE_WATERMARK";
	private static final String SPLIT_PAGE_NAME_PATTERN = "%s_page_%d";
	private static final String ROTATED_DOCUMENT_NAME_PATTERN = "%s_rotated" + FileExtension.PDF.getExtension();
	private static final String DOCUMENT_WITH_HEADER_NAME_PATTERN = "%s_with_header" + FileExtension.PDF.getExtension();
	private static final String DOCUMENT_WITH_FOOTER_NAME_PATTERN = "%s_with_footer" + FileExtension.PDF.getExtension();
	private static final String DOCUMENT_WITH_PAGE_NUMBER_NAME_PATTERN = "%s_numbered"
			+ FileExtension.PDF.getExtension();
	private static final String TXT_FILE_NAME_PATTERN = "%s_%s" + FileExtension.TXT.getExtension();
	private static final String MERGED_DOCUMENT_NAME = "merged_document" + FileExtension.PDF.getExtension();
	private static final String IMAGE_NAME_PATTERN = "%s_page_%d_image_%d" + FileExtension.PNG.getExtension();
	private static final String IMAGE_ZIP_NAME_PATTERN = "%s_images_zipped" + FileExtension.ZIP.getExtension();
	private static final String SPLIT_PAGE_ZIP_NAME_PATTERN = "%s_split_zipped" + FileExtension.ZIP.getExtension();
	private static final String RANGE_SPLIT_FILE_NAME_PATTERN = "%s_page_%d_to_%d" + FileExtension.PDF.getExtension();
	private static final String FILE_NAME_WITH_WATERMARK_PATTERN = "%s_with_watermark"
			+ FileExtension.PDF.getExtension();
	private SplitOption splitOption = SplitOption.ALL;
	private TextExtractType textExtractType = TextExtractType.ALL;
	private Integer startPage;
	private Integer endPage;
	private String headerText;
	private String footerText;
	private String watermarkText = SAMPLE_WATERMARK;
	private UploadedFile uploadedFile;
	private UploadedFiles uploadedFiles;
	private DefaultStreamedContent fileForDownload;
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

	public void addHeader() {
		if (uploadedFile == null || StringUtils.isBlank(headerText)) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			TextStamp textStamp = new TextStamp(headerText);
			textStamp.setTopMargin(10);
			textStamp.setHorizontalAlignment(HorizontalAlignment.Center);
			textStamp.setVerticalAlignment(VerticalAlignment.Top);

			for (Page page : pdfDocument.getPages()) {
				page.addStamp(textStamp);
			}
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithHeaderName(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addFooter() {
		if (uploadedFile == null || StringUtils.isBlank(footerText)) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			TextStamp textStamp = new TextStamp(footerText);
			textStamp.setBottomMargin(10);
			textStamp.setHorizontalAlignment(HorizontalAlignment.Center);
			textStamp.setVerticalAlignment(VerticalAlignment.Bottom);

			for (Page page : pdfDocument.getPages()) {
				page.addStamp(textStamp);
			}
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithFooterName(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addWatermark() {
		if (uploadedFile == null) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			FormattedText formattedText = new FormattedText(watermarkText, java.awt.Color.BLUE, FontStyle.TimesRoman,
					EncodingType.Identity_h, true, DEFAULT_WATERMARK_FONT_SIZE);
			WatermarkArtifact artifact = new WatermarkArtifact();
			artifact.setText(formattedText);
			artifact.setArtifactHorizontalAlignment(HorizontalAlignment.Center);
			artifact.setArtifactVerticalAlignment(VerticalAlignment.Center);
			artifact.setRotation(DEFAULT_WATERMARK_ROTATION);
			artifact.setOpacity(DEFAULT_WATERMARK_OPACITY);
			artifact.setBackground(false);

			for (Page page : pdfDocument.getPages()) {
				page.getArtifacts().add(artifact);
			}
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateFileNameWithWatermark(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void rotatePages() {
		if (uploadedFile == null) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();) {

			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			for (Page page : pdfDocument.getPages()) {
				page.setRotate(Rotation.on90);
			}
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateRotatedFileName(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addPageNumbers() {
		if (uploadedFile == null) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();) {

			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

			PageNumberStamp pageNumberStamp = new PageNumberStamp();
			pageNumberStamp.setBackground(false);
			pageNumberStamp.setFormat("Page # of " + pdfDocument.getPages().size());
			pageNumberStamp.setBottomMargin(10);
			pageNumberStamp.setHorizontalAlignment(HorizontalAlignment.Center);
			pageNumberStamp.setStartingNumber(1);

			pageNumberStamp.getTextState().setFont(FontRepository.findFont(TIMES_NEW_ROMAN_FONT));
			pageNumberStamp.getTextState().setFontSize(DEFAULT_PAGE_NUMBER_FONT_SIZE);
			pageNumberStamp.getTextState().setFontStyle(FontStyles.Bold);
			pageNumberStamp.getTextState().setForegroundColor(Color.getBlack());

			for (Page page : pdfDocument.getPages()) {
				page.addStamp(pageNumberStamp);
			}
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithPageNumberName(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void extractHighlightedText(String originalFileName, InputStream input, ByteArrayOutputStream textStream,
			OutputStreamWriter writer) throws IOException {
		com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);

		StringBuilder highlightedText = new StringBuilder();

		for (Page page : pdfDocument.getPages()) {
			for (Annotation annotation : page.getAnnotations()) {
				if (annotation instanceof HighlightAnnotation) {
					HighlightAnnotation highlight = (HighlightAnnotation) annotation;

					TextFragmentCollection fragments = highlight.getMarkedTextFragments();
					for (TextFragment tf : fragments) {
						highlightedText.append(tf.getText()).append(System.lineSeparator());
					}
				}
			}
		}

		writer.write(highlightedText.toString());
		writer.flush();

		pdfDocument.close();

		setFileForDownload(buildFileStream(textStream.toByteArray(), updateTxtFileName(originalFileName)));
	}

	public void extractAllText(String originalFileName, InputStream input, ByteArrayOutputStream textStream,
			OutputStreamWriter writer) throws IOException {
		com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(input);
		com.aspose.pdf.TextAbsorber textAbsorber = new com.aspose.pdf.TextAbsorber();

		pdfDocument.getPages().accept(textAbsorber);

		String extractedText = textAbsorber.getText();

		writer.write(extractedText);
		writer.flush();

		pdfDocument.close();

		setFileForDownload(buildFileStream(textStream.toByteArray(), updateTxtFileName(originalFileName)));
	}

	public void extractTextFromPdf() {
		if (uploadedFile == null) {
			return;
		}

		String originalFileName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream textStream = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(textStream, StandardCharsets.UTF_8)) {
			if (TextExtractType.ALL.equals(textExtractType)) {
				extractAllText(originalFileName, input, textStream, writer);
			} else {
				extractHighlightedText(originalFileName, input, textStream, writer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void extractImagesFromPdf() {
		if (uploadedFile == null) {
			return;
		}

		try (InputStream input = uploadedFile.getInputStream();) {
			String originalFileName = uploadedFile.getFileName();
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
								StringUtils.substringBeforeLast(originalFileName, DOT), pageCount, imageCount));
						Files.write(imageFile, imageStream.toByteArray());
						imageCount++;
					}
				}
				pageCount++;
			}

			byte[] zipBytes = Files.readAllBytes(zipDirectory(tempDir, TEMP_ZIP_FILE_NAME));
			setFileForDownload(buildFileStream(zipBytes, updateImageZipName(originalFileName)));

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

		String orginalFileName = uploadedFile.getFileName();
		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
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
		String originalFileName = uploadedFile.getFileName();
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
									StringUtils.substringBeforeLast(originalFileName, DOT), pageCount));
					newDoc.save(pageFile.toString());
					newDoc.close();
					pageCount++;
				}
				setFileForDownload(buildFileStream(Files.readAllBytes(zipDirectory(tempDir, TEMP_ZIP_FILE_NAME)),
						updateFileWithZipExtension(originalFileName)));
			} else {
				handleSplitByRange(pdfDocument, originalFileName);
			}
			pdfDocument.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleSplitByRange(com.aspose.pdf.Document pdfDocument, String originalFileName) throws IOException {
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
					updateRangeSplitFileWithZipExtension(originalFileName, getStartPage(), getEndPage())));
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
		String originalFileName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(uploadedFile.getContent()));
			int widthPx = bufferedImage.getWidth();
			int heightPx = bufferedImage.getHeight();

			com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document();
			Page page = pdfDocument.getPages().add();
			page.getPageInfo().setWidth(widthPx);
			page.getPageInfo().setHeight(heightPx);
			page.getPageInfo().setMargin(new MarginInfo(0, 0, 0, 0));

			Image image = new Image();
			image.setImageStream(uploadedFile.getInputStream());
			page.getParagraphs().add(image);
			pdfDocument.save(output);
			pdfDocument.close();

			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithPdfExtension(originalFileName)));
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
		String originalFileName = uploadedFile.getFileName();

		try (InputStream input = uploadedFile.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			String fileName = originalFileName.toLowerCase();

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
			setFileForDownload(buildFileStream(output.toByteArray(), updateFileWithPdfExtension(originalFileName)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getBaseName(String originalFileName, String substitudeName) {
		return StringUtils.isNotBlank(originalFileName) ? StringUtils.substringBeforeLast(originalFileName, DOT)
				: substitudeName;
	}

	private String updateFileWithPdfExtension(String originalFileName) {
		return getBaseName(originalFileName, "pdf") + FileExtension.PDF.getExtension();
	}

	private String updateFileWithZipExtension(String originalFileName) {
		return String.format(SPLIT_PAGE_ZIP_NAME_PATTERN, getBaseName(originalFileName, "zip"));
	}

	private String updateRangeSplitFileWithZipExtension(String originalFileName, int startPage, int endPage) {
		return String.format(RANGE_SPLIT_FILE_NAME_PATTERN, getBaseName(originalFileName, "split_zip"), startPage,
				endPage);
	}

	private String updateFileNameWithWatermark(String originalFileName) {
		return String.format(FILE_NAME_WITH_WATERMARK_PATTERN, getBaseName(originalFileName, "pdf_with_watermark"));
	}

	private String updateImageZipName(String originalFileName) {
		return String.format(IMAGE_ZIP_NAME_PATTERN, getBaseName(originalFileName, "images_zip"));
	}

	private String updateRotatedFileName(String originalFileName) {
		return String.format(ROTATED_DOCUMENT_NAME_PATTERN, getBaseName(originalFileName, "rotated"));
	}

	private String updateFileWithNewExtension(String originalFileName, FileExtension fileExtension) {
		return getBaseName(originalFileName, "converted") + fileExtension.getExtension();
	}

	private String updateFileWithPageNumberName(String originalFileName) {
		return String.format(DOCUMENT_WITH_PAGE_NUMBER_NAME_PATTERN, getBaseName(originalFileName, "numbered"));
	}

	private String updateFileWithHeaderName(String originalFileName) {
		return String.format(DOCUMENT_WITH_HEADER_NAME_PATTERN, getBaseName(originalFileName, "with_header"));
	}

	private String updateFileWithFooterName(String originalFileName) {
		return String.format(DOCUMENT_WITH_FOOTER_NAME_PATTERN, getBaseName(originalFileName, "with_footerer"));
	}

	private String updateTxtFileName(String originalFileName) {
		if (TextExtractType.ALL.equals(textExtractType)) {
			return String.format(TXT_FILE_NAME_PATTERN, getBaseName(originalFileName, EXTRACTED_TEXT), EXTRACTED_TEXT);
		}
		return String.format(TXT_FILE_NAME_PATTERN, getBaseName(originalFileName, EXTRACTED_HIGHLIGHTED_TEXT),
				EXTRACTED_HIGHLIGHTED_TEXT);
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

	public TextExtractType getTextExtractType() {
		return textExtractType;
	}

	public void setTextExtractType(TextExtractType textExtractType) {
		this.textExtractType = textExtractType;
	}

	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public String getFooterText() {
		return footerText;
	}

	public void setFooterText(String footerText) {
		this.footerText = footerText;
	}

	public String getWatermarkText() {
		return watermarkText;
	}

	public void setWatermarkText(String watermarkText) {
		this.watermarkText = watermarkText;
	}
}
