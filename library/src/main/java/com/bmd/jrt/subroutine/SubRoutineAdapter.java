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
package com.bmd.jrt.subroutine;

import com.bmd.jrt.channel.ResultChannel;

/**
 * Created by davide on 9/11/14.
 */
public abstract class SubRoutineAdapter<INPUT, OUTPUT> implements SubRoutine<INPUT, OUTPUT> {

    @Override
    public void onInit() {

    }

    @Override
    public void onInput(final INPUT input, final ResultChannel<OUTPUT> results) {

    }

    @Override
    public void onReset(final Throwable throwable) {

    }

    @Override
    public void onResult(final ResultChannel<OUTPUT> results) {

    }

    @Override
    public void onReturn() {

    }
}