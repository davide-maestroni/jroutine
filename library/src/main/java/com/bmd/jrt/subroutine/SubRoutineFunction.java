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
package com.bmd.jrt.subroutine;

import com.bmd.jrt.channel.ResultChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public abstract class SubRoutineFunction<INPUT, OUTPUT> extends SubRoutineAdapter<INPUT, OUTPUT> {

    private ArrayList<INPUT> mInputs;

    @Override
    public void onInput(final INPUT input, final ResultChannel<OUTPUT> results) {

        if (mInputs == null) {

            mInputs = new ArrayList<INPUT>();
        }

        mInputs.add(input);
    }

    @Override
    public void onResult(final ResultChannel<OUTPUT> results) {

        final ArrayList<INPUT> inputs = mInputs;
        final ArrayList<INPUT> clone;

        if (inputs == null) {

            clone = new ArrayList<INPUT>(0);

        } else {

            clone = new ArrayList<INPUT>(inputs);
        }

        onRun(clone, results);
    }

    @Override
    public void onReturn() {

        final ArrayList<INPUT> inputs = mInputs;

        if (inputs != null) {

            inputs.clear();
        }
    }

    public abstract void onRun(List<? extends INPUT> inputs, ResultChannel<OUTPUT> results);
}