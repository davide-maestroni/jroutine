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

package com.github.dm.jrt.android.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.DecoratingService.StringInvocation;
import com.github.dm.jrt.android.invocation.FilterContextInvocation;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Target invocation factories unit test.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class TargetInvocationFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public TargetInvocationFactoryTest() {

        super(TestActivity.class);
    }

    public void testInvocationDecoratorAbort() {

        final Routine<String, String> routine =
                JRoutineService.with(serviceFrom(getActivity(), DecoratingService.class))
                               .on(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.asyncInvoke().after(millis(100)).pass("test").result().abort()).isTrue();
        routine.purge();
    }

    public void testInvocationDecoratorLifecycle() {

        final Routine<String, String> routine =
                JRoutineService.with(serviceFrom(getActivity(), DecoratingService.class))
                               .on(factoryOf(PassingStringInvocation.class))
                               .buildRoutine();
        assertThat(routine.asyncCall("test").afterMax(seconds(10)).all()).containsExactly("test");
        routine.purge();
    }

    private static class PassingStringInvocation extends FilterContextInvocation<String, String>
            implements StringInvocation {

        private PassingStringInvocation() {

            super(null);
        }

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input);
        }
    }
}
