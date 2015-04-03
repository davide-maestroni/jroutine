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

import android.os.Looper;

import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.runner.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p/>
 * Created by davide on 3/29/15.
 */
public interface ObjectServiceRoutineBuilder extends ServiceRoutineBuilder, ObjectRoutineBuilder {

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.<b/>
     * Note that any {@link com.gh.bmd.jrt.annotation.Share} annotation will be ignored.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.<b/>
     * Note that any {@link com.gh.bmd.jrt.annotation.Share} annotation will be ignored.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class token is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder withShareGroup(@Nullable String group);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder dispatchingOn(@Nullable Looper looper);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder withLogClass(@Nullable Class<? extends Log> logClass);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder withRunnerClass(@Nullable Class<? extends Runner> runnerClass);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectServiceRoutineBuilder withServiceClass(
            @Nullable Class<? extends RoutineService> serviceClass);

    /**
     * Sets the arguments to be passed to the constructor of the wrapped instance factory.
     * <p/>
     * Note that, like the object passed to the service routine input and output channels, the
     * specified arguments must comply with the {@link android.os.Parcel#writeValue(Object)} method.
     *
     * @param args the arguments.
     * @return this builder.
     */
    @Nonnull
    ObjectServiceRoutineBuilder withArgs(@Nullable Object... args);
}
