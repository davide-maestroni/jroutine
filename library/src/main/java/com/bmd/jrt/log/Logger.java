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

import com.bmd.jrt.log.Log.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class used for log messages.
 * <p/>
 * Created by davide on 10/3/14.
 */
public class Logger {

    private static final int DEBUG_LEVEL = LogLevel.DEBUG.ordinal();

    private static final int ERROR_LEVEL = LogLevel.ERROR.ordinal();

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final String EXCEPTION_MESSAGE = NEW_LINE + "Caused by exception:" + NEW_LINE;

    private static final int WARNING_LEVEL = LogLevel.WARNING.ordinal();

    private static final AtomicReference<Log> sLog = new AtomicReference<Log>(new SystemLog());

    private static final AtomicReference<LogLevel> sLogLevel =
            new AtomicReference<LogLevel>(LogLevel.ERROR);

    private final int mLevel;

    private final Log mLog;

    /**
     * Constructor.
     *
     * @param log   the log instance.
     * @param level the log level.
     */
    public Logger(final Log log, final LogLevel level) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;
        mLevel = level.ordinal();
    }

    /**
     * Gets the default log instance.
     *
     * @return the log instance.
     */
    public static Log getLog() {

        return sLog.get();
    }

    /**
     * Gets the default log level.
     *
     * @return the log level.
     */
    public static LogLevel getLogLevel() {

        return sLogLevel.get();
    }

    /**
     * Sets the default log level.
     *
     * @param level the log level.
     * @throws NullPointerException if the specified level is null.
     */
    public static void setLogLevel(final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        sLogLevel.set(level);
    }

    /**
     * Sets the default log instance.
     *
     * @param log the log instance.
     * @throws NullPointerException if the specified level is null.
     */
    public static void seLog(final Log log) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        sLog.set(log);
    }

    private static String write(final Throwable t) {

        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    /**
     * Logs a debug message.
     *
     * @param message the message.
     */
    public void dbg(final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(message);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void dbg(final String format, final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void dbg(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void dbg(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void dbg(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void dbg(final String format, final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, args));
        }
    }

    /**
     * Logs a debug exception.
     *
     * @param t the related throwable.
     */
    public void dbg(final Throwable t) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t       the related throwable.
     * @param message the message.
     */
    public void dbg(final Throwable t, final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void dbg(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void dbg(final Throwable t, final String format, final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param message the message.
     */
    public void err(final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(message);
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void err(final String format, final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1));
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void err(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2));
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void err(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3));
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void err(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void err(final String format, final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, args));
        }
    }

    /**
     * Logs an error exception.
     *
     * @param t the related throwable.
     */
    public void err(final Throwable t) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t       the related throwable.
     * @param message the message.
     */
    public void err(final Throwable t, final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void err(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void err(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void err(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void err(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs an error message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void err(final Throwable t, final String format, final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param message the message.
     */
    public void wrn(final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(message);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void wrn(final String format, final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void wrn(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void wrn(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void wrn(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void wrn(final String format, final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, args));
        }
    }

    /**
     * Logs a warning exception.
     *
     * @param t the related throwable.
     */
    public void wrn(final Throwable t) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t       the related throwable.
     * @param message the message.
     */
    public void wrn(final Throwable t, final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void wrn(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     */
    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     * @param arg3   the third format argument.
     * @param arg4   the fourth format argument.
     */
    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param t      the related throwable.
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void wrn(final Throwable t, final String format, final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }
}