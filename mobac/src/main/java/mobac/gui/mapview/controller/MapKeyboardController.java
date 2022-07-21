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
package mobac.gui.mapview.controller;

import mobac.gui.mapview.PreviewMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements the GUI logic for the preview map panel that manages the map movement by mouse and actions triggered by
 * key strokes.
 */
public class MapKeyboardController extends JMapController {

    /**
     * A Timer for smoothly moving the map area
     */
    private static final Timer timer = new Timer(true);
    /**
     * The maximum speed (pixels per timer interval)
     */
    private static final double MAX_SPEED = 20;
    /**
     * The speed increase per timer interval when a cursor button is clicked
     */
    private static final double ACCELERATION = 0.10;
    private static final String ACTION_MOVE_RIGHT = "MOVE_RIGHT";
    private static final String ACTION_MOVE_LEFT = "MOVE_LEFT";
    private static final String ACTION_MOVE_UP = "MOVE_UP";
    private static final String ACTION_MOVE_DOWN = "MOVE_DOWN";
    private static final String ACTION_STOP_MOVE_HORIZONTALLY = "STOP_MOVE_HORIZONTALLY";
    private static final String ACTION_STOP_MOVE_VERTICALLY = "STOP_MOVE_VERTICALLY";
    private static final String ACTION_ZOOM_IN = "ZOOM_IN";
    private static final String ACTION_ZOOM_OUT = "ZOOM_OUT";
    private static final String ACTION_PREVIOUS_MAP = "PREVIOUS_MAP";
    private static final String ACTION_NEXT_MAP = "NEXT_MAP";
    private static final String ACTION_REFRESH = "REFRESH";
    /**
     * How often to do the moving (milliseconds)
     */
    private static final long timerInterval = 20;
    private final InputMap inputMap;
    /**
     * Does the moving
     */
    private MoveTask moveTask = new MoveTask();

