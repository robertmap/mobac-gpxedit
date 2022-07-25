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
package mobac.gui.actions;

import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SplitPaneHideLeftComponent implements ActionListener {

	private final JSplitPane splitPane;
	private final Component leftComponent;
	private int dividerSize = 0;

	public SplitPaneHideLeftComponent(JSplitPane splitPane) {
		super();
		this.splitPane = splitPane;
		this.leftComponent = splitPane.getLeftComponent();
	}

	public void actionPerformed(ActionEvent e) {
		if (splitPane.getLeftComponent() == null) {
			// show left panel
			splitPane.setLeftComponent(leftComponent);
			splitPane.setDividerSize(dividerSize);
		} else {
			// hide left panel
			dividerSize = splitPane.getDividerSize();
			leftComponent.setPreferredSize(new Dimension(leftComponent.getWidth(), 100));
			splitPane.setDividerSize(0);
			splitPane.setLeftComponent(null);
		}
		splitPane.revalidate();
	}

}
