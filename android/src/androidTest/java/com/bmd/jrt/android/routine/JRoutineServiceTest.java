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
package com.bmd.jrt.android.routine;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;

import com.bmd.jrt.android.invocation.AndroidSimpleInvocation;
import com.bmd.jrt.android.invocation.AndroidTemplateInvocation;
import com.bmd.jrt.android.invocation.AndroidTunnelInvocation;
import com.bmd.jrt.android.log.AndroidLog;
import com.bmd.jrt.android.runner.MainRunner;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.time.TimeDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JRoutine activity unit tests.
 * <p/>
 * Created by davide on 12/1/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineServiceTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public JRoutineServiceTest() {

        super(TestActivity.class);
    }

    public void testAbort() {

        final Data data = new Data();
        final Routine<Data, Data> routine1 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(Delay.class))
                        .dispatchIn(Looper.getMainLooper())
                        .runnerClass(MainRunner.class)
                        .buildRoutine();

        final OutputChannel<Data> channel = routine1.callAsync(data);
        assertThat(channel.abort(new IllegalArgumentException("test"))).isTrue();

        try {

            channel.readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final Routine<Data, Data> routine2 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(Abort.class))
                        .dispatchIn(Looper.getMainLooper())
                        .buildRoutine();

        try {

            routine2.callAsync(data).readFirst();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    public void testInvocations() throws InterruptedException {

        final Routine<String, String> routine1 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringTunnelInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .syncRunner(RunnerType.QUEUED)
                        .inputOrder(DataOrder.DELIVERY)
                        .logClass(AndroidLog.class)
                        .logLevel(LogLevel.DEBUG)
                        .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine1.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                       "3", "4",
                                                                                       "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");

        final Routine<String, String> routine2 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringSimpleInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .syncRunner(RunnerType.SEQUENTIAL)
                        .outputOrder(DataOrder.DELIVERY)
                        .logClass(AndroidLog.class)
                        .logLevel(LogLevel.DEBUG)
                        .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5").readAll()).containsExactly("1", "2",
                                                                                         "3", "4",
                                                                                         "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5").readAll()).containsExactly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");

        final RoutineConfigurationBuilder builder =
                new RoutineConfigurationBuilder().inputOrder(DataOrder.DELIVERY)
                                                 .outputOrder(DataOrder.DELIVERY);
        final Routine<String, String> routine3 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringSimpleInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .apply(builder.buildConfiguration())
                        .buildRoutine();
        assertThat(routine3.callSync("1", "2", "3", "4", "5").readAll()).containsExactly("1", "2",
                                                                                         "3", "4",
                                                                                         "5");
        assertThat(routine3.callAsync("1", "2", "3", "4", "5").readAll()).containsExactly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");
        assertThat(routine3.callParallel("1", "2", "3", "4", "5").readAll()).containsExactly("1",
                                                                                             "2",
                                                                                             "3",
                                                                                             "4",
                                                                                             "5");

        final Routine<String, String> routine4 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringSimpleInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .maxRetained(0)
                        .maxRunning(2)
                        .availableTimeout(1, TimeUnit.SECONDS)
                        .availableTimeout(TimeDuration.millis(200))
                        .buildRoutine();
        assertThat(routine4.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine4.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                       "3", "4",
                                                                                       "5");
        assertThat(routine4.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                          "3", "4",
                                                                                          "5");
    }

    public void testParcelable() {

        final MyParcelable p = new MyParcelable(33, -17);
        final Routine<MyParcelable, MyParcelable> routine =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(MyParcelableInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .buildRoutine();
        assertThat(routine.callAsync(p).readFirst()).isEqualTo(p);
    }

    public void testService() {

        final Routine<String, String> routine =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringTunnelInvocation.class))
                        .dispatchIn(Looper.getMainLooper())
                        .serviceClass(TestService.class)
                        .buildRoutine();
        assertThat(routine.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                     "4", "5");
        assertThat(routine.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                         "3", "4",
                                                                                         "5");
    }

    private static class Abort extends AndroidTemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            result.abort(new IllegalStateException("test"));
        }
    }

    private static class Data implements Parcelable {

        public static final Creator<Data> CREATOR = new Creator<Data>() {


            @Override
            public Data createFromParcel(final Parcel source) {

                return new Data();
            }

            @Override
            public Data[] newArray(final int size) {

                return new Data[size];
            }
        };

        @Override
        public int describeContents() {

            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {

        }
    }

    private static class Delay extends AndroidTemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            result.after(TimeDuration.millis(500)).pass(d);
        }
    }

    private static class MyParcelable implements Parcelable {

        public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {

            @Override
            public MyParcelable createFromParcel(final Parcel source) {

                final int x = source.readInt();
                final int y = source.readInt();
                return new MyParcelable(x, y);
            }

            @Override
            public MyParcelable[] newArray(final int size) {

                return new MyParcelable[0];
            }
        };

        private final int mX;

        private final int mY;

        private MyParcelable(final int x, final int y) {

            mX = x;
            mY = y;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof MyParcelable)) {

                return false;
            }

            final MyParcelable that = (MyParcelable) o;

            return mX == that.mX && mY == that.mY;
        }

        @Override
        public int hashCode() {

            int result = mX;
            result = 31 * result + mY;
            return result;
        }

        @Override
        public int describeContents() {

            return 0;
        }


        @Override
        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeInt(mX);
            dest.writeInt(mY);
        }


    }

    private static class MyParcelableInvocation extends AndroidTunnelInvocation<MyParcelable> {

    }

    private static class StringSimpleInvocation extends AndroidSimpleInvocation<String, String> {

        @Override
        public void onCall(@Nonnull final List<? extends String> strings,
                @Nonnull final ResultChannel<String> result) {

            result.pass(strings);
        }
    }

    private static class StringTunnelInvocation extends AndroidTunnelInvocation<String> {

    }

    private static class ToUpperCase extends AndroidTemplateInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(TimeDuration.millis(500)).pass(s.toUpperCase());
        }
    }
}
