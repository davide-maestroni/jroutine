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

package com.github.dm.jrt.core.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining a log object responsible for formatting and writing the log messages.
 * <p/>
 * A default log instance can be set by invoking the proper logger methods. Note, however, that a
 * routine instance cannot dynamically change its log after creation.
 * <p/>
 * Note also that a log instance is typically accessed from different threads, so, it is
 * responsibility of the implementing class to avoid concurrency issues by synchronizing mutable
 * fields when required.
 * <p/>
 * To avoid an excessive number of log messages, it is sufficient to set an higher log level.
 * Though, it is also possible to completely remove the log source code (and related strings) from
 * the released code by using Proguard and adding, for example, the following rule to the
 * configuration file:
 * <pre>
 *     <code>
 *
 *         -assumenosideeffects class com.github.dm.jrt.core.log.Logger {
 *             public void dbg(...);
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 10/03/2014.
 *
 * @see com.github.dm.jrt.core.log.Logger Logger
 */
public interface Log {

    /**
     * Logs a debug message.
     *
     * @param contexts  the list of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    void dbg(@NotNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Logs an error message.
     *
     * @param contexts  the list of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    void err(@NotNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Logs a warning message.
     *
     * @param contexts  the list of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    void wrn(@NotNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Log levels enumeration from more to less verbose.
     */
    enum Level {

        /**
         * The most verbose log level.<br/>
         * Debug logs are meant to describe in detail what's happening inside the routine.
         */
        DEBUG,
        /**
         * The medium log level.<br/>
         * Warning logs are meant to notify events that are not completely unexpected,
         * but might be a clue that something wrong is happening.
         */
        WARNING,
        /**
         * The least verbose level.<br/>
         * Error logs notify unexpected events that are clearly an exception in the normal code
         * execution.
         */
        ERROR,
        /**
         * Silences all the logs.
         */
        SILENT
    }
}
