/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract implementation of a log.
 * <p/>
 * A standard format is applied to the log messages.<br/>
 * The inheriting class may just implement the writing of the formatted message, or customize it
 * at any level.
 * <p/>
 * Created by davide on 10/3/14.
 */
public abstract class LogAdapter implements Log {

    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS z";

    private static final String LOG_FORMAT = "[%s] %s - %s: %s";

    @Override
    public void dbg(final String message) {

        log(LogLevel.DEBUG, message);
    }

    @Override
    public void err(final String message) {

        log(LogLevel.ERROR, message);
    }

    @Override
    public void wrn(final String message) {

        log(LogLevel.WARNING, message);
    }

    /**
     * Writes the specified message after formatting it.
     *
     * @param level   the message level.
     * @param message the message.
     */
    protected void log(final LogLevel level, final String message) {

        log(String.format(LOG_FORMAT, new SimpleDateFormat(DATE_FORMAT).format(new Date()),
                          Thread.currentThread(), level, message));
    }

    /**
     * Writes the specified message after it's been formatted.
     *
     * @param message the message.
     */
    protected void log(final String message) {

    }
}