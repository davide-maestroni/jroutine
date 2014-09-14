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

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.subroutine.ResultPublisher;

import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public interface Routine<INPUT, OUTPUT> {

    public List<OUTPUT> asynCall();

    public List<OUTPUT> asynCall(INPUT input);

    public List<OUTPUT> asynCall(INPUT... inputs);

    public List<OUTPUT> asynCall(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> asynRun();

    public OutputChannel<OUTPUT> asynRun(INPUT input);

    public OutputChannel<OUTPUT> asynRun(INPUT... inputs);

    public OutputChannel<OUTPUT> asynRun(Iterable<? extends INPUT> inputs);

    public InputChannel<INPUT, OUTPUT> asynStart();

    public List<OUTPUT> call();

    public List<OUTPUT> call(INPUT input);

    public List<OUTPUT> call(INPUT... inputs);

    public List<OUTPUT> call(Iterable<? extends INPUT> inputs);

    public <TRANSFORMED> Routine<INPUT, TRANSFORMED> onResult(Routine<OUTPUT, TRANSFORMED> routine);

    public Routine<INPUT, OUTPUT> onResult(OutputFilter<OUTPUT> filter);

    public OutputChannel<OUTPUT> run();

    public OutputChannel<OUTPUT> run(INPUT input);

    public OutputChannel<OUTPUT> run(INPUT... inputs);

    public OutputChannel<OUTPUT> run(Iterable<? extends INPUT> inputs);

    public InputChannel<INPUT, OUTPUT> start();

    public interface OutputFilter<OUTPUT> {

        public void onEnd(ResultPublisher<OUTPUT> results);

        public void onException(Throwable throwable, ResultPublisher<OUTPUT> results);

        public void onReset(ResultPublisher<OUTPUT> results);

        public void onResult(OUTPUT result, ResultPublisher<OUTPUT> results);
    }
}