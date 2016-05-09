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

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for creating and sharing log instances.
 * <p>
 * Created by davide-maestroni on 12/22/2014.
 */
public class Logs {

    private static final NullLog sNullLog = new NullLog();

    private static final SystemLog sSystemLog = new SystemLog();

    /**
     * Avoid explicit instantiation.
     */
    protected Logs() {

        ConstantConditions.avoid();
    }

    /**
     * Returns the null log shared instance.
     *
     * @return the shared instance.
     */
    @NotNull
    public static NullLog nullLog() {

        return sNullLog;
    }

    /**
     * Returns the system output log shared instance.
     *
     * @return the shared instance.
     */
    @NotNull
    public static SystemLog systemLog() {

        return sSystemLog;
    }
}
