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

/**
 * Created by davide on 9/11/14.
 */
class ResultFilterRoutine<INPUT, OUTPUT> extends AbstractRoutine<INPUT, OUTPUT> {

    private final AbstractRoutine<INPUT, OUTPUT> mRoutine;

    private final FilteredResultPublisher<OUTPUT> mWrapper;

    public ResultFilterRoutine(final AbstractRoutine<INPUT, OUTPUT> wrapped,
            final ResultFilter<OUTPUT> filter) {

        super(wrapped.getRunner());

        mRoutine = wrapped;
        mWrapper = new FilteredResultPublisher<OUTPUT>(filter);
    }

    @Override
    protected RecyclableUnitProcessor<INPUT, OUTPUT> createProcessor() {

        return new ResultFilterUnitProcessor<INPUT, OUTPUT>(mRoutine.createProcessor(), mWrapper);
    }
}