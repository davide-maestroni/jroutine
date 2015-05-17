/*
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
package com.gh.bmd.jrt.log;

import com.gh.bmd.jrt.log.Log.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class used for logging messages.
 * <p/>
 * Created by davide-maestroni on 10/3/14.
 */
public class Logger {

    private static final int DEBUG_LEVEL = LogLevel.DEBUG.ordinal();

    private static final LogLevel DEFAULT_LEVEL = LogLevel.ERROR;

    private static final SystemLog DEFAULT_LOG = Logs.systemLog();

    private static final int ERROR_LEVEL = LogLevel.ERROR.ordinal();

    private static final int WARNING_LEVEL = LogLevel.WARNING.ordinal();

    private static final AtomicReference<Log> sLog = new AtomicReference<Log>(DEFAULT_LOG);

    private static final AtomicReference<LogLevel> sLogLevel =
            new AtomicReference<LogLevel>(DEFAULT_LEVEL);

    private final List<Object> mContextList;

    private final Object[] mContexts;

    private final int mLevel;

    private final Log mLog;

    private final LogLevel mLogLevel;

    /**
     * Constructor.
     *
     * @param contexts the array of contexts.
     * @param log      the log instance.
     * @param level    the log level.
     */
    private Logger(@Nonnull final Object[] contexts, @Nullable final Log log,
            @Nullable final LogLevel level) {

        mContexts = contexts.clone();
        mLog = (log == null) ? DEFAULT_LOG : log;
        mLogLevel = (level == null) ? DEFAULT_LEVEL : level;
        mLevel = mLogLevel.ordinal();
        mContextList = Arrays.asList(mContexts);
    }

    /**
     * Gets the global log instance.
     *
     * @return the log instance.
     */
    @Nonnull
    public static Log getGlobalLog() {

        return sLog.get();
    }

    /**
     * Sets the global log instance.
     *
     * @param log the log instance.
     */
    public static void setGlobalLog(@Nullable final Log log) {

        sLog.set((log == null) ? DEFAULT_LOG : log);
    }

    /**
     * Gets the global log level.
     *
     * @return the log level.
     */
    @Nonnull
    public static LogLevel getGlobalLogLevel() {

        return sLogLevel.get();
    }

    /**
     * Sets the global log level.
     *
     * @param level the log level.
     */
    public static void setGlobalLogLevel(@Nullable final LogLevel level) {

        sLogLevel.set((level == null) ? DEFAULT_LEVEL : level);
    }

    /**
     * Creates a new logger.
     *
     * @param log     the log instance.
     * @param level   the log level.
     * @param context the context.
     * @return the new logger.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static Logger newLogger(@Nullable final Log log, @Nullable final LogLevel level,
            @Nonnull final Object context) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        return new Logger(new Object[]{context}, log, level);
    }

    /**
     * Prints the stack trace of the specified throwable into a string.
     *
     * @param throwable the throwable instance.
     * @return the printed stack trace.
     */
    @Nonnull
    public static String printStackTrace(@Nonnull final Throwable throwable) {

        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Logs a debug message.
     *
     * @param message the message.
     */
    public void dbg(@Nullable final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, message, null);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void dbg(@Nonnull final String format, @Nullable final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1), null);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void dbg(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2), null);
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
    public void dbg(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2, arg3), null);
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
    public void dbg(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2, arg3, arg4), null);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void dbg(@Nonnull final String format, @Nullable final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, args), null);
        }
    }

    /**
     * Logs a debug exception.
     *
     * @param throwable the related throwable.
     */
    public void dbg(@Nullable final Throwable throwable) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, "", throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param message   the message.
     */
    public void dbg(@Nullable final Throwable throwable, @Nullable final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, message, throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     */
    public void dbg(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1), throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     */
    public void dbg(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2), throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     */
    public void dbg(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2, arg3), throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     * @param arg4      the fourth format argument.
     */
    public void dbg(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
            @Nullable final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, arg1, arg2, arg3, arg4), throwable);
        }
    }

    /**
     * Logs a debug message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param args      the format arguments.
     */
    public void dbg(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(mContextList, String.format(format, args), throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param message the message.
     */
    public void err(@Nullable final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, message, null);
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void err(@Nonnull final String format, @Nullable final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1), null);
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void err(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2), null);
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
    public void err(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2, arg3), null);
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
    public void err(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2, arg3, arg4), null);
        }
    }

    /**
     * Logs an error message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void err(@Nonnull final String format, @Nullable final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, args), null);
        }
    }

    /**
     * Logs an error exception.
     *
     * @param throwable the related throwable.
     */
    public void err(@Nonnull final Throwable throwable) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, "", throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param message   the message.
     */
    public void err(@Nonnull final Throwable throwable, @Nullable final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, message, throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     */
    public void err(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1), throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     */
    public void err(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2), throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     */
    public void err(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2, arg3), throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     * @param arg4      the fourth format argument.
     */
    public void err(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
            @Nullable final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, arg1, arg2, arg3, arg4), throwable);
        }
    }

    /**
     * Logs an error message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param args      the format arguments.
     */
    public void err(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(mContextList, String.format(format, args), throwable);
        }
    }

    /**
     * Returns the list of contexts.
     *
     * @return the list of contexts.
     */
    @Nonnull
    public List<Object> getContextList() {

        return mContextList;
    }

    /**
     * Returns the log instance of this logger.
     *
     * @return the log instance.
     */
    @Nonnull
    public Log getLog() {

        return mLog;
    }

    /**
     * Returns the log level of this logger.
     *
     * @return the log level.
     */
    @Nonnull
    public LogLevel getLogLevel() {

        return mLogLevel;
    }

    /**
     * Creates a new logger with the same log instance and log level, but adding the specified
     * contexts to the contexts array.
     *
     * @param context the context.
     * @return the new logger.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public Logger subContextLogger(@Nonnull final Object context) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        final Object[] thisContexts = mContexts;
        final int thisLength = thisContexts.length;
        final Object[] newContexts = new Object[thisLength + 1];
        System.arraycopy(thisContexts, 0, newContexts, 0, thisLength);
        newContexts[thisLength] = context;
        return new Logger(newContexts, mLog, mLogLevel);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message.
     */
    public void wrn(@Nullable final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, message, null);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     */
    public void wrn(@Nonnull final String format, @Nullable final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1), null);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param arg1   the first format argument.
     * @param arg2   the second format argument.
     */
    public void wrn(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2), null);
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
    public void wrn(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2, arg3), null);
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
    public void wrn(@Nonnull final String format, @Nullable final Object arg1,
            @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2, arg3, arg4), null);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param format the message format.
     * @param args   the format arguments.
     */
    public void wrn(@Nonnull final String format, @Nullable final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, args), null);
        }
    }

    /**
     * Logs a warning exception.
     *
     * @param throwable the related throwable.
     */
    public void wrn(@Nonnull final Throwable throwable) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, "", throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param message   the message.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nullable final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, message, throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1), throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2), throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2, arg3), throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param arg1      the first format argument.
     * @param arg2      the second format argument.
     * @param arg3      the third format argument.
     * @param arg4      the fourth format argument.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
            @Nullable final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, arg1, arg2, arg3, arg4), throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param throwable the related throwable.
     * @param format    the message format.
     * @param args      the format arguments.
     */
    public void wrn(@Nonnull final Throwable throwable, @Nonnull final String format,
            @Nullable final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(mContextList, String.format(format, args), throwable);
        }
    }
}
