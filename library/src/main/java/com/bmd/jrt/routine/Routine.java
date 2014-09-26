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
 * TODO
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input type.
 * @param <OUTPUT> the output type.
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

    public List<OUTPUT> callParall();

    public List<OUTPUT> callParall(INPUT input);

    public List<OUTPUT> callParall(INPUT... inputs);

    public List<OUTPUT> callParall(Iterable<? extends INPUT> inputs);

    public List<OUTPUT> callParall(OutputChannel<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invoke();

    public OutputChannel<OUTPUT> invoke(INPUT input);

    public OutputChannel<OUTPUT> invoke(INPUT... inputs);

    public OutputChannel<OUTPUT> invoke(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invoke(OutputChannel<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invokeAsyn();

    public OutputChannel<OUTPUT> invokeAsyn(INPUT input);

    public OutputChannel<OUTPUT> invokeAsyn(INPUT... inputs);

    public OutputChannel<OUTPUT> invokeAsyn(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invokeAsyn(OutputChannel<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invokeParall();

    public OutputChannel<OUTPUT> invokeParall(INPUT input);

    public OutputChannel<OUTPUT> invokeParall(INPUT... inputs);

    public OutputChannel<OUTPUT> invokeParall(Iterable<? extends INPUT> inputs);

    public OutputChannel<OUTPUT> invokeParall(OutputChannel<? extends INPUT> inputs);

    public RoutineChannel<INPUT, OUTPUT> launch();

    public RoutineChannel<INPUT, OUTPUT> launchAsyn();

    public RoutineChannel<INPUT, OUTPUT> launchParall();
}