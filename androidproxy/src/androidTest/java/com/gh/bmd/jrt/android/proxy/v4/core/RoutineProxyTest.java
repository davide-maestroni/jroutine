package com.gh.bmd.jrt.android.proxy.v4.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.proxy.builder.ContextProxyRoutineBuilder;
import com.gh.bmd.jrt.android.proxy.v4.annotation.ContextProxy;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide on 07/05/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class RoutineProxyTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public RoutineProxyTest() {

        super(TestActivity.class);
    }

    public void testGenericProxyCache() {

        final ContextProxyRoutineBuilder builder =
                JRoutineProxy.onActivity(getActivity(), TestList.class)
                             .withRoutineConfiguration()
                             .withReadTimeout(TimeDuration.seconds(10))
                             .set();

        final TestListItf<String> testListItf1 =
                builder.buildProxy(new ClassToken<TestListItf<String>>() {});
        testListItf1.add("test");

        assertThat(testListItf1.get(0)).isEqualTo("test");
        assertThat(builder.buildProxy(new ClassToken<TestListItf<Integer>>() {})).isSameAs(
                testListItf1);

        final TestListItf<Integer> testListItf2 =
                builder.buildProxy(new ClassToken<TestListItf<Integer>>() {});
        assertThat(testListItf2).isSameAs(testListItf1);
        assertThat(builder.buildProxy(new ClassToken<TestListItf<Integer>>() {})).isSameAs(
                testListItf2);

        testListItf2.add(3);
        assertThat(testListItf2.get(1)).isEqualTo(3);
        assertThat(testListItf2.getAsync(1).readNext()).isEqualTo(3);
        assertThat(testListItf2.getList(1)).containsExactly(3);
    }

    @ContextProxy(TestList.class)
    public interface TestListItf<TYPE> {

        void add(Object t);

        TYPE get(int i);

        @Bind("get")
        @Pass(Object.class)
        OutputChannel<TYPE> getAsync(int i);

        @Bind("get")
        @Pass(Object.class)
        List<TYPE> getList(int i);
    }

    @SuppressWarnings("unused")
    public static class TestList<TYPE> {

        private final ArrayList<TYPE> mList = new ArrayList<TYPE>();

        public void add(TYPE t) {

            mList.add(t);
        }

        public TYPE get(int i) {

            return mList.get(i);
        }
    }
}
