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

import javax.annotation.Nonnull;

/**
 * Utility class used to create builders of objects wrapping target ones, so to enable asynchronous
 * calls of their methods.
 * <p/>
 * The builders returned by this class are based on compile time code generation, enabled by
 * pre-processing of Java annotations.<br/>
 * The pre-processing is automatically triggered just by including the artifact of this class
 * module.
 * <p/>
 * Created by davide on 3/23/15.
 *
 * @see com.gh.bmd.jrt.annotation.Bind
 * @see com.gh.bmd.jrt.annotation.Pass
 * @see com.gh.bmd.jrt.annotation.Share
 * @see com.gh.bmd.jrt.annotation.Timeout
 * @see com.gh.bmd.jrt.processor.annotation.Wrap
 */
public class JRoutineProcessor {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineProcessor() {

    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.
     *
     * @param target the target object.
     * @return the routine builder instance.
     * @throws java.lang.NullPointerException if the specified target is null.
     */
    @Nonnull
    public static WrapperRoutineBuilder on(@Nonnull final Object target) {

        return new DefaultWrapperRoutineBuilder(target);
    }
}
