package com.axonivy.utils.axonivypdf.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.aspose.pdf.License;
import com.axonivy.utils.axonivypdf.service.PdfFactory;

import ch.ivyteam.ivy.ThirdPartyLicenses;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class PdfFactoryTest {
	@BeforeEach
	void resetLicenseField() throws Exception {
		var field = PdfFactory.class.getDeclaredField("license");
		field.setAccessible(true);
		field.set(null, null);
	}

	@Test
	void testLoadLicenseWithoutRealAsposeFile() throws Exception {
		withMockedLicense((stream, mockedLicenses, mockedLicenseConstructor) -> {
			PdfFactory.loadLicense();
			PdfFactory.loadLicense();
			mockedLicenses.verify(ThirdPartyLicenses::getDocumentFactoryLicense);
			assertEquals(1, mockedLicenseConstructor.constructed().size());
			verify(mockedLicenseConstructor.constructed().get(0)).setLicense(stream);
		});
	}

	@Test
	void testGetCallsLoadLicenseAndReturnsValue() throws Exception {
		withMockedLicense((stream, mockedThirdParty, mockedLicenseConstructor) -> {
			Supplier<String> supplier = () -> "HelloWorld";
			String result = PdfFactory.get(supplier);
			assertEquals("HelloWorld", result);
		});
	}

	@Test
	void testRunCallsLoadLicenseAndExecutesRunnable() throws Exception {
		withMockedLicense((stream, mockedThirdParty, mockedLicenseConstructor) -> {
			final boolean[] executed = { false };
			Runnable runnable = () -> executed[0] = true;
			PdfFactory.run(runnable);
			assertTrue(executed[0]);
		});
	}

	@FunctionalInterface
	private interface TestLogic {
		void run(InputStream stream, MockedStatic<ThirdPartyLicenses> mockedThirdParty,
				MockedConstruction<License> mockedLicenseCtor) throws Exception;
	}

	@SuppressWarnings("resource")
	private void withMockedLicense(TestLogic logic) throws Exception {
		try (MockedStatic<ThirdPartyLicenses> mockedThirdParty = Mockito.mockStatic(ThirdPartyLicenses.class)) {
			InputStream dummyStream = mock(InputStream.class);
//			InputStream dummyStream = new ByteArrayInputStream("<License></License>".getBytes(StandardCharsets.UTF_8));
			mockedThirdParty.when(ThirdPartyLicenses::getDocumentFactoryLicense).thenReturn(dummyStream);
			try (MockedConstruction<License> mockedLicenseConstructor = Mockito.mockConstruction(License.class,
					(mock, context) -> doNothing().when(mock).setLicense(any(InputStream.class)))) {
				logic.run(dummyStream, mockedThirdParty, mockedLicenseConstructor);
			}
		}
	}
}
