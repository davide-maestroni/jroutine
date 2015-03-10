/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * This is a special abstract implementation that centralizes the routine invocation inside a
 * single method, which gets called only when all the inputs are available.
 * <p/>
 * The implementing class may additionally override the invocation methods to specifically handle
 * the object lifecycle. Note anyway that the super class must be invoked in order to properly
 * work.
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class SingleCallInvocation<INPUT, OUTPUT>
        extends TemplateInvocation<INPUT, OUTPUT> {

    private ArrayList<INPUT> mInputs;

    /**
     * This method is called when all the inputs are available and ready to be processed.
     *
     * @param inputs the input list.
     * @param result the result channel.
     */
    public abstract void onCall(@Nonnull List<? extends INPUT> inputs,
            @Nonnull ResultChannel<OUTPUT> result);

    @Override
    public void onInput(final INPUT input, @Nonnull final ResultChannel<OUTPUT> result) {

        if (mInputs == null) {

            mInputs = new ArrayList<INPUT>();
        }

        mInputs.add(input);
    }

    @Override
    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        final ArrayList<INPUT> inputs = mInputs;
        final ArrayList<INPUT> clone;

        if (inputs == null) {

            clone = new ArrayList<INPUT>(0);

        } else {

            clone = new ArrayList<INPUT>(inputs);
        }

        onCall(clone, result);
    }

    @Override
    public void onReturn() {

        final ArrayList<INPUT> inputs = mInputs;

        if (inputs != null) {

            inputs.clear();
        }
    }
}
