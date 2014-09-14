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

import com.bmd.jrt.routine.Routine.OutputFilter;
import com.bmd.jrt.subroutine.ResultPublisher;

/**
 * Created by davide on 9/10/14.
 */
class ResultPublisherWrapper<OUTPUT> implements ResultPublisher<OUTPUT> {

    private final OutputFilter<OUTPUT> mFilter;

    private final Object mMutex = new Object();

    private final ResultPublisher<OUTPUT> mResultPublisher;

    public ResultPublisherWrapper(final OutputFilter<OUTPUT> filter) {

        this(filter, null);
    }

    private ResultPublisherWrapper(final OutputFilter<OUTPUT> filter,
            final ResultPublisher<OUTPUT> results) {

        if (filter == null) {

            throw new IllegalArgumentException();
        }

        mFilter = filter;
        mResultPublisher = results;
    }

    public void end() {

        synchronized (mMutex) {

            mFilter.onEnd(mResultPublisher);
        }
    }

    @Override
    public ResultPublisherWrapper<OUTPUT> publish(final OUTPUT result) {

        synchronized (mMutex) {

            mFilter.onResult(result, mResultPublisher);
        }

        return this;
    }

    @Override
    public ResultPublisherWrapper<OUTPUT> publish(final OUTPUT... results) {

        if (results != null) {

            synchronized (mMutex) {

                final OutputFilter<OUTPUT> filter = mFilter;
                final ResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

                for (final OUTPUT result : results) {

                    filter.onResult(result, resultPublisher);
                }
            }
        }

        return this;
    }

    @Override
    public ResultPublisherWrapper<OUTPUT> publish(final Iterable<? extends OUTPUT> results) {

        if (results != null) {

            synchronized (mMutex) {

                final OutputFilter<OUTPUT> filter = mFilter;
                final ResultPublisher<OUTPUT> resultPublisher = mResultPublisher;

                for (final OUTPUT result : results) {

                    filter.onResult(result, resultPublisher);
                }
            }
        }

        return this;
    }

    @Override
    public ResultPublisherWrapper<OUTPUT> publishException(final Throwable throwable) {

        synchronized (mMutex) {

            mFilter.onException(throwable, mResultPublisher);
        }

        return this;
    }

    public void reset() {

        synchronized (mMutex) {

            mFilter.onReset(mResultPublisher);
        }
    }

    public ResultPublisherWrapper<OUTPUT> wrap(final ResultPublisher<OUTPUT> results) {

        return new ResultPublisherWrapper<OUTPUT>(mFilter, results);
    }
}