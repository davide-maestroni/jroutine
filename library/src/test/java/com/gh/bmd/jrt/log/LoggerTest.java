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

import org.junit.Test;

import java.util.List;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Logger unit tests.
 * <p/>
 * Created by davide-maestroni on 10/4/14.
 */
public class LoggerTest {

    private static final String[] ARGS = new String[]{"test1", "test2", "test3", "test4", "test5"};

    private static final String FORMAT0 = "0: %s";

    private static final String FORMAT1 = "0: %s - 1: %s";

    private static final String FORMAT2 = "0: %s - 1: %s - 2: %s";

    private static final String FORMAT3 = "0: %s - 1: %s - 2: %s - 3: %s";

    private static final String FORMAT4 = "0: %s - 1: %s - 2: %s - 3: %s - 4: %s";

    @Test
    public void testDefaultLog() {

        final NullLog log = Logs.nullLog();
        Logger.setDefaultLog(log);
        assertThat(Logger.getDefaultLog()).isEqualTo(log);

        final Logger logger = Logger.newLogger(null, LogLevel.DEBUG, this);
        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    public void testDefaultLogLevel() {

        final LogLevel logLevel = LogLevel.SILENT;
        Logger.setDefaultLogLevel(logLevel);
        assertThat(Logger.getDefaultLogLevel()).isEqualTo(logLevel);

        final NullLog log = Logs.nullLog();
        final Logger logger = Logger.newLogger(log, null, this);
        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(logLevel);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            Logger.setDefaultLog(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        assertThat(Logger.getDefaultLog()).isNotNull();

        try {

            Logger.newLogger(new NullLog(), LogLevel.DEBUG, this).subContextLogger(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Logger.setDefaultLogLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        assertThat(Logger.getDefaultLogLevel()).isNotNull();

        Logger.newLogger(new NullLog(), LogLevel.DEBUG, this).err((Throwable) null);
    }

    @Test
    public void testLoggerDebug() {

        final NullPointerException ex = new NullPointerException();
        final TestLog log = new TestLog();
        final Logger logger = Logger.newLogger(log, LogLevel.DEBUG, this);

        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(LogLevel.DEBUG);

        // - DBG
        logger.dbg(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.dbg(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.dbg(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).isNotEmpty();

        logger.dbg(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.dbg(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        // - WRN
        logger.wrn(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.wrn(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.wrn(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).isNotEmpty();

        logger.wrn(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.wrn(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        // - ERR
        logger.err(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.err(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).isNotEmpty();

        logger.err(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));
    }

    @Test
    public void testLoggerError() {

        final NullPointerException ex = new NullPointerException();
        final TestLog log = new TestLog();
        final Logger logger = Logger.newLogger(log, LogLevel.ERROR, this);

        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(LogLevel.ERROR);

        // - DBG
        logger.dbg(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        // - WRN
        logger.wrn(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        // - ERR
        logger.err(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.err(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).isNotEmpty();

        logger.err(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));
    }

    @Test
    public void testLoggerSilent() {

        final NullPointerException ex = new NullPointerException();
        final TestLog log = new TestLog();
        final Logger logger = Logger.newLogger(log, LogLevel.SILENT, this);

        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(LogLevel.SILENT);

        // - DBG
        logger.dbg(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        // - WRN
        logger.wrn(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        // - ERR
        logger.err(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();
    }

    @Test
    public void testLoggerWarning() {

        final NullPointerException ex = new NullPointerException();
        final TestLog log = new TestLog();
        final Logger logger = Logger.newLogger(log, LogLevel.WARNING, this);

        assertThat(logger.getLog()).isEqualTo(log);
        assertThat(logger.getLogLevel()).isEqualTo(LogLevel.WARNING);

        // - DBG
        logger.dbg(ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isNull();
        assertThat(log.getMessage()).isNull();

        // - WRN
        logger.wrn(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.wrn(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.wrn(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).isNotEmpty();

        logger.wrn(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.wrn(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        // - ERR
        logger.err(ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

        logger.err(ex);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).isNotEmpty();

        logger.err(ex, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(ARGS[0]);

        logger.err(ex, FORMAT0, ARGS[0]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

        logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

        logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

        logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

        logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
        assertThat(log.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(log.getMessage()).contains(
                String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));
    }

    @Test
    public void testSubContext() {

        final TestLog log = new TestLog();
        final Logger logger = Logger.newLogger(log, LogLevel.WARNING, "ctx1");
        final Logger subLogger = logger.subContextLogger("ctx2");

        assertThat(logger.getContextList()).containsExactly("ctx1");
        assertThat(subLogger.getContextList()).containsExactly("ctx1", "ctx2");

        logger.wrn("test1");
        assertThat(log.getMessage()).contains("ctx1");
        assertThat(log.getMessage()).contains("test1");
        assertThat(log.getMessage()).doesNotContain("test2");
        assertThat(log.getMessage()).doesNotContain("ctx2");

        subLogger.wrn("test2");
        assertThat(log.getMessage()).contains("ctx1");
        assertThat(log.getMessage()).doesNotContain("test1");
        assertThat(log.getMessage()).contains("test2");
        assertThat(log.getMessage()).contains("ctx2");
    }

    private static class TestLog extends TemplateLog {

        private LogLevel mLevel;

        private String mMessage;

        public LogLevel getLevel() {

            return mLevel;
        }

        public String getMessage() {

            return mMessage;
        }

        @Override
        protected void log(@Nonnull final LogLevel level, @Nonnull final List<Object> contexts,
                final String message, final Throwable throwable) {

            mLevel = level;

            super.log(level, contexts, message, throwable);
        }

        @Override
        protected void log(@Nonnull final String message) {

            mMessage = message;
        }
    }
}
