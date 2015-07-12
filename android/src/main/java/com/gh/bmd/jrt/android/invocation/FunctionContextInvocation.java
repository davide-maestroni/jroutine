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

import android.content.Context;

import com.gh.bmd.jrt.invocation.FunctionInvocation;

import javax.annotation.Nonnull;

/**
 * This is a special abstract implementation that centralizes the routine invocation inside a
 * single method, which gets called only when all the inputs are available.
 * <p/>
 * The implementing class may additionally override the invocation methods to specifically handle
 * the object lifecycle. Note anyway that the superclass must be invoked in order to properly work.
 * <p/>
 * Created by davide-maestroni on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class FunctionContextInvocation<INPUT, OUTPUT>
        extends FunctionInvocation<INPUT, OUTPUT> implements ContextInvocation<INPUT, OUTPUT> {

    private Context mContext;

    public void onContext(@Nonnull final Context context) {

        mContext = context;
    }

    /**
     * Returns this invocation context.
     *
     * @return the context of this invocation.
     */
    protected Context getContext() {

        return mContext;
    }
}