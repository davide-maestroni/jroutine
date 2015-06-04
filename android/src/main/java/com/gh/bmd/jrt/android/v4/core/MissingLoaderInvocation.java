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
package com.gh.bmd.jrt.android.v4.core;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.MissingInvocationException;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.channel.ResultChannel;

import javax.annotation.Nonnull;

/**
 * Invocation used to know whether a loader with a specific ID is present or not.
 * <p/>
 * Created by davide-maestroni on 1/14/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
final class MissingLoaderInvocation<INPUT, OUTPUT> extends TemplateContextInvocation<INPUT, OUTPUT>
        implements ContextInvocationFactory<INPUT, OUTPUT> {

    /**
     * The invocation type.
     */
    static final String TYPE = MissingLoaderInvocation.class.getName();

    private final int mId;

    /**
     * Constructor.
     *
     * @param id the loader ID.
     */
    private MissingLoaderInvocation(final int id) {

        mId = id;
    }

    /**
     * Returns a factory of missing loader invocations.
     *
     * @param id       the loader ID.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the factory.
     */
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryOf(final int id) {

        return new MissingLoaderInvocation<INPUT, OUTPUT>(id);
    }

    @Nonnull
    public String getInvocationType() {

        return TYPE;
    }

    @Nonnull
    public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

        return this;
    }

    @Override
    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        result.abort(new MissingInvocationException(mId));
    }
}
