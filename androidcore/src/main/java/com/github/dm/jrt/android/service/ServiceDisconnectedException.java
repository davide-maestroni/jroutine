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

package com.github.dm.jrt.android.service;

import android.content.ComponentName;

import com.github.dm.jrt.core.common.RoutineException;

import org.jetbrains.annotations.Nullable;

/**
 * Exception indicating that the routine service has unexpectedly disconnected.
 * <p/>
 * Created by davide-maestroni on 05/25/2015.
 */
public class ServiceDisconnectedException extends RoutineException {

    private final String mClassName;

    private final String mPackageName;

    /**
     * Constructor.
     */
    public ServiceDisconnectedException() {

        this(null);
    }

    /**
     * Constructor.
     *
     * @param name the service component name.
     */
    public ServiceDisconnectedException(@Nullable final ComponentName name) {

        if (name != null) {
            mPackageName = name.getPackageName();
            mClassName = name.getClassName();

        } else {
            mPackageName = null;
            mClassName = null;
        }
    }

    /**
     * Gets the service component name.
     *
     * @return the component name.
     */
    @Nullable
    public ComponentName getComponentName() {

        if ((mPackageName != null) && (mClassName != null)) {
            return new ComponentName(mPackageName, mClassName);
        }

        return null;
    }
}
