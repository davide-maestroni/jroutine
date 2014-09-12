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
import com.bmd.jrt.procedure.LoopProcedure;
import com.bmd.jrt.procedure.ResultPublisher;
import com.bmd.jrt.routine.DefaultRoutineChannel.ProcedureProvider;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public abstract class AbstractRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    private final int mMaxInstancePerCall;

    private final int mMaxInstanceRecycled;

    private final Object mMutex = new Object();

    private final Runner mRunner;

    private LinkedList<RecyclableLoopProcedure> mProcedureList =
            new LinkedList<RecyclableLoopProcedure>();

    public AbstractRoutine(final Runner runner, final int maxPerCall, final int maxRecycled) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxPerCall < 1) {

            throw new IllegalArgumentException();
        }

        if (maxRecycled < 1) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mMaxInstancePerCall = maxPerCall;
        mMaxInstanceRecycled = maxRecycled;
    }

    public AbstractRoutine(final AbstractRoutine<?, ?> other) {

        this(other.mRunner, other.mMaxInstancePerCall, other.mMaxInstanceRecycled);
    }

    @Override
    public List<OUTPUT> asynCall() {

        return asynStart().end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final INPUT input) {

        return asynStart().push(input).end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final INPUT... inputs) {

        return asynStart().push(inputs).end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final Iterable<? extends INPUT> inputs) {

        return asynStart().push(inputs).end().all();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun() {

        return asynStart().end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final INPUT input) {

        return asynStart().push(input).end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final INPUT... inputs) {

        return asynStart().push(inputs).end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final Iterable<? extends INPUT> inputs) {

        return asynStart().push(inputs).end();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> asynStart() {

        return start(true);
    }

    @Override
    public List<OUTPUT> call() {

        return start().end().all();
    }

    @Override
    public List<OUTPUT> call(final INPUT input) {

        return start().push(input).end().all();
    }

    @Override
    public List<OUTPUT> call(final INPUT... inputs) {

        return start().push(inputs).end().all();
    }

    @Override
    public List<OUTPUT> call(final Iterable<? extends INPUT> inputs) {

        return start().push(inputs).end().all();
    }

    @Override
    public <TRANSFORMED> Routine<INPUT, TRANSFORMED> onResult(
            final Routine<OUTPUT, TRANSFORMED> routine) {

        return new ResultRoutine<INPUT, OUTPUT, TRANSFORMED>(this, routine);
    }

    @Override
    public Routine<INPUT, OUTPUT> onResult(final ResultFilter<OUTPUT> filter) {

        return new ResultFilterRoutine<INPUT, OUTPUT>(this, filter);
    }

    @Override
    public OutputChannel<OUTPUT> run() {

        return start().end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT input) {

        return start().push(input).end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT... inputs) {

        return start().push(inputs).end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final Iterable<? extends INPUT> inputs) {

        return start().push(inputs).end();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> start() {

        return start(false);
    }

    protected abstract LoopProcedure<INPUT, OUTPUT> createProcedure(final boolean async);

    protected InputChannel<INPUT, OUTPUT> start(final boolean async) {

        return new DefaultRoutineChannel<INPUT, OUTPUT>(new RoutineProcedureProvider(async),
                                                        (async) ? mRunner : Runners.sync(),
                                                        mMaxInstancePerCall);
    }

    private RecyclableLoopProcedure getRecyclableProcedure(final boolean async) {

        synchronized (mMutex) {

            final LinkedList<RecyclableLoopProcedure> procedures = mProcedureList;

            if (!procedures.isEmpty()) {

                return procedures.removeFirst();
            }

            return new RecyclableLoopProcedure(createProcedure(async));
        }
    }

    private class RecyclableLoopProcedure implements LoopProcedure<INPUT, OUTPUT> {

        private final LoopProcedure<INPUT, OUTPUT> mProcedure;

        public RecyclableLoopProcedure(final LoopProcedure<INPUT, OUTPUT> procedure) {

            mProcedure = procedure;
        }

        @Override
        public void onInput(final INPUT input, final ResultPublisher<OUTPUT> results) {

            mProcedure.onInput(input, results);
        }

        @Override
        public void onReset(final ResultPublisher<OUTPUT> results) {

            mProcedure.onReset(results);

            recycle();
        }

        @Override
        public void onResult(final ResultPublisher<OUTPUT> results) {

            mProcedure.onResult(results);

            recycle();
        }

        private void recycle() {

            synchronized (mMutex) {

                final LinkedList<RecyclableLoopProcedure> instances = mProcedureList;

                if (instances.size() < mMaxInstanceRecycled) {

                    instances.add(this);
                }
            }
        }
    }

    private class RoutineProcedureProvider implements ProcedureProvider<INPUT, OUTPUT> {

        private final boolean mAsync;

        private RoutineProcedureProvider(final boolean async) {

            mAsync = async;
        }

        @Override
        public LoopProcedure<INPUT, OUTPUT> create() {

            return getRecyclableProcedure(mAsync);
        }
    }
}