/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.core.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for verifying constant conditions.
 * <p/>
 * Created by davide-maestroni on 03/27/2016.
 */
public class ConstantConditions {

    /**
     * Avoid explicit instantiation.
     */
    protected ConstantConditions() {

    }

    /**
     * Assert that the specified object is not null.
     *
     * @param object the object.
     * @param <TYPE> the object type.
     * @return the object.
     * @throws java.lang.NullPointerException is the object is null.
     */
    @NotNull
    public static <TYPE> TYPE notNull(final TYPE object) {

        return notNull("object", object);
    }

    /**
     * Assert that the specified object is not null.
     *
     * @param name   the name of the object used to build the error message.
     * @param object the object.
     * @param <TYPE> the object type.
     * @return the object.
     * @throws java.lang.NullPointerException is the object is null.
     */
    @NotNull
    public static <TYPE> TYPE notNull(final String name, final TYPE object) {

        if (object == null) {
            throw new NullPointerException(String.format("the %s must not be null", name));
        }

        return object;
    }
}
