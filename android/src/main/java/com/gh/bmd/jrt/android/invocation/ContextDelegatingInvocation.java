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

import com.gh.bmd.jrt.invocation.DelegatingInvocation;
import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;

/**
 * Created by davide on 19/04/15.
 */
public class ContextDelegatingInvocation<INPUT, OUTPUT> extends DelegatingInvocation<INPUT, OUTPUT>
        implements ContextInvocation<INPUT, OUTPUT> {

    public ContextDelegatingInvocation(@Nonnull final Routine<INPUT, OUTPUT> routine) {

        super(routine);
    }

    public void onContext(@Nonnull final Context context) {

    }
}
