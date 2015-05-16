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

import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routine objects based on an invocation class token.<br/>
 * The invocation execution will happen in a dedicated service.
 * <p/>
 * The local context of the invocations will be the specific service instance.
 * <p/>
 * Note that, like the object passed to the service routine input and output channels, the
 * invocation factory arguments must comply with the {@link android.os.Parcel#writeValue(Object)}
 * method. Be aware though, that issues may arise when employing {@link java.io.Serializable}
 * objects on the Lollipop OS version, so, it is advisable to use {@link android.os.Parcelable}
 * objects instead.
 * <p/>
 * Created by davide-maestroni on 3/7/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface ServiceRoutineBuilder<INPUT, OUTPUT> extends RoutineBuilder<INPUT, OUTPUT>,
        ServiceConfigurableBuilder<ServiceRoutineBuilder<INPUT, OUTPUT>> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>> withRoutine();
}
