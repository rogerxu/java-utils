package org.demo.java.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class NodeInstaller {
	public static final String INSTALL_PATH = "/node";

	public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

	private static final Object LOCK = new Object();

	private String npmVersion, nodeVersion, nodeDownloadRoot, userName, password;

	private final Logger logger;

//	private final InstallConfig config;

//	private final ArchiveExtractor archiveExtractor;

	private final FileDownloader fileDownloader;

//	NodeInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
	NodeInstaller(FileDownloader fileDownloader) {
		this.logger = LoggerFactory.getLogger(getClass());
//		this.config = config;
//		this.archiveExtractor = archiveExtractor;
		this.fileDownloader = fileDownloader;
	}

	public NodeInstaller setNodeVersion(String nodeVersion) {
		this.nodeVersion = nodeVersion;
		return this;
	}

	public NodeInstaller setNodeDownloadRoot(String nodeDownloadRoot) {
		this.nodeDownloadRoot = nodeDownloadRoot;
		return this;
	}

	public NodeInstaller setNpmVersion(String npmVersion) {
		this.npmVersion = npmVersion;
		return this;
	}

	public void install() throws InstallationException {
		// use static lock object for a synchronized block
		synchronized (LOCK) {
			if (this.nodeDownloadRoot == null || this.nodeDownloadRoot.isEmpty()) {
				this.nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
			}
			if (!nodeIsAlreadyInstalled()) {
				this.logger.info("Installing node version {}", this.nodeVersion);
				if (!this.nodeVersion.startsWith("v")) {
					this.logger.warn("Node version does not start with naming convention 'v'.");
				}
//				if (this.config.getPlatform().isWindows()) {
				if (true) {
					installNodeForWindows();
				} else {
					installNodeDefault();
				}
			}
		}
	}

	private boolean nodeIsAlreadyInstalled() {
		return false;
//		try {
//			NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
//			File nodeFile = executorConfig.getNodePath();
//			if (nodeFile.exists()) {
//				final String version =
//					new NodeExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger);
//
//				if (version.equals(this.nodeVersion)) {
//					this.logger.info("Node {} is already installed.", version);
//					return true;
//				} else {
//					this.logger.info("Node {} was installed, but we need version {}", version,
//						this.nodeVersion);
//					return false;
//				}
//			} else {
//				return false;
//			}
//		} catch (ProcessExecutionException e) {
//			return false;
//		}
	}

	private void installNodeDefault() throws InstallationException {
		try {
			final String longNodeFilename =
				this.config.getPlatform().getLongNodeFilename(this.nodeVersion, false);
			String downloadUrl = this.nodeDownloadRoot
				+ this.config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
			String classifier = this.config.getPlatform().getNodeClassifier();

			File tmpDirectory = getTempDirectory();

			CacheDescriptor cacheDescriptor = new CacheDescriptor("node", this.nodeVersion, classifier,
				this.config.getPlatform().getArchiveExtension());

			File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

			downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

			extractFile(archive, tmpDirectory);

			// Search for the node binary
			File nodeBinary =
				new File(tmpDirectory, longNodeFilename + File.separator + "bin" + File.separator + "node");
			if (!nodeBinary.exists()) {
				throw new FileNotFoundException(
					"Could not find the downloaded Node.js binary in " + nodeBinary);
			} else {
				File destinationDirectory = getInstallDirectory();

				File destination = new File(destinationDirectory, "node");
				this.logger.info("Copying node binary from {} to {}", nodeBinary, destination);
				if (!nodeBinary.renameTo(destination)) {
					throw new InstallationException("Could not install Node: Was not allowed to rename "
						+ nodeBinary + " to " + destination);
				}

				if (!destination.setExecutable(true, false)) {
					throw new InstallationException(
						"Could not install Node: Was not allowed to make " + destination + " executable.");
				}

				deleteTempDirectory(tmpDirectory);

				this.logger.info("Installed node locally.");
			}
		} catch (IOException e) {
			throw new InstallationException("Could not install Node", e);
		} catch (DownloadException e) {
			throw new InstallationException("Could not download Node.js", e);
		} catch (ArchiveExtractionException e) {
			throw new InstallationException("Could not extract the Node archive", e);
		}
	}

	private void installNodeForWindows() throws InstallationException {
		final String downloadUrl = this.nodeDownloadRoot
			+ this.config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
		try {
			File destinationDirectory = getInstallDirectory();

			File destination = new File(destinationDirectory, "node.exe");

			String classifier = this.config.getPlatform().getNodeClassifier();

			CacheDescriptor cacheDescriptor =
				new CacheDescriptor("node", this.nodeVersion, classifier, "exe");

			File binary = this.config.getCacheResolver().resolve(cacheDescriptor);

			downloadFileIfMissing(downloadUrl, binary, this.userName, this.password);

			this.logger.info("Copying node binary from {} to {}", binary, destination);
			FileUtils.copyFile(binary, destination);

			this.logger.info("Installed node locally.");
		} catch (DownloadException e) {
			throw new InstallationException("Could not download Node.js from: " + downloadUrl, e);
		} catch (IOException e) {
			throw new InstallationException("Could not install Node.js", e);
		}
	}

	private File getTempDirectory() {
		File tmpDirectory = new File(getInstallDirectory(), "tmp");
		if (!tmpDirectory.exists()) {
			this.logger.debug("Creating temporary directory {}", tmpDirectory);
			tmpDirectory.mkdirs();
		}
		return tmpDirectory;
	}

	private File getInstallDirectory() {
		File installDirectory = new File(this.config.getInstallDirectory(), INSTALL_PATH);
		if (!installDirectory.exists()) {
			this.logger.debug("Creating install directory {}", installDirectory);
			installDirectory.mkdirs();
		}
		return installDirectory;
	}

	private void deleteTempDirectory(File tmpDirectory) throws IOException {
		if (tmpDirectory != null && tmpDirectory.exists()) {
			this.logger.debug("Deleting temporary directory {}", tmpDirectory);
			FileUtils.deleteDirectory(tmpDirectory);
		}
	}

	private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
		this.logger.info("Unpacking {} into {}", archive, destinationDirectory);
		this.archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
	}

	private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password) throws Exception {
		if (!destination.exists()) {
			downloadFile(downloadUrl, destination);
		}
	}

	private void downloadFile(String downloadUrl, File destination) throws Exception {
		this.logger.info("Downloading {} to {}", downloadUrl, destination);
		this.fileDownloader.download(downloadUrl, destination.getPath());
	}
}