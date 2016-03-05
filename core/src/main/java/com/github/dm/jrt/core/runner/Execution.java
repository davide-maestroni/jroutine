/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.runner;

/**
 * Interface defining an invocation execution.
 * <p/>
 * This interface is meant to be used by a runner to ensure that the routine execution will take
 * place in the specific handled thread or threads.
 * <p/>
 * Created by davide-maestroni on 09/7/2014.
 */
public interface Execution extends Runnable {

    /**
     * Checks if this execution might be canceled in the future.<br/>
     * Note that this is just an hint for the runner: if false is returned, the runner might not be
     * able to cancel the execution when requested. On the other side, the runner may be unable to
     * cancel the execution anyway, based on the internal status or specific implementation.
     *
     * @return whether the execution should be cancelable.
     */
    boolean mayBeCanceled();
}
