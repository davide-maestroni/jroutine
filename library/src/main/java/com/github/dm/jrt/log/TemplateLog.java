/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Abstract implementation of a log.
 * <p/>
 * This class is useful to avoid the need of implementing all the methods defined in the interface.
 * <p/>
 * A standard format is applied to the log messages.<br/>
 * The inheriting class may just implement the writing of the formatted message, or customize it
 * at any level.
 * <p/>
 * Created by davide-maestroni on 10/03/2014.
 */
public abstract class TemplateLog implements Log {

    private static final String DATE_FORMAT = "MM/dd HH:mm:ss.SSS z";

    private static final String EXCEPTION_FORMAT = " > caused by:%n%s";

    private static final String LOG_FORMAT = "%s\t%s\t%s\t%s> %s";

    /**
     * Prints the stack trace of the specified throwable into a string.
     *
     * @param throwable the throwable instance.
     * @return the printed stack trace.
     */
    @NotNull
    public static String printStackTrace(@NotNull final Throwable throwable) {

        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String format(@NotNull final Level level, @NotNull final List<Object> contexts,
            @Nullable final String message) {

        return String.format(LOG_FORMAT,
                             new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(
                                     new Date()), Thread.currentThread().getName(),
                             contexts.toString(), level, message);
    }

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(Level.DEBUG, contexts, message, throwable);
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(Level.ERROR, contexts, message, throwable);
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        log(Level.WARNING, contexts, message, throwable);
    }

    /**
     * Formats and then write the specified log message.
     *
     * @param level     the log level.
     * @param contexts  the log context array.
     * @param message   the log message.
     * @param throwable the related exception.
     */
    protected void log(@NotNull final Level level, @NotNull final List<Object> contexts,
            @Nullable final String message, @Nullable final Throwable throwable) {

        String formatted = format(level, contexts, message);
        if (throwable != null) {
            formatted += String.format(EXCEPTION_FORMAT, printStackTrace(throwable));
        }

        log(formatted);
    }

    /**
     * Writes the specified message after it's been formatted.
     *
     * @param message the message.
     */
    protected void log(@NotNull final String message) {

    }
}
