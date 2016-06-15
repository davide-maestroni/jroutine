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

/**
 * Simple log implementation writing messages to the system output.
 * <p>
 * Created by davide-maestroni on 10/03/2014.
 */
public class SystemLog extends TemplateLog {

    @Override
    public void log(@NotNull final String message) {
        System.out.println(message);
    }
}
