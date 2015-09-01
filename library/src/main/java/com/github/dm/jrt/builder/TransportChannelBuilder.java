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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.channel.TransportChannel;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of transport channel objects.
 * <p/>
 * Created by davide-maestroni on 03/07/2015.
 */
public interface TransportChannelBuilder
        extends ConfigurableChannelBuilder<TransportChannelBuilder> {

    /**
     * Builds and returns the transport channel instance.
     *
     * @param <DATA> the data type.
     * @return the newly created channel.
     */
    @Nonnull
    <DATA> TransportChannel<DATA> buildChannel();
}
