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
package com.gh.bmd.jrt.android.routine;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.routine.JRoutine.InstanceFactory;
import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Object service routine unit tests.
 * <p/>
 * Created by davide on 3/30/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ObjectServiceRoutineBuilderTest
        extends ActivityInstrumentationTestCase2<TestActivity> {

    public ObjectServiceRoutineBuilderTest() {

        super(TestActivity.class);
    }

    public void testAAA() {

        assertThat(JRoutine.onService(getActivity(), ClassToken.tokenOf(TestClass.class),
                                      ClassToken.tokenOf(TestClass.class))
                           .dispatchingOn(Looper.getMainLooper())
                           .boundMethod("ciao")
                           .callAsync()
                           .readNext()).isEqualTo(new TestClass().hello());
        assertThat(JRoutine.onService(getActivity(), ClassToken.tokenOf(TestClass.class),
                                      ClassToken.tokenOf(TestClass.class))
                           .dispatchingOn(Looper.getMainLooper())
                           .buildProxy(TestItf.class)
                           .ciao()
                           .readNext()).isEqualTo(new TestClass().hello());
    }

    public interface TestItf {

        @Bind("ciao")
        @Timeout(1000)
        @Pass(String.class)
        OutputChannel<String> ciao();
    }

    public static class TestClass implements InstanceFactory<TestClass> {

        @Bind("ciao")
        @Timeout(1000)
        public String hello() {

            return "Hello!!";
        }

        @Nonnull
        public TestClass newInstance(@Nonnull final Context context) {

            return this;
        }
    }
}
