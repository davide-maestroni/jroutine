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

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Interface defining a log object responsible for formatting and writing the framework log
 * messages.
 * <p/>
 * A default global log instance can be set by invoking the proper logger methods. Note, however,
 * that a routine instance cannot dynamically change its log after creation.
 * <p/>
 * Note also that a log instance is typically accessed from different threads, so, it is
 * responsibility of the implementing class to avoid concurrency issues by synchronizing mutable
 * fields when needed.
 * <p/>
 * To avoid an excessive number of log messages it is sufficient to set an higher log level.
 * Though, it is also possible to completely remove the log source code (and related strings) from
 * the released code, by using Proguard and adding, for example, the following rule to the
 * configuration file:
 * <pre>
 *     <code>
 *
 *         -assumenosideeffects class com.bmd.jrt.log.Logger {
 *             public void dbg(...);
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 10/3/14.
 *
 * @see Logger
 */
public interface Log {

    /**
     * Logs a debug message.
     *
     * @param contexts  the array of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    public void dbg(@NonNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Logs an error message.
     *
     * @param contexts  the array of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    public void err(@NonNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Logs a warning message.
     *
     * @param contexts  the array of contexts.
     * @param message   the message.
     * @param throwable the optional throwable or null.
     */
    public void wrn(@NonNull List<Object> contexts, @Nullable String message,
            @Nullable Throwable throwable);

    /**
     * Log levels enumeration from more to less verbose.
     */
    public enum LogLevel {

        DEBUG,
        WARNING,
        ERROR,
        SILENT
    }
}