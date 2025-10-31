package com.axonivy.utils.axonivypdf.demo.managedBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

@ManagedBean
@ViewScoped
public class PdfFactoryBean {
	private UploadedFiles files;
	private DefaultStreamedContent mergedFile;

	public void uploadMultiple() {
		if (files != null) {
			try {
				File merged = File.createTempFile("merged-", ".pdf");
				PDFMergerUtility merger = new PDFMergerUtility();

				for (UploadedFile uf : files.getFiles()) {
					File temp = File.createTempFile("upload-", ".pdf");
					try (InputStream in = uf.getInputStream(); OutputStream out = new FileOutputStream(temp)) {
						in.transferTo(out);
					}
					merger.addSource(temp);
				}

				merger.setDestinationFileName(merged.getAbsolutePath());
				merger.mergeDocuments(null);

				InputStream mergedStream = new FileInputStream(merged);
				setMergedFile(DefaultStreamedContent.builder().name("combined.pdf").contentType("application/pdf")
						.stream(() -> mergedStream).build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public UploadedFiles getFiles() {
		return files;
	}

	public void setFiles(UploadedFiles files) {
		this.files = files;
	}

	public void setMergedFile(DefaultStreamedContent mergedFile) {
		this.mergedFile = mergedFile;
	}

	public DefaultStreamedContent getMergedFile() {
		return mergedFile;
	}
}
