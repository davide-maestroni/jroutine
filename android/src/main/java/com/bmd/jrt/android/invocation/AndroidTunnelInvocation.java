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
package com.bmd.jrt.android.invocation;

import com.bmd.jrt.channel.ResultChannel;

import javax.annotation.Nonnull;

/**
 * Implementation of an invocation simply passing on the input data.
 * <p/>
 * Created by davide on 1/12/14.
 *
 * @param <DATA> the data type.
 */
public class AndroidTunnelInvocation<DATA> extends AndroidTemplateInvocation<DATA, DATA> {

    @Override
    public void onInput(final DATA input, @Nonnull final ResultChannel<DATA> result) {

        result.pass(input);
    }
}