/*
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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Interface defining a method invocation through a routine instance.
 * <p/>
 * Created by davide-maestroni on 12/08/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public interface MethodInvocation<INPUT, OUTPUT> {

    /**
     * Called when the method is invoked.
     *
     * @param inputs the input list.
     * @param result the result channel.
     */
    void onInvocation(@Nonnull List<? extends INPUT> inputs, @Nonnull ResultChannel<OUTPUT> result);
}
