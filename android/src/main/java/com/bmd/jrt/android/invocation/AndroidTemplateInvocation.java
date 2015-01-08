/**
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
package com.bmd.jrt.android.invocation;

import android.content.Context;

import com.bmd.jrt.invocation.TemplateInvocation;

import javax.annotation.Nonnull;

/**
 * Template invocation implementing an Android invocation.
 * <p/>
 * Created by davide on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class AndroidTemplateInvocation<INPUT, OUTPUT> extends TemplateInvocation<INPUT, OUTPUT>
        implements AndroidInvocation<INPUT, OUTPUT> {

    private Context mContext;

    @Override
    public void onSetContext(@Nonnull final Context context) {

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
