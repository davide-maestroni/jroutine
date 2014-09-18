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

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.RoutineChannel;

import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public interface Routine<INPUT, OUTPUT> {

    public List<OUTPUT> call();

    public List<OUTPUT> call(INPUT input);

    public List<OUTPUT> call(INPUT... inputs);

    public List<OUTPUT> call(Iterable<? extends INPUT> inputs);

    public List<OUTPUT> call(OutputChannel<? extends INPUT> inputs);

    public List<OUTPUT> callAsyn();

    public List<OUTPUT> callAsyn(INPUT input);

    public List<OUTPUT> callAsyn(INPUT... inputs);

    public List<OUTPUT> callAsyn(Iterable<? extends INPUT> inputs);

    public List<OUTPUT> callAsyn(OutputChannel<? extends INPUT> inputs);

    public RoutineChannel<INPUT, OUTPUT> launch();

    public RoutineChannel<INPUT, OUTPUT> launchAsyn();

    public RoutineChannel<INPUT, OUTPUT> launchPar();

    public OutputChannel<OUTPUT> run();

    public OutputChannel<OUTPUT> run(INPUT input);

    public OutputChannel<OUTPUT> run(INPUT... inputs);

    public OutputChannel<OUTPUT> run(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> run(OutputChannel<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> runAsyn();

    public OutputChannel<OUTPUT> runAsyn(INPUT input);

    public OutputChannel<OUTPUT> runAsyn(INPUT... inputs);

    public OutputChannel<OUTPUT> runAsyn(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> runAsyn(OutputChannel<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> runPar();

    public OutputChannel<OUTPUT> runPar(INPUT input);

    public OutputChannel<OUTPUT> runPar(INPUT... inputs);

    public OutputChannel<OUTPUT> runPar(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> runPar(OutputChannel<? extends INPUT> inputs);
}