    public MapKeyboardController(PreviewMap map, boolean enabled) {
        super(map);

        inputMap = new ComponentInputMap(map);
        ActionMap actionMap = map.getActionMap();

        // map moving
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), ACTION_MOVE_RIGHT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), ACTION_MOVE_LEFT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), ACTION_MOVE_UP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), ACTION_MOVE_DOWN);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), ACTION_STOP_MOVE_HORIZONTALLY);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), ACTION_STOP_MOVE_HORIZONTALLY);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), ACTION_STOP_MOVE_VERTICALLY);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), ACTION_STOP_MOVE_VERTICALLY);

        // zooming. To avoid confusion about which modifier key to use,
        // we just add all keys left of the space bar
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK, false), ACTION_ZOOM_IN);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.META_DOWN_MASK, false), ACTION_ZOOM_IN);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK, false), ACTION_ZOOM_IN);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK, false), ACTION_ZOOM_OUT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.META_DOWN_MASK, false), ACTION_ZOOM_OUT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK, false), ACTION_ZOOM_OUT);

        // map selection
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK, false), ACTION_PREVIOUS_MAP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.META_DOWN_MASK, false), ACTION_PREVIOUS_MAP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK, false), ACTION_PREVIOUS_MAP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK, false), ACTION_NEXT_MAP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.META_DOWN_MASK, false), ACTION_NEXT_MAP);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK, false), ACTION_NEXT_MAP);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true), ACTION_REFRESH);

        // action mapping
        actionMap.put(ACTION_MOVE_RIGHT, new MoveRightAction());
        actionMap.put(ACTION_MOVE_LEFT, new MoveLeftAction());
        actionMap.put(ACTION_MOVE_UP, new MoveUpAction());
        actionMap.put(ACTION_MOVE_DOWN, new MoveDownAction());
        actionMap.put(ACTION_STOP_MOVE_HORIZONTALLY, new StopMoveHorizontallyAction());
        actionMap.put(ACTION_STOP_MOVE_VERTICALLY, new StopMoveVerticallyAction());
        actionMap.put(ACTION_ZOOM_IN, new ZoomInAction());
        actionMap.put(ACTION_ZOOM_OUT, new ZoomOutAction());
        actionMap.put(ACTION_NEXT_MAP, new NextMapAction());
        actionMap.put(ACTION_PREVIOUS_MAP, new PreviousMapAction());
        actionMap.put(ACTION_REFRESH, new RefreshAction());

        if (enabled) {
            enable();
        }
    }

    @Override
    public void disable() {
        map.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, new ComponentInputMap(map));
    }

    @Override
    public void enable() {
        map.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
    }

    private class MoveRightAction extends AbstractAction {
        private static final long serialVersionUID = -6758721144600926744L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionX(1);
        }
    }

    private class MoveLeftAction extends AbstractAction {
        private static final long serialVersionUID = 2695221718338284951L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionX(-1);
        }
    }

    private class MoveUpAction extends AbstractAction {
        private static final long serialVersionUID = -8414310977137213707L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionY(-1);
        }
    }

    private class MoveDownAction extends AbstractAction {
        private static final long serialVersionUID = -5360890019457799681L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionY(1);
        }
    }

    private class StopMoveHorizontallyAction extends AbstractAction {
        private static final long serialVersionUID = -5360890019457799681L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionX(0);
        }
    }

    private class StopMoveVerticallyAction extends AbstractAction {
        private static final long serialVersionUID = -5360890019457799681L;

        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionY(0);
        }
    }

    /**
     * Moves the map depending on which cursor keys are pressed (or not)
     */
    private class MoveTask extends TimerTask {
        /**
         * Indicated if <code>moveTask</code> is currently enabled (periodically executed via timer) or disabled
         */
        protected boolean scheduled = false;
        /**
         * The current x speed (pixels per timer interval)
         */
        private double speedX = 1;
        /**
         * The current y speed (pixels per timer interval)
         */
        private double speedY = 1;
        /**
         * The horizontal direction of movement, -1:left, 0:stop, 1:right
         */
        private int directionX = 0;
        /**
         * The vertical direction of movement, -1:up, 0:stop, 1:down
         */
        private int directionY = 0;

        protected void setDirectionX(int directionX) {
            this.directionX = directionX;
            updateScheduleStatus();
        }

        protected void setDirectionY(int directionY) {
            this.directionY = directionY;
            updateScheduleStatus();
        }

        private void updateScheduleStatus() {
            boolean newMoveTaskState = !(directionX == 0 && directionY == 0);

            if (newMoveTaskState != scheduled) {
                scheduled = newMoveTaskState;
                if (newMoveTaskState)
                    timer.schedule(this, 0, timerInterval);
                else {
                    // We have to create a new instance because rescheduling a
                    // once canceled TimerTask is not possible
                    moveTask = new MoveTask();
                    cancel(); // Stop this TimerTask
                }
            }
        }

        @Override
        public void run() {
            // update the x speed
            switch (directionX) {
                case -1:
                    if (speedX > -1)
                        speedX = -1;
                    if (speedX > -1 * MAX_SPEED)
                        speedX -= ACCELERATION;
                    break;
                case 0:
                    speedX = 0;
                    break;
                case 1:
                    if (speedX < 1)
                        speedX = 1;
                    if (speedX < MAX_SPEED)
                        speedX += ACCELERATION;
                    break;
            }

            // update the y speed
            switch (directionY) {
                case -1:
                    if (speedY > -1)
                        speedY = -1;
                    if (speedY > -1 * MAX_SPEED)
                        speedY -= ACCELERATION;
                    break;
                case 0:
                    speedY = 0;
                    break;
                case 1:
                    if (speedY < 1)
                        speedY = 1;
                    if (speedY < MAX_SPEED)
                        speedY += ACCELERATION;
                    break;
            }

            // move the map
            int moveX = (int) Math.floor(speedX);
            int moveY = (int) Math.floor(speedY);
            if (moveX != 0 || moveY != 0)
                map.moveMap(moveX, moveY);
        }
    }

    private class ZoomInAction extends AbstractAction {
        private static final long serialVersionUID = 1471739991027644588L;

        public void actionPerformed(ActionEvent e) {
            map.zoomIn();
        }
    }

    private class ZoomOutAction extends AbstractAction {
        private static final long serialVersionUID = 1471739991027644588L;

        public void actionPerformed(ActionEvent e) {
            map.zoomOut();
        }
    }

    private class PreviousMapAction extends AbstractAction {
        private static final long serialVersionUID = -1492075614917423363L;

        public void actionPerformed(ActionEvent e) {
            map.selectPreviousMap();
        }
    }

    private class NextMapAction extends AbstractAction {
        private static final long serialVersionUID = -1491235614917423363L;

        public void actionPerformed(ActionEvent e) {
            map.selectNextMap();
        }
    }

    private class RefreshAction extends AbstractAction {

        private static final long serialVersionUID = -7235666079485033823L;

        public void actionPerformed(ActionEvent e) {
            map.refreshMap();
        }
    }

}
