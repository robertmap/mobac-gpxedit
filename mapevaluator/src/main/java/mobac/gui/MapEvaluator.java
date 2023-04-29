package mobac.gui;

import bsh.EvalError;
import mobac.gui.actions.HelpAction;
import mobac.gui.components.LineNumberedPaper;
import mobac.gui.mapview.LogPreviewMap;
import mobac.mapsources.MapEvaluatorBeanShellHttpMapSource;
import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.custom.BeanShellHttpMapSource;
import mobac.mapsources.loader.CustomMapSourceLoader;
import mobac.program.DirectoryManager;
import mobac.program.ProgramInfo;
import mobac.program.interfaces.HttpMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.model.EastNorthCoordinate;
import mobac.tools.MapSourceCapabilityDetector;
import mobac.tools.MapSourceCapabilityGUI;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MapEvaluator extends JFrame {

	private static MapEvaluator INSTANCE;
	private final LogPreviewMap previewMap;
	private final JSplitPane splitPane;
	private final LineNumberedPaper mapSourceEditor;
	private final CustomMapSourceLoader xmlLoader;
	private final MapSource defaultOsmMapSource;
	protected final Logger log;
	private File chooserDir;

	private File loadedFile;

	public MapEvaluator() throws HeadlessException {
		super(ProgramInfo.getCompleteTitle());
		log = LoggerFactory.getLogger(this.getClass());
		addWindowListener(new MEWindowAdapter());
		setMinimumSize(new Dimension(300, 300));
		setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		previewMap = new LogPreviewMap();

		// previewMap.setMapMarkerVisible(true);
		// previewMap.addMapMarker(new ReferenceMapMarker(Color.RED, 1, 2));

		defaultOsmMapSource = MapSourcesManager.getInstance().getDefaultMapSource();

		chooserDir = DirectoryManager.mapSourcesDir;

		xmlLoader = new CustomMapSourceLoader(null, null);
		mapSourceEditor = new LineNumberedPaper(3, 60);
		try {
			String code = Utilities.loadTextResource("bsh/default.bsh");
			mapSourceEditor.setText(code);
		} catch (IOException e) {
			log.error("", e);
		}
		JPanel bottomPanel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar("Toolbar");
		addButtons(toolBar);
		bottomPanel.setMinimumSize(new Dimension(200, 100));

		JScrollPane editorScrollPane = new JScrollPane(mapSourceEditor);
		bottomPanel.add(toolBar, BorderLayout.NORTH);
		bottomPanel.add(editorScrollPane, BorderLayout.CENTER);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewMap, bottomPanel);
		add(splitPane, BorderLayout.CENTER);
		setSize(800, 600);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		INSTANCE = this;
	}

	public static void log(String msg) {
		INSTANCE.previewMap.addLog(msg);
	}

	private void addButtons(JToolBar toolBar) {
		JButton button = null;

		button = new JButton("Load Template", Utilities.loadResourceImageIcon("new-icon.png"));
		button.setToolTipText("Reset custom code editor to one of several templates");
		button.addActionListener((event) -> loadTemplate());
		toolBar.add(button);

		button = new JButton("Load", Utilities.loadResourceImageIcon("open-icon.png"));
		button.setToolTipText("Load custom map source from file");
		button.addActionListener((event) -> loadMapSource());
		toolBar.add(button);

		button = new JButton("Save", Utilities.loadResourceImageIcon("save-icon.png"));
		button.setToolTipText("Save custom map source to file");
		button.addActionListener((event) -> saveMapSource());
		toolBar.add(button);

		button = new JButton("Execute code", Utilities.loadResourceImageIcon("check-icon.png"));
		button.setToolTipText("Switch to custom map source (as defined by the custom code)");
		button.addActionListener((event) -> executeCode());
		toolBar.add(button);

		button = new JButton("OSM", Utilities.loadResourceImageIcon("osm-icon.png"));
		button.setToolTipText("Switch back to predefined OpenStreetMap mapsource");
		button.addActionListener((event) -> previewMap.setMapSource(defaultOsmMapSource));
		toolBar.add(button);
		button = new JButton("Toggle tile info", Utilities.loadResourceImageIcon("info-icon.png"));
		button.setToolTipText("Show/hide tile info");
		button.addActionListener((event) -> previewMap.setTileGridVisible(!previewMap.isTileGridVisible()));
		toolBar.add(button);

		button = new JButton("Test Capabilities", Utilities.loadResourceImageIcon("capabilities-icon.png"));
		button.setToolTipText("<html>Test the tile-update capabilities for the current map<br>"
				+ "using the current center of the map as test point</html>");
		button.addActionListener((event) -> testCapabilities());
		toolBar.add(button);

		// button = new JButton("Log");
		// button.setToolTipText("Show Log");
		// button.addActionListener((event) -> showLog());
		// toolBar.add(button);

		button = new JButton("Help", Utilities.loadResourceImageIcon("help-icon.png"));
		button.setToolTipText("Show help dialog");
		button.addActionListener(new HelpAction());
		toolBar.add(button);
	}

	private void showLog() {

	}
	private void loadTemplate() {
		try {
			String[] options = {"Empty", "OpenStreetMap Mapnik"};
			int a = JOptionPane.showOptionDialog(MapEvaluator.this, "Please select an template", "Select template", 0,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			String code = "";
			switch (a) {
				case (0) :
					code = Utilities.loadTextResource("bsh/empty.bsh");
					break;
				case (1) :
					code = Utilities.loadTextResource("bsh/osm.bsh");
					break;
			}

			mapSourceEditor.setText(code);
		} catch (IOException e) {
			log.error("", e);
		}
	}

	private void loadMapSource() {
		try {
			final JFileChooser fc = getMapSourceFileChooser(false);
			int returnVal = fc.showOpenDialog(MapEvaluator.this);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			chooserDir = fc.getSelectedFile().getParentFile();
			List<String> lines = Files.readAllLines(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8);
			StringWriter sw = new StringWriter();
			for (String s : lines) {
				sw.write(s);
				sw.write("\n");
			}
			mapSourceEditor.setText(sw.toString());
			loadedFile = fc.getSelectedFile();
		} catch (IOException e) {
			log.error("", e);
			JOptionPane.showMessageDialog(MapEvaluator.this, "Error reading code from file:\n" + e.getMessage(),
					"Loading failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveMapSource() {
		final JFileChooser fc = getMapSourceFileChooser(true);
		int returnVal = fc.showOpenDialog(MapEvaluator.this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		chooserDir = fc.getSelectedFile().getParentFile();
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), StandardCharsets.UTF_8))) {
			bw.write(mapSourceEditor.getText());
		} catch (IOException e) {
			log.error("", e);
			JOptionPane.showMessageDialog(MapEvaluator.this, "Error writing code to disk:\n" + e.getMessage(),
					"Saving failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private JFileChooser getMapSourceFileChooser(boolean save) {
		final JFileChooser fc = new JFileChooser();
		if (save) {
			fc.setDialogTitle("Save custom map source");
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			fc.setSelectedFile(loadedFile);
		} else {
			fc.setDialogTitle("Load custom map source");
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
		}
		fc.setCurrentDirectory(chooserDir);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileNameExtensionFilter defaultFilter = new FileNameExtensionFilter("MOBAC custom map source", "bsh", "xml");
		fc.addChoosableFileFilter(defaultFilter);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Beanshell map source", "bsh"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter("Custom XML map source", "xml"));
		fc.setFileFilter(defaultFilter);
		return fc;
	}

	private void testCapabilities() {
		final MapSource mapSource = previewMap.getMapSource();
		final EastNorthCoordinate coordinate = previewMap.getCenterCoordinate();

		final List<MapSourceCapabilityDetector> result = new ArrayList<>();
		Runnable r = () -> {

			MapSourceCapabilityGUI gui = null;
			try {
				gui = new MapSourceCapabilityGUI(result);
				gui.setWorkerThread(Thread.currentThread());
				gui.setVisible(true);
				for (int zoom = mapSource.getMinZoom(); zoom < mapSource.getMaxZoom(); zoom++) {
					MapSourceCapabilityDetector mstd = new MapSourceCapabilityDetector((HttpMapSource) mapSource,
							coordinate, zoom);
					if (!gui.isVisible()) {
						return;
					}
					mstd.testMapSource();
					result.add(mstd);
					gui.refresh();
					Utilities.checkForInterruption();
				}
				gui.toFront();
			} catch (InterruptedException e) {
			} finally {
				gui.workerFinished();
			}
		};
		new Thread(r).start();
	}

	private void executeCode() {
		String code = mapSourceEditor.getText().trim();
		if (code.startsWith("<?xml")) {
			executeXMLCode(code);
		} else {
			executeBeanShellCode(code);
		}
	}

	private void executeXMLCode(String code) {
		try (InputStream in = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8))) {
			MapSource mapSource = xmlLoader.loadCustomMapSource(in);
			previewMap.setMapSource(mapSource, true);
			previewMap.refreshMap();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(this, "Error in custom code: \n" + e.getMessage(), "Error in custom code",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void executeBeanShellCode(String code) {
		try {
			BeanShellHttpMapSource testMapSource = new MapEvaluatorBeanShellHttpMapSource(code);
			if (testMapSource.testCode()) {
				previewMap.setMapSource(testMapSource);
				return;
			}
			JOptionPane.showMessageDialog(this, "Error in custom code: result is null", "Error in custom code",
					JOptionPane.ERROR_MESSAGE);
		} catch (EvalError e) {
			log.error("", e);
			JOptionPane.showMessageDialog(this, "Error in custom code: \n" + e.getMessage(), "Error in custom code",
					JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof EvalError) {
				log.error("", cause);
				JOptionPane.showMessageDialog(this, "Error in custom code: \n" + cause.getMessage(),
						"Error in custom code", JOptionPane.ERROR_MESSAGE);
			} else {
				GUIExceptionHandler.processException(e);
			}
		}
	}

	private class MEWindowAdapter extends WindowAdapter {

		@Override
		public void windowOpened(WindowEvent e) {
			splitPane.setDividerLocation(0.8);
			previewMap.setEnabled(true);
		}

	}

}
