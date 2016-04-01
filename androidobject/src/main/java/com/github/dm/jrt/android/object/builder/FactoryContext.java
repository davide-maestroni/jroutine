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

package com.github.dm.jrt.android.object.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface defining a factory providing object instances, whose methods are to be called inside
 * asynchronous invocations.
 * <p>
 * Created by davide-maestroni on 04/06/2015.
 */
public interface FactoryContext {

    /**
     * Returns an instance of the object described by the specified parameters.<br>
     * If a null instance is returned, the constructor matching the specified arguments will be
     * called by default.
     *
     * @param type   the type of the returned instance.
     * @param args   the custom arguments.
     * @param <TYPE> the target object type.
     * @return the object instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @Nullable
    <TYPE> TYPE geInstance(@NotNull Class<? extends TYPE> type, @NotNull Object... args) throws
            Exception;
}
