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
package com.github.dm.jrt.log;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Log implementation simply discarding all messages.
 * <p/>
 * Created by davide-maestroni on 10/4/14.
 */
public class NullLog extends TemplateLog {

    @Override
    protected void log(@Nonnull final LogLevel level, @Nonnull final List<Object> contexts,
            @Nullable final String message, @Nullable final Throwable throwable) {

    }
}
