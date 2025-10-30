package com.axonivy.utils.axonivypdf.service;

import java.io.InputStream;

import com.aspose.pdf.License;

import ch.ivyteam.ivy.ThirdPartyLicenses;
import ch.ivyteam.ivy.environment.Ivy;

public class PdfFactory {
	private static License license;

	private PdfFactory() {
	}

	static {
		loadLicense();
	}

	/**
	 * Initializes the Aspose DocumentFactory license.
	 * <p>
	 * Ensures the license is loaded once per request. If not already set, this
	 * method retrieves the license from {@link ThirdPartyLicenses} and applies it
	 * to the Aspose {@link License} instance.
	 * </p>
	 *
	 * <p>
	 * In case of failure, the exception is logged and the license reference is
	 * reset to {@code null}, leaving the application in evaluation mode.
	 * </p>
	 */
	public static void loadLicense() {
		if (license != null) {
			return;
		}
		try {
			InputStream in = ThirdPartyLicenses.getDocumentFactoryLicense();
			if (in != null) {
				license = new License();
				license.setLicense(in);
			}
		} catch (Exception e) {
			Ivy.log().error(e);
			license = null;
		}
	}
}
