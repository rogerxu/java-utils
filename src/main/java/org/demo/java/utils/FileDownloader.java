package org.demo.java.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;

public class FileDownloader {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloader.class);

	private static FileDownloader instance;

	public static FileDownloader getInstance() {
		if (instance == null) {
			instance = new FileDownloader();
		}

		return instance;
	}

	public void download(String downloadUrl, String destination) throws Exception {
		String fixedDownloadUrl = downloadUrl;
		try {
			fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
			URI downloadURI = new URI(fixedDownloadUrl);

			if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
				// copy file
				FileUtils.copyFile(new File(downloadURI), new File(destination));
			} else {
				CloseableHttpResponse response = execute(fixedDownloadUrl);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					throw new Exception("Got error code " + statusCode + " from the server.");
				}

				// create directory for destination
				new File(FilenameUtils.getFullPathNoEndSeparator(destination)).mkdirs();

				// download file
				ReadableByteChannel rbc = Channels.newChannel(response.getEntity().getContent());
				FileOutputStream fos = new FileOutputStream(destination);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				LOGGER.info(MessageFormat.format("Downloaded to {0}", new File(destination).getAbsolutePath()));
			}
		} catch (IOException e) {
			throw new Exception("Could not download " + fixedDownloadUrl, e);
		} catch (URISyntaxException e) {
			throw new Exception("Could not download " + fixedDownloadUrl, e);
		}
	}

	private CloseableHttpResponse execute(String requestUrl) throws IOException {
		Proxy proxy = getProxy();
		if (proxy == Proxy.NO_PROXY) {
			LOGGER.info("No proxy was configured, downloading directly");
		} else {
			LOGGER.info("Downloading via proxy " + proxy.toString());
		}

		final CloseableHttpClient httpClient = buildHttpClient();
		final HttpGet request = new HttpGet(requestUrl);
		LOGGER.info(MessageFormat.format("Downloading {0}", requestUrl));
		CloseableHttpResponse response = httpClient.execute(request);

		return response;
	}

	private CloseableHttpResponse executeViaProxy(String requestUrl, Proxy proxy) throws IOException {
		final CloseableHttpClient httpClient = buildHttpClient();

		final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
		final HttpHost proxyHttpHost = new HttpHost(proxyAddress.getHostName(), proxyAddress.getPort(), proxy.type().name());
		final RequestConfig requestConfig = RequestConfig.custom()
			.setProxy(proxyHttpHost)
			.build();

		final HttpGet request = new HttpGet(requestUrl);
		request.setConfig(requestConfig);
		LOGGER.info(MessageFormat.format("Downloading {0}", requestUrl));

		return httpClient.execute(request);
	}

	private CloseableHttpClient buildHttpClient() {
		return HttpClients.createSystem();
	}

	private Proxy getProxy() {
		Proxy proxy = Proxy.NO_PROXY;

		String host = System.getProperty("http.proxyHost");
		String port = System.getProperty("http.proxyPort");

		if (host != null && port != null) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.parseInt(port)));
		}

		return proxy;
	}
}
