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

    public Logger(final Log log, final LogLevel level) {

        if (log == null) {

            throw new NullPointerException("the log instance must not be null");
        }

        mLog = log;
        mLevel = level.ordinal();
    }

    public static Log getLog() {

        return sLog.get();
    }

    public static LogLevel getLogLevel() {

        return sLogLevel.get();
    }

    public static void setLogLevel(final LogLevel level) {

        if (level == null) {

            throw new NullPointerException("the log level must not be null");
        }

        sLogLevel.set(level);
    }

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

    public void dbg(final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(message);
        }
    }

    public void dbg(final String format, final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1));
        }
    }

    public void dbg(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2));
        }
    }

    public void dbg(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3));
        }
    }

    public void dbg(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    public void dbg(final String format, final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, args));
        }
    }

    public void dbg(final Throwable t, final String message) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void dbg(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void dbg(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void dbg(final Throwable t, final String format, final Object... args) {

        if (mLevel <= DEBUG_LEVEL) {

            mLog.dbg(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(message);
        }
    }

    public void err(final String format, final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1));
        }
    }

    public void err(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2));
        }
    }

    public void err(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3));
        }
    }

    public void err(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    public void err(final String format, final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, args));
        }
    }

    public void err(final Throwable t, final String message) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void err(final Throwable t, final String format, final Object... args) {

        if (mLevel <= ERROR_LEVEL) {

            mLog.err(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(message);
        }
    }

    public void wrn(final String format, final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1));
        }
    }

    public void wrn(final String format, final Object arg1, final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2));
        }
    }

    public void wrn(final String format, final Object arg1, final Object arg2, final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3));
        }
    }

    public void wrn(final String format, final Object arg1, final Object arg2, final Object arg3,
            final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3, arg4));
        }
    }

    public void wrn(final String format, final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, args));
        }
    }

    public void wrn(final Throwable t, final String message) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(message + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final Throwable t, final String format, final Object arg1) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final Throwable t, final String format, final Object arg1, final Object arg2,
            final Object arg3, final Object arg4) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, arg1, arg2, arg3, arg4) + EXCEPTION_MESSAGE + write(t));
        }
    }

    public void wrn(final Throwable t, final String format, final Object... args) {

        if (mLevel <= WARNING_LEVEL) {

            mLog.wrn(String.format(format, args) + EXCEPTION_MESSAGE + write(t));
        }
    }
}