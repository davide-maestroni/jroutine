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
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.time.TimeDuration;

import java.util.List;

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

    public void testInvocations() throws InterruptedException {

        final Routine<String, String> routine1 =
                JRoutine.onService(getActivity(), ClassToken.tokenOf(StringTunnelInvocation.class))
                        .syncRunner(RunnerType.QUEUED).dispatchIn(Looper.getMainLooper())
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
                        .syncRunner(RunnerType.SEQUENTIAL).dispatchIn(Looper.getMainLooper())
                        .logClass(AndroidLog.class)
                        .logLevel(LogLevel.DEBUG)
                        .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2", "3",
                                                                                      "4", "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
                                                                                       "3", "4",
                                                                                       "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5").readAll()).containsOnly("1", "2",
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

    private static class Abort extends AndroidTemplateInvocation<Data, Data> {

        @Override
        public void onInput(final Data d, @Nonnull final ResultChannel<Data> result) {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException e) {

                RoutineInterruptedException.interrupt(e);
            }

            result.abort(new IllegalStateException());
        }
    }

    private static class Data {

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
        public int describeContents() {

            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeInt(mX);
            dest.writeInt(mY);
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
