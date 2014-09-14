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
package com.bmd.jrt.routine;

import com.bmd.jrt.subroutine.ResultPublisher;
import com.bmd.jrt.subroutine.SubRoutineLoop;

/**
 * Created by davide on 9/11/14.
 */
class OutputFilterSubRoutineLoop<INPUT, OUTPUT> implements SubRoutineLoop<INPUT, OUTPUT> {

    private final ResultPublisherWrapper<OUTPUT> mResultWrapper;

    private final SubRoutineLoop<INPUT, OUTPUT> mRoutine;

    public OutputFilterSubRoutineLoop(final SubRoutineLoop<INPUT, OUTPUT> routine,
            final ResultPublisherWrapper<OUTPUT> wrapper) {

        if (routine == null) {

            throw new IllegalArgumentException();
        }

        mRoutine = routine;
        mResultWrapper = wrapper;
    }

    @Override
    public void onInit() {

        mRoutine.onInit();
    }

    @Override
    public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

        mRoutine.onInput(input, mResultWrapper.wrap(results));
    }

    @Override
    public void onReset(final ResultPublisher<OUTPUT> results) {

        final ResultPublisherWrapper<OUTPUT> resultWrapper = mResultWrapper;

        try {

            mRoutine.onReset(resultWrapper.wrap(results));

        } finally {

            resultWrapper.reset();
        }
    }

    @Override
    public void onResult(final ResultPublisher<OUTPUT> results) {

        final ResultPublisherWrapper<OUTPUT> resultWrapper = mResultWrapper;

        try {

            mRoutine.onResult(resultWrapper.wrap(results));

        } finally {

            resultWrapper.end();
        }
    }

    @Override
    public void onReturn() {

        mRoutine.onReturn();
    }
}