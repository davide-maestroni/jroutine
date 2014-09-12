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

import com.bmd.jrt.procedure.LoopProcedure;
import com.bmd.jrt.procedure.ResultPublisher;

/**
 * Created by davide on 9/11/14.
 */
class ResultFilterLoopProcedure<INPUT, OUTPUT> implements LoopProcedure<INPUT, OUTPUT> {

    private final LoopProcedure<INPUT, OUTPUT> mLoopProcedure;

    private final FilteredResultPublisher<OUTPUT> mResultPublisher;

    public ResultFilterLoopProcedure(final LoopProcedure<INPUT, OUTPUT> procedure,
            final FilteredResultPublisher<OUTPUT> resultPublisher) {

        if (procedure == null) {

            throw new IllegalArgumentException();
        }

        mLoopProcedure = procedure;
        mResultPublisher = resultPublisher;
    }

    @Override
    public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

        mLoopProcedure.onInput(input, mResultPublisher.wrap(results));
    }

    @Override
    public void onReset(final ResultPublisher<OUTPUT> results) {

        final FilteredResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

        try {

            mLoopProcedure.onReset(resultPublisher.wrap(results));

        } finally {

            resultPublisher.reset();
        }
    }

    @Override
    public void onResult(final ResultPublisher<OUTPUT> results) {

        final FilteredResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

        try {

            mLoopProcedure.onResult(resultPublisher.wrap(results));

        } finally {

            resultPublisher.end();
        }
    }
}