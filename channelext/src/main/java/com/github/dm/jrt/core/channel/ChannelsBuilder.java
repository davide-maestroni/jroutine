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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.builder.ChannelConfigurableBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a generic configurable builder.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <TYPE> the built object type.
 */
public interface ChannelsBuilder<TYPE> extends ChannelConfigurableBuilder<ChannelsBuilder<TYPE>> {

    /**
     * Builds and returns an object instance.
     *
     * @return the object instance.
     */
    @NotNull
    TYPE build();
}
