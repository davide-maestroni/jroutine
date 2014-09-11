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

import com.bmd.jrt.process.ResultPublisher;

/**
 * Created by davide on 9/11/14.
 */
class ResultFilterUnitProcessor<INPUT, OUTPUT> implements RecyclableUnitProcessor<INPUT, OUTPUT> {

    private final RecyclableUnitProcessor<INPUT, OUTPUT> mProcessor;

    private final FilteredResultPublisher<OUTPUT> mResultPublisher;

    public ResultFilterUnitProcessor(final RecyclableUnitProcessor<INPUT, OUTPUT> processor,
            final FilteredResultPublisher<OUTPUT> resultPublisher) {

        if (processor == null) {

            throw new IllegalArgumentException();
        }

        mProcessor = processor;
        mResultPublisher = resultPublisher;
    }

    @Override
    public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

        mProcessor.onInput(input, mResultPublisher.wrap(results));
    }

    @Override
    public void onReset(final ResultPublisher<OUTPUT> results) {

        final FilteredResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

        try {

            mProcessor.onReset(resultPublisher.wrap(results));

        } finally {

            resultPublisher.reset();
        }
    }

    @Override
    public void onResult(final ResultPublisher<OUTPUT> results) {

        final FilteredResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

        try {

            mProcessor.onResult(resultPublisher.wrap(results));

        } finally {

            resultPublisher.end();
        }
    }

    @Override
    public void recycle() {

        mProcessor.recycle();
    }
}