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
package com.gh.bmd.jrt.android.invocation;

import javax.annotation.Nonnull;

/**
 * Empty abstract implementation of a context invocation factory.
 * <p/>
 * This class is useful to avoid the need of implementing all the methods defined in the interface.
 * <p/>
 * Created by davide on 28/05/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class TemplateContextInvocationFactory<INPUT, OUTPUT>
        implements ContextInvocationFactory<INPUT, OUTPUT> {

    private final String mInvocationType = getClass().getName();

    @Nonnull
    public String getInvocationType() {

        return mInvocationType;
    }
}
