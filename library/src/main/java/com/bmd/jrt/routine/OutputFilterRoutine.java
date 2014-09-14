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

import com.bmd.jrt.subroutine.SubRoutineLoop;

/**
 * Created by davide on 9/11/14.
 */
class OutputFilterRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final AbstractRoutine<INPUT, OUTPUT> mRoutine;

    private final ResultPublisherWrapper<OUTPUT> mWrapper;

    public OutputFilterRoutine(final AbstractRoutine<INPUT, OUTPUT> wrapped,
            final OutputFilter<OUTPUT> filter) {

        super(wrapped);

        mRoutine = wrapped;
        mWrapper = new ResultPublisherWrapper<OUTPUT>(filter);
    }

    @Override
    protected SubRoutineLoop<INPUT, OUTPUT> createSubRoutine(final boolean async) {

        return new OutputFilterSubRoutineLoop<INPUT, OUTPUT>(mRoutine.createSubRoutine(async),
                                                             mWrapper);
    }
}