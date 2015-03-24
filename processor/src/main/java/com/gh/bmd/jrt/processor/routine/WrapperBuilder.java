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
package com.gh.bmd.jrt.processor.routine;

import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.routine.SharableBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of async wrapper objects.
 * <p/>
 * Created by davide on 3/7/15.
 *
 * @param <TYPE> the interface type.
 */
public interface WrapperBuilder<TYPE> extends SharableBuilder {

    /**
     * Returns a wrapper object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout}
     * and {@link com.gh.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.gh.bmd.jrt.processor.annotation.Wrap}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor artifact
     * to the specific project dependencies.
     *
     * @return the wrapping object.
     */
    @Nonnull
    TYPE buildWrapper();

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    WrapperBuilder<TYPE> withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    WrapperBuilder<TYPE> withShareGroup(@Nullable String group);
}
