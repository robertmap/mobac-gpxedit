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
package mobac.gui.dialogs;

import mobac.gui.MainGUI;
import mobac.utilities.GBC;
import mobac.utilities.I18nUtils;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Help extends JFrame implements WindowListener {

	private static Help INSTANCE = null;

	public Help() throws HeadlessException {
		super(I18nUtils.localizedStringForKey("dlg_help_title"));
		setIconImages(MainGUI.MOBAC_ICONS);
		setLayout(new GridBagLayout());
		JLabel text = new JLabel();
		JButton closeButton = new JButton(I18nUtils.localizedStringForKey("Close"));
		closeButton.setDefaultCapable(true);
		closeButton.addActionListener((e) -> {
			setVisible(false);
			dispose();
		});

		try (InputStream in = I18nUtils.getI18nResourceAsStream("resources/text/help_dialog", "html")) {
			byte[] buf = in.readAllBytes();
			String helpMessage = new String(buf, StandardCharsets.UTF_8);
			// Strip out all line breaks because JOptionPane shows
			// the raw HTML code otherwise
			// helpMessage = helpMessage.replaceAll("\n", "");
			// text.setFont(mobac.gui.MainGUI.defaultFont());
			text.setText(helpMessage);
		} catch (IOException e) {
		}
		add(text, GBC.eol().insets(10, 10, 10, 10));
		add(closeButton, GBC.eol().anchor(GBC.CENTER).insets(0, 0, 0, 10));
		pack();

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((dim.width - getWidth()) / 2, (dim.height - getHeight()) / 2);

		setAlwaysOnTop(true);
		setResizable(false);
	}

	public static synchronized void showHelp() {
		if (INSTANCE == null) {
			INSTANCE = new Help();
		}
		INSTANCE.setVisible(true);
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
		INSTANCE = null;
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}
}
