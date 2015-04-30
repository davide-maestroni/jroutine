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

import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p/>
 * Note that, like the object passed to the service routine input and output channels, the
 * object factory arguments must comply with the {@link android.os.Parcel#writeValue(Object)}
 * method. Be aware though, that issues may arise when employing {@link java.io.Serializable}
 * objects on the Lollipop OS version, so, it is advisable to use {@link android.os.Parcelable}
 * objects instead.
 * <p/>
 * Created by davide on 3/29/15.
 */
public interface ServiceObjectRoutineBuilder
        extends ObjectRoutineBuilder, ServiceConfigurableBuilder<ServiceObjectRoutineBuilder> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withProxyConfiguration();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    RoutineConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withRoutineConfiguration();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ServiceConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withServiceConfiguration();
}
