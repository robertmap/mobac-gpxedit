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
package mobac.utilities;

import com.sleepycat.je.ExceptionEvent;
import com.sleepycat.je.ExceptionListener;
import mobac.exceptions.AbortedByUserException;
import mobac.program.ProgramInfo;
import mobac.program.interfaces.ExceptionExtendedInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class GUIExceptionHandler implements Thread.UncaughtExceptionHandler, ExceptionListener {

	private static final GUIExceptionHandler INSTANCE = new GUIExceptionHandler();

	private static final Logger log = LoggerFactory.getLogger(GUIExceptionHandler.class);

	private static final double MB_DIV = 1024d * 1024d;

	private static final String URL_BUGTRACKER = "https://sourceforge.net/p/mobac/bugs/";

	static {
		Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
	}

	private GUIExceptionHandler() {
		super();
	}

	public static void registerForCurrentThread() {
		Thread t = Thread.currentThread();
		if (t.getUncaughtExceptionHandler() != INSTANCE) {
			log.trace("Registering MOBAC exception handler for thread \"{}\" [{}]", t.getName(), t.getId());
			t.setUncaughtExceptionHandler(INSTANCE);
		}
	}

	public static GUIExceptionHandler getInstance() {
		return INSTANCE;
	}

	public static void processException(Throwable e) {
		processException(Thread.currentThread(), e);
	}

	public static void processFatalExceptionSimpleDialog(String dialogMessage, Throwable e) {
		log.error(dialogMessage + ": " + e.getMessage(), e);
		String[] options = {I18nUtils.localizedStringForKey("Exit"),
				I18nUtils.localizedStringForKey("dlg_download_show_error_report")};
		int a = JOptionPane.showOptionDialog(null, dialogMessage, I18nUtils.localizedStringForKey("Error"), 0,
				JOptionPane.ERROR_MESSAGE, null, options, options[0]);
		if (a == 1) {
			GUIExceptionHandler.showExceptionDialog(e);
		}
		System.exit(1);
	}

	public static void processException(Thread thread, Throwable t) {
		log.error("Uncaught exception: {}", t.toString(), t);
		showExceptionDialog(thread, t, null);
	}

	public static void processException(Thread thread, Throwable t, AWTEvent newEvent) {
		String eventText = newEvent.toString();
		log.error("Uncaught exception on processing event {}", eventText, t);
		if (eventText.length() > 100) {
			String[] parts = eventText.split(",");
			StringWriter sw = new StringWriter(eventText.length() + 20);
			sw.write(parts[0]);
			int len = parts[0].length();
			for (int i = 1; i < parts.length; i++) {
				String s = parts[i];
				if (s.length() + len > 80) {
					sw.write("\n\t");
					len = 0;
				}
				sw.write(s);
			}
			eventText = "Event: " + sw;
		}
		showExceptionDialog(thread, t, eventText);
	}

	public static String prop(String key) {
		return System.getProperty(key, "");
	}

	public static void showExceptionDialog(Throwable t) {
		showExceptionDialog(null, Thread.currentThread(), t, null);
	}

	public static void showExceptionDialog(String message, Throwable t) {
		showExceptionDialog(message, Thread.currentThread(), t, null);
	}

	public static void showExceptionDialog(Thread thread, Throwable t, String additionalInfo) {
		showExceptionDialog(null, thread, t, additionalInfo);
	}

	public static synchronized void showExceptionDialog(String message, Thread thread, Throwable t,
			String additionalInfo) {
		try {
			if (t != null) {
				// Recursive check - in case the Exception was thrown in a Component.paint()
				// method
				// this may lead to an infinite recursion with one message dialog per recursion
				// loop
				String thisClassName = GUIExceptionHandler.class.getName();
				for (StackTraceElement ste : t.getStackTrace()) {
					if (ste.getClassName().startsWith(thisClassName)
							&& "showExceptionDialog".equals(ste.getMethodName())) {
						log.error("Recursive error loop detected - aborting");
						return;
					}
				}
			}
			if (ignoreException(t)) {
				log.info("Ignored Exception: " + t);
				return;
			}

			boolean notABug = t instanceof NotABug;

			StringBuilder sb = new StringBuilder(2048);
			if (message != null) {
				sb.append("Error message: " + message + "\n");
			}
			sb.append("Version: " + ProgramInfo.getCompleteTitle());
			sb.append("\nPlatform: " + prop("os.name") + " (" + prop("os.version") + ")");
			String windowManager = System.getProperty("sun.desktop");
			if (windowManager != null) {
				sb.append(" (" + windowManager + ")");
			}

			String dist = OSUtilities.getLinuxDistributionName();
			if (dist != null) {
				sb.append("\nDistribution name: " + dist);
			}

			sb.append("\nJava VM: " + prop("java.vm.vendor") + " " + prop("java.vm.name") + " ("
					+ prop("java.runtime.version") + ")");
			Runtime r = Runtime.getRuntime();
			sb.append(String.format("\nMax heap size: %3.2f MiB", r.maxMemory() / MB_DIV));
			// sb.append("\nMapsources rev: " +
			// MapSourcesUpdater.getCurrentMapSourcesRev());

			sb.append("\nCPU cores: " + Runtime.getRuntime().availableProcessors());

			if (thread != null) {
				sb.append("\n\nThread: " + thread.getName());
			}

			if (additionalInfo != null) {
				sb.append("\n\n" + additionalInfo);
			}

			if (t instanceof ExceptionExtendedInfo) {
				ExceptionExtendedInfo ei = (ExceptionExtendedInfo) t;
				sb.append("\n");
				sb.append(ei.getExtendedInfo());
			}

			String guiText;
			String dialogTitle;
			JPanel panel = new JPanel(new BorderLayout());
			if (t != null && !notABug) {
				sb.append("\n\nError hierarchy:");
				Throwable tmp = t;
				while (tmp != null) {
					sb.append("\n  " + tmp.getClass().getSimpleName() + ": " + tmp.getMessage());
					tmp = tmp.getCause();
				}

				String exceptionName = t.getClass().getSimpleName();
				dialogTitle = "Unexpected Exception: " + exceptionName;

				sb.append("\n\n#############################################################\n\n");
				while (t != null) {

					sb.append(t);
					printStackTrace(t, sb);
					sb.append("\n");

					Throwable[] sup = t.getSuppressed();
					if (sup != null) {
						for (Throwable st : sup) {
							sb.append("    Suppressed: ");
							sb.append(st.toString());
							sb.append("\n");
						}
					}

					t = t.getCause();
					if (t != null) {
						sb.append("\nCaused by: ");
					}
				}

				sb.append("\n#############################################################");

				guiText = "An unexpected exception occurred (" + exceptionName + ")<br>"
						+ "<p>Please create a ticket in the bug tracker on <a href=\"" + URL_BUGTRACKER
						+ "\">SourceForge.net</a><br>"
						+ "<b>Please include a detailed description of your performed actions <br>"
						+ "before the error occurred.</b></p>Be sure to include the following information:";
			} else {
				dialogTitle = "System report";
				guiText = "This is a user generated system report.<br>"
						+ "<p>It can be used for providing detailed version information <br>"
						+ "in case a new ticket in the bug tracker is created on <a href=\"" + URL_BUGTRACKER
						+ "\">SourceForge.net</a><br><b>Please include a detailed description of your problem <br>"
						+ "and what steps are necessary to reproduce the problem.</b>" + "</p>";
			}
			JEditorPane text = new JEditorPane("text/html", "");
			text.setOpaque(true);
			text.setBackground(UIManager.getColor("JFrame.background"));
			text.setEditable(false);
			text.addHyperlinkListener((e) -> {
				if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
					return;
				}
				try {
					Desktop.getDesktop().browse(e.getURL().toURI());
				} catch (Exception e1) {
					log.error("", e1);
				}
			});
			panel.add(text, BorderLayout.NORTH);
			if (!notABug) {
				try {
					String clipboardData = sb.toString();
					// format for Sourceforge.net bugtracker
					clipboardData = "~~~" + System.lineSeparator() + clipboardData + System.lineSeparator() + "~~~";
					StringSelection contents = new StringSelection(clipboardData);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents,
							(clipboard, content) -> Function.identity());
					guiText += "<p>(The following text has already been copied to your clipboard.)</p>";
				} catch (RuntimeException x) {
					log.error("", x);
				}
			}
			text.setText("<html>" + guiText + "</html>");

			JCheckBox quickMOBACcb = new JCheckBox("Do not continue and quit program by pressing OK");
			JTextArea info = new JTextArea(sb.toString(), 20, 60);
			info.setCaretPosition(0);
			info.setEditable(false);
			info.setMinimumSize(new Dimension(200, 150));
			panel.add(new JScrollPane(info), BorderLayout.CENTER);
			panel.add(quickMOBACcb, BorderLayout.SOUTH);
			panel.setMinimumSize(new Dimension(700, 300));
			panel.validate();
			JOptionPane.showMessageDialog(null, panel, dialogTitle, JOptionPane.ERROR_MESSAGE);
			if (quickMOBACcb.isSelected()) {
				log.warn("User selected to quit MOBAC after an exception");
				System.exit(1);
			}
		} catch (Throwable t1) {
			t1.printStackTrace();
		}
	}

	/**
	 * Print stack trace of <code>t</code> to <code>sb</code> but ignore lines if
	 * they occur more than 2 times (useful for {@link StackOverflowError})
	 *
	 * @param t
	 * @param sb
	 */
	private static void printStackTrace(Throwable t, StringBuilder sb) {
		List<StackTraceElement> stackTrace = List.of(t.getStackTrace());
		String lastSte = "";
		Iterator<StackTraceElement> it = stackTrace.iterator();
		while (it.hasNext()) {
			String ste = it.next().toString();
			if (ste.equals(lastSte)) {
				int repeat = 0;
				while (lastSte.equals(ste)) {
					repeat++;
					if (!it.hasNext()) {
						ste = null;
						break;
					}
					ste = it.next().toString();
				}
				sb.append(String.format(" [%d identical lines suppressed]", repeat));
			}
			if (ste == null) {
				break;
			}
			sb.append("\n\tat " + ste);
			lastSte = ste;
		}
	}

	private static boolean ignoreException(Throwable t) {
		return t instanceof AbortedByUserException;
	}

	public static void installToolkitEventQueueProxy() {

		boolean isLikeJava7 = Float.parseFloat(System.getProperty("java.specification.version")) > 1.6;

		EventQueueProxy eventQueueProxy = new EventQueueProxy();
		// Bug handling. See
		// http://stackoverflow.com/questions/3158254/how-to-replace-the-awt-eventqueue-with-own-implementation
		try {
			if (isLikeJava7) {
				EventQueue.invokeAndWait(eventQueueProxy);
			} else {
				eventQueueProxy.run();
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Exception e = new RuntimeException("Test_" + i, new Exception("Inner"));
				e.addSuppressed(new RuntimeException("Suppressed"));
				throw e;
			} catch (Exception e) {
				showExceptionDialog("Test " + i, e);
			} catch (Error e) {
				showExceptionDialog(e);
			}
			break;
		}
	}

	/**
	 * Implementation for {@link com.sleepycat.je.ExceptionListener}
	 */
	public void exceptionThrown(ExceptionEvent paramExceptionEvent) {
		Exception e = paramExceptionEvent.getException();
		log.error("Exception in tile store: {}", paramExceptionEvent, e);
		showExceptionDialog(e);
	}

	/**
	 * Implementation for {@link Thread.UncaughtExceptionHandler}
	 */
	public void uncaughtException(Thread t, Throwable e) {
		processException(t, e);
	}

	public interface NotABug {
	}

	/**
	 * Catching all Runtime Exceptions in Swing
	 * <p>
	 * http://ruben42.wordpress.com/2009/03/30/catching-all-runtime-exceptions-in-swing/
	 */
	protected static class EventQueueProxy extends EventQueue implements Runnable {

		/**
		 * Installs the {@link EventQueueProxy}
		 */
		@Override
		public void run() {
			EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
			queue.push(this);
		}

		protected void dispatchEvent(AWTEvent newEvent) {
			try {
				super.dispatchEvent(newEvent);
			} catch (Throwable e) {
				if (e instanceof ArrayIndexOutOfBoundsException) {
					StackTraceElement[] st = e.getStackTrace();
					if (st.length > 0) {
						if ("sun.font.FontDesignMetrics".equals(st[0].getClassName())) {
							log.error("Ignored JRE bug exception {} caused by : {}", e.getMessage(), st[0]);
							// This is a known JRE bug - we just ignore it
							return;
						}
					}
				}
				GUIExceptionHandler.processException(Thread.currentThread(), e);
			}
		}
	}
}
