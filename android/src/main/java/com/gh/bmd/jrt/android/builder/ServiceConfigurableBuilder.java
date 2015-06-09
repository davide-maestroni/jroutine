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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration.Builder;

import javax.annotation.Nonnull;

/**
 * Interface defining a configurable builder of service routines.
 * <p/>
 * Created by davide-maestroni on 01/05/15.
 *
 * @param <TYPE> the builder type.
 */
public interface ServiceConfigurableBuilder<TYPE> {

    /**
     * Gets the service configuration builder related to the routine builder instance.<br/>
     * The configuration options not supported by the routine builder implementation might be
     * ignored.
     * <p/>
     * Note that the configuration builder will be initialized with the current configuration.
     *
     * @return the service configuration builder.
     */
    @Nonnull
    Builder<? extends TYPE> withService();
}
