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
 * Created by davide on 9/11/14.
 */
public abstract class OutputFilterAdapter<RESULT> implements OutputFilter<RESULT> {

    @Override
    public void onEnd(final ResultPublisher<RESULT> results) {

    }

    @Override
    public void onException(final Throwable throwable, final ResultPublisher<RESULT> results) {

        results.publishException(throwable);
    }

    @Override
    public void onReset(final ResultPublisher<RESULT> results) {

    }

    @Override
    public void onResult(final RESULT result, final ResultPublisher<RESULT> results) {

        results.publish(result);
    }
}