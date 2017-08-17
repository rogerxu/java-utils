package org.demo.java.utils;

import org.testng.annotations.*;

public class FileDownloaderTest {
	private String downloadUrl = "http://www.sample-videos.com/text/Sample-text-file-10kb.txt";

	@BeforeClass
	public void setUp() {
		// code that will be invoked when this test is instantiated

	}

	@Test(groups = {"direct"})
	public void downloadDirect() throws Exception {
		FileDownloader downloader = FileDownloader.getInstance();
		downloader.download(downloadUrl, "target/node/test.txt");
	}

	@Test(groups = {"proxy"})
	public void downloadViaProxy() throws Exception {
		// set proxy
		System.setProperty("http.proxyHost", "localhost");
		System.setProperty("http.proxyPort", "1984");

		FileDownloader downloader = FileDownloader.getInstance();
		downloader.download(downloadUrl, "target/node/test.txt");
	}
}
