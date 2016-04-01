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
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Log implementation backed by a set of log objects.
 * <p>
 * Created by davide-maestroni on 12/28/2015.
 */
public class LogSet extends CopyOnWriteArraySet<Log> implements Log {

    private static final long serialVersionUID = -1;

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        for (final Log log : this) {
            log.dbg(contexts, message, throwable);
        }
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        for (final Log log : this) {
            log.err(contexts, message, throwable);
        }
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        for (final Log log : this) {
            log.wrn(contexts, message, throwable);
        }
    }
}
