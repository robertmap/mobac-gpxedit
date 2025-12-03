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
package mobac.program.model;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.Toolkit;

public class NumericDocument extends PlainDocument {
	public static final String NUMERIC = "0123456789";
	private static final long serialVersionUID = 1L;

	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {

		if (str == null) {
			return;
		}

		for (char c : str.toCharArray()) {
			if (NUMERIC.indexOf(c) == -1) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}
		}

		super.insertString(offset, str, attr);

	}
}
