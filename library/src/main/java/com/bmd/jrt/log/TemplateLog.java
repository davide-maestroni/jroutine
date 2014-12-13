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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract implementation of a log.
 * <p/>
 * This class is useful to avoid the need of implementing all the methods defined in the interface.
 * <p/>
 * A standard format is applied to the log messages.<br/>
 * The inheriting class may just implement the writing of the formatted message, or customize it
 * at any level.
 * <p/>
 * Created by davide on 10/3/14.
 */
public abstract class TemplateLog implements Log {

    private static final String DATE_FORMAT = "MM/dd HH:mm:ss.SSS z";

    private static final String EXCEPTION_FORMAT = " > caused by:%n%s";

    private static final String LOG_FORMAT = "%s\t%s\t%s\t%s> %s";

    private static String format(@Nonnull final LogLevel level,
            @Nonnull final List<Object> contexts, @Nullable final String message) {

        return String.format(LOG_FORMAT, new SimpleDateFormat(DATE_FORMAT).format(new Date()),
                             Thread.currentThread().getName(), contexts.toString(), level, message);
    }

    @Override
    public void dbg(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(LogLevel.DEBUG, contexts, message, throwable);
    }

    @Override
    public void err(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(LogLevel.ERROR, contexts, message, throwable);
    }

    @Override
    public void wrn(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(LogLevel.WARNING, contexts, message, throwable);
    }

    /**
     * Formats and then write the specified log message.
     *
     * @param level     the log level.
     * @param contexts  the log context array.
     * @param message   the log message.
     * @param throwable the related exception.
     */
    protected void log(@Nonnull final LogLevel level, @Nonnull final List<Object> contexts,
            @Nullable final String message, @Nullable final Throwable throwable) {

        String formatted = format(level, contexts, message);

        if (throwable != null) {

            formatted += String.format(EXCEPTION_FORMAT, Logger.printStackTrace(throwable));
        }

        log(formatted);
    }

    /**
     * Writes the specified message after it's been formatted.
     *
     * @param message the message.
     */
    protected void log(@Nonnull final String message) {

    }
}