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

import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;

import javax.annotation.Nonnull;

/**
 * // TODO: 20/08/15 javadoc
 *
 * Created by davide-maestroni on 20/08/15.
 */
public interface ServiceClassRoutineBuilder
        extends ClassRoutineBuilder, ServiceConfigurableBuilder<ServiceClassRoutineBuilder> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationConfiguration.Builder<? extends ServiceClassRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends ServiceClassRoutineBuilder> proxies();
}
