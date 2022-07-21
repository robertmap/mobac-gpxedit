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

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

public class JMenuItem2 extends JMenuItem implements ActionListener {

    private final Class<? extends ActionListener> actionClass;

    public JMenuItem2(String text, int mnemonic, Class<? extends ActionListener> actionClass) {
        super(text, mnemonic);
        this.actionClass = actionClass;
        addActionListener(this);
    }

    public JMenuItem2(String text, Class<? extends ActionListener> actionClass) {
        super(text);
        this.actionClass = actionClass;
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
        ActionListener al;
        try {
            al = actionClass.getConstructor().newInstance();
            al.actionPerformed(event);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
