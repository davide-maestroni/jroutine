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
package com.bmd.wtf.example5;

import com.bmd.wtf.example3.RetryPolicy;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.io.IOException;

/**
 * This class is meant to retry the discharge of data in case an error occurred.<br/>
 * The abort exception is treated in a different way to ensure that the message is properly
 * handled through the waterfall.
 */
public class CancelableRetryPolicy extends RetryPolicy {

    public CancelableRetryPolicy(final Spring<String> spring, final int maxCount) {

        super(spring, maxCount);
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof AbortException) {

            final String url = ((AbortException) debris).getMessage();

            onReset(url);

            gate.drop(new IOException(url));

        } else {

            super.onDrop(gate, debris);
        }
    }
}