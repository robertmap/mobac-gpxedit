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
package mobac.gui.components;

import mobac.utilities.I18nUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

public class JTileSizeCombo extends JIntCombo {

	public static final int MIN = 50;
	public static final int MAX = 8192;
	private static final long serialVersionUID = 1L;
	private static final Vector<Integer> TILE_SIZE_VALUES;

	static Integer DEFAULT;

	private static final Logger log = LoggerFactory.getLogger(JTileSizeCombo.class);

	static {
		DEFAULT = 256;
		TILE_SIZE_VALUES = new Vector<>();
		TILE_SIZE_VALUES.addElement(64);
		TILE_SIZE_VALUES.addElement(128);
		TILE_SIZE_VALUES.addElement(DEFAULT);
		TILE_SIZE_VALUES.addElement(512);
		TILE_SIZE_VALUES.addElement(768);
		TILE_SIZE_VALUES.addElement(1024);
		TILE_SIZE_VALUES.addElement(1536);
		for (int i = 2048; i <= MAX; i += 1024) {
			TILE_SIZE_VALUES.addElement(i);
		}
	}

	public JTileSizeCombo() {
		super(TILE_SIZE_VALUES, DEFAULT);
		setEditable(true);
		setEditor(new Editor());
		setMaximumRowCount(TILE_SIZE_VALUES.size());
		setSelectedItem(DEFAULT);
	}

	@Override
	protected void createEditorComponent() {
		editorComponent = new JIntField(MIN, MAX, 4, I18nUtils.localizedStringForKey("msg_invalid_tile_size"));
	}

}
