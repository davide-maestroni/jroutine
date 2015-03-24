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

import com.gh.bmd.jrt.invocation.Invocation;

import javax.annotation.Nonnull;

/**
 * Interface defining an invocation aware of the specific Android context.
 * <p/>
 * Created by davide on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface AndroidInvocation<INPUT, OUTPUT> extends Invocation<INPUT, OUTPUT> {

    /**
     * Called right after the instantiation to specify the invocation context.
     *
     * @param context the context of the invocation.
     */
    void onContext(@Nonnull Context context);
}
