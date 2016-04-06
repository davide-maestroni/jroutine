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

package com.github.dm.jrt.android;

import com.github.dm.jrt.TargetRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.object.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.object.config.ProxyConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader routine builder acting both as proxy and object builder.
 * <p>
 * The builder will automatically decide whether to employ reflection or code generation to build
 * the proxy instance, based on the presence of the proper annotation and target value. So, if the
 * pre-processor annotation is present in the proxy interface and the target object is assignable to
 * the annotation target class, then code generation will be employed, reflection otherwise.
 * <br>
 * Note that the use of one or the other can be forced by calling the proper method.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
public interface LoaderTargetRoutineBuilder
        extends TargetRoutineBuilder, LoaderObjectRoutineBuilder {

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderTargetRoutineBuilder> invocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends LoaderTargetRoutineBuilder> proxyConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderTargetRoutineBuilder withType(@Nullable BuilderType builderType);

    /**
     * {@inheritDoc}
     */
    @NotNull
    LoaderConfiguration.Builder<? extends LoaderTargetRoutineBuilder> loaderConfiguration();
}
