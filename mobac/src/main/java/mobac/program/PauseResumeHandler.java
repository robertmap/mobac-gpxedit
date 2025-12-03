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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central instance that allows to pause/resume multiple threads at once. Used
 * in MOBAC for pausing/resuming map tile download and map creation process.
 */
public class PauseResumeHandler {

	protected final AtomicBoolean paused = new AtomicBoolean(false);

	public boolean isPaused() {
		return paused.get();
	}

	/**
	 * Enters the pause state.
	 */
	public void pause() {
		paused.set(true);
	}

	/**
	 * End the pause state and resumes all waiting threads.
	 */
	public void resume() {
		paused.set(false);
		synchronized (this) {
			this.notifyAll();
		}
	}

	/**
	 * If {@link #isPaused()}== <code>true</code> this method will not return until
	 * {@link #resume()} has been called. Otherwise this method returns immediately.
	 *
	 * @throws InterruptedException
	 *             Thrown if the calling {@link Thread} is interrupted while waiting
	 *             for resume
	 */
	public void pauseWait() throws InterruptedException {
		if (paused.get()) {
			synchronized (this) {
				if (paused.get()) {
					this.wait();
				}
			}
		}
	}

}
