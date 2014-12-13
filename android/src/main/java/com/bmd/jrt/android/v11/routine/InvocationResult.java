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
package com.bmd.jrt.android.v11.routine;

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.common.RoutineException;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Class storing the invocation loader result.
 * <p/>
 * Created by davide on 12/12/14.
 *
 * @param <OUTPUT> the output data type.
 */
class InvocationResult<OUTPUT> {

    private final RoutineException mException;

    private final List<OUTPUT> mOutputs;

    /**
     * Constructor.
     *
     * @param outputs the output data.
     */
    InvocationResult(@Nonnull final List<OUTPUT> outputs) {

        mException = null;
        mOutputs = outputs;
    }

    /**
     * Constructor.
     *
     * @param exception the abort exception.
     */
    InvocationResult(@Nonnull final RoutineException exception) {

        mException = exception;
        mOutputs = null;
    }

    /**
     * Checks if this result represents an error.
     *
     * @return whether the result is an error.
     */
    boolean isError() {

        return (mException != null);
    }

    /**
     * Passes the result to the specified channel.
     *
     * @param channel the input channel.
     */
    void passTo(@Nonnull final InputChannel<OUTPUT> channel) {

        if (isError()) {

            channel.abort(mException);

        } else {

            try {

                channel.pass(mOutputs);

            } catch (final RoutineException ignored) {

            }
        }
    }
}
