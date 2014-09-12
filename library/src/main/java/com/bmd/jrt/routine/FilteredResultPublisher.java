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

import com.bmd.jrt.procedure.ResultPublisher;
import com.bmd.jrt.routine.Routine.ResultFilter;

/**
 * Created by davide on 9/10/14.
 */
class FilteredResultPublisher<OUTPUT> implements ResultPublisher<OUTPUT> {

    private final ResultFilter<OUTPUT> mFilter;

    private ResultPublisher<OUTPUT> mResultPublisher;

    public FilteredResultPublisher(final ResultFilter<OUTPUT> filter) {

        if (filter == null) {

            throw new IllegalArgumentException();
        }

        mFilter = filter;
    }

    public void end() {

        mFilter.onEnd(mResultPublisher);
    }

    @Override
    public FilteredResultPublisher<OUTPUT> publish(final OUTPUT result) {

        mFilter.onResult(result, mResultPublisher);

        return this;
    }

    @Override
    public FilteredResultPublisher<OUTPUT> publish(final OUTPUT... results) {

        if (results != null) {

            final ResultFilter<OUTPUT> filter = mFilter;
            final ResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

            for (final OUTPUT result : results) {

                filter.onResult(result, resultPublisher);
            }
        }

        return this;
    }

    @Override
    public FilteredResultPublisher<OUTPUT> publish(final Iterable<? extends OUTPUT> results) {

        if (results != null) {

            final ResultFilter<OUTPUT> filter = mFilter;
            final ResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

            for (final OUTPUT result : results) {

                filter.onResult(result, resultPublisher);
            }
        }

        return this;
    }

    @Override
    public FilteredResultPublisher<OUTPUT> publishException(final Throwable throwable) {

        mFilter.onException(throwable, mResultPublisher);

        return this;
    }

    public void reset() {

        mFilter.onReset(mResultPublisher);
    }

    public FilteredResultPublisher<OUTPUT> wrap(final ResultPublisher<OUTPUT> resultPublisher) {

        mResultPublisher = resultPublisher;

        return this;
    }
}