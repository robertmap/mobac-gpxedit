/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapDownloadSkippedException;
import mobac.gui.AtlasProgress;
import mobac.gui.AtlasProgress.AtlasCreationController;
import mobac.mapsources.AbstractMultiLayerMapSource;
import mobac.program.atlascreators.AtlasCreator;
import mobac.program.atlascreators.tileprovider.DownloadedTileProvider;
import mobac.program.atlascreators.tileprovider.FilteredMapSourceProvider;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.download.DownloadJobProducerThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.DownloadJobListener;
import mobac.program.interfaces.DownloadableElement;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSource.LoadMethod;
import mobac.program.interfaces.MapSourceCallerThreadInfo;
import mobac.program.model.AtlasOutputFormat;
import mobac.program.model.Settings;
import mobac.program.tilestore.TileStore;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import mobac.utilities.tar.TarIndex;
import mobac.utilities.tar.TarIndexedArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

public class AtlasThread extends Thread
		implements
			DownloadJobListener,
			AtlasCreationController,
			MapSourceCallerThreadInfo {

	private static final Logger LOG = LoggerFactory.getLogger(AtlasThread.class);
	private static int threadNum = 0;
	private File customAtlasDir = null;
	private boolean quitMobacAfterAtlasCreation = false;
	private DownloadJobProducerThread downloadJobProducerThread = null;
	private JobDispatcher downloadJobDispatcher;
	private final AtlasProgress atlasProgress; // The GUI showing the progress
	private final AtlasInterface atlas;
	private AtlasCreator atlasCreator = null;
	private final PauseResumeHandler pauseResumeHandler;
	private int activeDownloads = 0;
	private int jobsCompleted = 0;
	private int jobsRetryError = 0;
	private int jobsPermanentError = 0;
	private int maxDownloadRetries = 1;

	{
		ImageIO.setUseCache(false);
	}

	public AtlasThread(AtlasInterface atlas) throws AtlasTestException {
		this(atlas, atlas.getOutputFormat().createAtlasCreatorInstance());
	}

	public AtlasThread(AtlasInterface atlas, AtlasCreator atlasCreator) throws AtlasTestException {
		super("AtlasThread " + getNextThreadNum());
		atlasProgress = new AtlasProgress(this);
		this.atlas = atlas;
		this.atlasCreator = atlasCreator;
		testAtlas();
		TileStore.getInstance().closeAll();
		maxDownloadRetries = Settings.getInstance().downloadRetryCount;
		pauseResumeHandler = new PauseResumeHandler();
	}

	private static synchronized int getNextThreadNum() {
		threadNum++;
		return threadNum;
	}

	private static long getFileBasedTileCount(MapInterface map) {
		MapSource mapSource = map.getMapSource();
		if (mapSource instanceof FileBasedMapSource) {
			return map.calculateTilesToDownload();
		}
		if (mapSource instanceof AbstractMultiLayerMapSource) {
			long result = 0;
			AbstractMultiLayerMapSource mlMapSource = (AbstractMultiLayerMapSource) mapSource;
			long tilesPerLayer = map.calculateTilesToDownload() / mlMapSource.getLayerMapSources().length;
			for (MapSource ms : mlMapSource) {
				// check all layers if they are file-based
				if (ms instanceof FileBasedMapSource) {
					result += tilesPerLayer;
				}
			}
			return result;
		}
		return 0;
	}

	private void testAtlas() throws AtlasTestException {
		try {
			for (LayerInterface layer : atlas) {
				for (MapInterface map : layer) {
					MapSource mapSource = map.getMapSource();
					if (!atlasCreator.testMapSource(mapSource)) {
						throw new AtlasTestException("The selected atlas output format \"" + atlas.getOutputFormat()
								+ "\" does not support the map source \"" + map.getMapSource() + "\"", map);
					}
				}
			}
		} catch (AtlasTestException e) {
			throw e;
		} catch (Exception e) {
			throw new AtlasTestException(e);
		}
	}

	public void run() {
		GUIExceptionHandler.registerForCurrentThread();
		LOG.info("Starting creation of {} atlas \"{}\"", atlas.getOutputFormat(), atlas.getName());
		if (customAtlasDir != null) {
			LOG.debug("Target directory: {}", customAtlasDir);
		}
		atlasProgress.setDownloadControllerListener(this);
		try {
			createAtlas();
			LOG.info("Atlas creation finished");
			if (quitMobacAfterAtlasCreation) {
				System.exit(0);
			}
		} catch (OutOfMemoryError e) {
			System.gc();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					String message = I18nUtils.localizedStringForKey("msg_out_of_memory_head");
					int maxMem = Utilities.getJavaMaxHeapMB();
					if (maxMem > 0) {
						message += String.format(I18nUtils.localizedStringForKey("msg_out_of_memory_detail"), maxMem);
					}
					JOptionPane.showMessageDialog(null, message,
							I18nUtils.localizedStringForKey("msg_out_of_memory_title"), JOptionPane.ERROR_MESSAGE);
					atlasProgress.closeWindow();
				}
			});
			LOG.error("Out of memory: ", e);
		} catch (InterruptedException e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, I18nUtils.localizedStringForKey("msg_atlas_download_abort"),
							I18nUtils.localizedStringForKey("Information"), JOptionPane.INFORMATION_MESSAGE);
					atlasProgress.closeWindow();
				}
			});
			LOG.info("Atlas creation was interrupted by user");
		} catch (Exception e) {
			LOG.error("Atlas creation aborted because of an error: ", e);
			GUIExceptionHandler.showExceptionDialog(e);
		}
		System.gc();
		if (quitMobacAfterAtlasCreation) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			System.exit(1);
		}
	}

	/**
	 * Create atlas: For each map download the tiles and perform atlas/map creation
	 */
	protected void createAtlas() throws InterruptedException, IOException {

		long totalNrOfOnlineTiles = atlas.calculateTilesToDownload();

		for (LayerInterface l : atlas) {
			for (MapInterface m : l) {
				// Offline map sources are not relevant for the maximum tile limit.
				totalNrOfOnlineTiles -= getFileBasedTileCount(m);
			}
		}

		if (totalNrOfOnlineTiles > 500000) {
			JOptionPane.showMessageDialog(null,
					String.format(I18nUtils.localizedStringForKey("msg_too_many_tiles_msg"), 500000,
							totalNrOfOnlineTiles),
					I18nUtils.localizedStringForKey("msg_too_many_tiles_title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			atlasCreator.startAtlasCreation(atlas, customAtlasDir);
		} catch (AtlasTestException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Atlas format restriction violated",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		atlasProgress.initAtlas(atlas);
		atlasProgress.setVisible(true);

		Settings s = Settings.getInstance();

		try (JobDispatcher downloadJobDispatcher = new JobDispatcher(this, s.downloadThreadCount, pauseResumeHandler,
				atlasProgress)) {
			this.downloadJobDispatcher = downloadJobDispatcher;
			for (LayerInterface layer : atlas) {
				atlasCreator.initLayerCreation(layer);
				for (MapInterface map : layer) {
					try {
						while (!createMap(map)) {
						}
					} catch (InterruptedException e) {
						throw e; // User has aborted
					} catch (MapDownloadSkippedException e) {
						// Do nothing and continue with next map
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
						String[] options = {I18nUtils.localizedStringForKey("Continue"),
								I18nUtils.localizedStringForKey("Abort"),
								I18nUtils.localizedStringForKey("dlg_download_show_error_report")};
						int a = JOptionPane.showOptionDialog(null,
								I18nUtils.localizedStringForKey("dlg_download_erro_head") + e.getMessage() + "\n["
										+ e.getClass().getSimpleName() + "]\n\n",
								I18nUtils.localizedStringForKey("Error"), 0, JOptionPane.ERROR_MESSAGE, null, options,
								options[0]);
						switch (a) {
							case 2 : // show error report
								GUIExceptionHandler.processException(e);
							case 1 : // Abort
								throw new InterruptedException();
							default : // Continue
						}
					}
				}
				atlasCreator.finishLayerCreation();
			}
		} catch (InterruptedException e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} catch (Error e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} finally {
			// In case of an abort: Stop create new download jobs
			if (downloadJobProducerThread != null) {
				downloadJobProducerThread.cancel();
			}
			this.downloadJobDispatcher = null;
			if (!atlasCreator.isAborted()) {
				atlasCreator.finishAtlasCreation();
			}
			atlasProgress.atlasCreationFinished();
		}

	}

	/**
	 * @param map
	 * @return true if map creation process was finished and false if something went
	 *         wrong and the user decided to retry map download
	 * @throws Exception
	 */
	public boolean createMap(MapInterface map) throws Exception {
		TarIndex tileIndex = null;
		TarIndexedArchive tileArchive = null;

		jobsCompleted = 0;
		jobsRetryError = 0;
		jobsPermanentError = 0;

		atlasProgress.initMapDownload(map);

		if (map.getMapSource() instanceof InitializableMapSource) {
			((InitializableMapSource) map.getMapSource()).initialize();
		}

		if (currentThread().isInterrupted()) {
			throw new InterruptedException();
		}

		// Prepare the tile store directory
		// ts.prepareTileStore(map.getMapSource());

		/***
		 * In this section of code below, tiles for Atlas is being downloaded and saved
		 * in the temporary layer tar file in the system temp directory.
		 **/
		int zoom = map.getZoom();

		final int tileCount = (int) map.calculateTilesToDownload();

		atlasProgress.setZoomLevel(zoom);
		try {
			TileProvider mapTileProvider;
			if (!(map.getMapSource() instanceof FileBasedMapSource)) {
				// For online maps we download the tiles first and then start creating the map
				// if
				// we are sure we got all tiles
				if (!AtlasOutputFormat.TILESTORE.equals(atlas.getOutputFormat())) {
					String tempSuffix = "MOBAC_" + atlas.getName() + "_" + zoom + "_";
					File tileArchiveFile = File.createTempFile(tempSuffix, ".tar", DirectoryManager.tempDir);
					// If something goes wrong the temp file only persists until the VM exits
					tileArchiveFile.deleteOnExit();
					LOG.debug("Writing downloaded tiles to {}", tileArchiveFile.getPath());
					tileArchive = new TarIndexedArchive(tileArchiveFile, tileCount);
				} else {
					LOG.debug("Downloading to tile store only");
				}

				downloadJobProducerThread = new DownloadJobProducerThread(this, downloadJobDispatcher, tileArchive,
						(DownloadableElement) map);

				boolean failedMessageAnswered = false;

				while (downloadJobProducerThread.isAlive() || (downloadJobDispatcher.getWaitingJobCount() > 0)
						|| downloadJobDispatcher.isAtLeastOneWorkerActive()) {
					Thread.sleep(500);
					if (!failedMessageAnswered && (jobsRetryError > 50) && !atlasProgress.ignoreDownloadErrors()) {
						pauseResumeHandler.pause();
						String[] answers = new String[]{I18nUtils.localizedStringForKey("Continue"),
								I18nUtils.localizedStringForKey("Retry"), I18nUtils.localizedStringForKey("Skip"),
								I18nUtils.localizedStringForKey("Abort")};
						int answer = JOptionPane.showOptionDialog(atlasProgress,
								I18nUtils.localizedStringForKey("dlg_download_errors_todo_msg"),
								I18nUtils.localizedStringForKey("dlg_download_errors_todo"), 0,
								JOptionPane.QUESTION_MESSAGE, null, answers, answers[0]);
						failedMessageAnswered = true;
						switch (answer) {
							case 0 : // Continue
								pauseResumeHandler.resume();
								break;
							case 1 : // Retry
								downloadJobProducerThread.cancel();
								downloadJobProducerThread = null;
								downloadJobDispatcher.cancelOutstandingJobs();
								return false;
							case 2 : // Skip
								downloadJobDispatcher.cancelOutstandingJobs();
								throw new MapDownloadSkippedException();
							default : // Abort or close dialog
								downloadJobDispatcher.cancelOutstandingJobs();
								downloadJobDispatcher.terminateAllWorkerThreads();
								throw new InterruptedException();
						}
					}
				}
				downloadJobProducerThread = null;
				LOG.debug("All download jobs has been completed!");
				if (tileArchive != null) {
					tileArchive.writeEndofArchive();
					tileArchive.close();
					tileIndex = tileArchive.getTarIndex();
					if (tileIndex.size() < tileCount && !atlasProgress.ignoreDownloadErrors()) {
						int missing = tileCount - tileIndex.size();
						LOG.debug("Expected tile count: {} downloaded tile count: {} missing: {}", tileCount,
								tileIndex.size(), missing);
						int answer = JOptionPane.showConfirmDialog(atlasProgress,
								String.format(I18nUtils.localizedStringForKey("dlg_download_errors_missing_tile_msg"),
										missing),
								I18nUtils.localizedStringForKey("dlg_download_errors_missing_tile"),
								JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						if (answer != JOptionPane.YES_OPTION) {
							throw new InterruptedException();
						}
					}
				}
				downloadJobDispatcher.cancelOutstandingJobs();
				LOG.debug("Starting to create atlas from downloaded tiles");
				mapTileProvider = new DownloadedTileProvider(tileIndex, map);
			} else {
				// We don't need to download anything. Everything is already stored locally
				// therefore we can just use it
				mapTileProvider = new FilteredMapSourceProvider(map, LoadMethod.DEFAULT);
			}
			atlasCreator.initializeMap(map, mapTileProvider);
			atlasCreator.createMap();
		} catch (Error e) {
			LOG.error("Error in createMap: {}", e.getMessage(), e);
			throw e;
		} finally {
			if (tileIndex != null) {
				tileIndex.closeAndDelete();
			} else if (tileArchive != null) {
				tileArchive.delete();
			}
		}
		return true;
	}

	public void pauseResumeAtlasCreation() {
		if (pauseResumeHandler.isPaused()) {
			LOG.debug("Atlas creation resumed");
			pauseResumeHandler.resume();
		} else {
			LOG.debug("Atlas creation paused");
			pauseResumeHandler.pause();
		}
	}

	public boolean isPaused() {
		return pauseResumeHandler.isPaused();
	}

	public PauseResumeHandler getPauseResumeHandler() {
		return pauseResumeHandler;
	}

	/**
	 * Stop listener from {@link AtlasProgress}
	 */
	public void abortAtlasCreation() {
		try {
			DownloadJobProducerThread djp_ = downloadJobProducerThread;
			if (djp_ != null) {
				djp_.cancel();
			}
			JobDispatcher dispatcher = downloadJobDispatcher;
			if (dispatcher != null) {
				dispatcher.terminateAllWorkerThreads();
			}
			pauseResumeHandler.resume();
			this.interrupt();
		} catch (Exception e) {
			LOG.error("Exception thrown in stopDownload(): {}", e.getMessage());
		}
	}

	public int getActiveDownloads() {
		return activeDownloads;
	}

	public synchronized void jobStarted() {
		activeDownloads++;
	}

	public void jobFinishedSuccessfully(int bytesDownloaded) {
		synchronized (this) {
			atlasProgress.incMapDownloadProgress();
			activeDownloads--;
			jobsCompleted++;
		}
		atlasProgress.updateGUI();
	}

	public void jobFinishedWithError(boolean retry) {
		synchronized (this) {
			activeDownloads--;
			if (retry) {
				jobsRetryError++;
			} else {
				jobsPermanentError++;
				atlasProgress.incMapDownloadProgress();
			}
		}
		if (!atlasProgress.ignoreDownloadErrors()) {
			Toolkit.getDefaultToolkit().beep();
		}
		atlasProgress.setErrorCounter(jobsRetryError, jobsPermanentError);
		atlasProgress.updateGUI();
	}

	public int getMaxDownloadRetries() {
		return maxDownloadRetries;
	}

	public AtlasProgress getAtlasProgress() {
		return atlasProgress;
	}

	public File getCustomAtlasDir() {
		return customAtlasDir;
	}

	public void setCustomAtlasDir(File customAtlasDir) {
		this.customAtlasDir = customAtlasDir;
	}

	public void setQuitMobacAfterAtlasCreation(boolean quitMobacAfterAtlasCreation) {
		this.quitMobacAfterAtlasCreation = quitMobacAfterAtlasCreation;
	}

	@Override
	public boolean isMapPreviewThread() {
		return false;
	}
}
