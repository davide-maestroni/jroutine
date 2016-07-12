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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.github.dm.jrt.stream.processor.Processors.output;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provider factory unit tests
 * <p>
 * Created by davide-maestroni on 05/21/2016.
 */
public class ProviderAdapterFactoryTest {

    @Test
    public void testDefault() {

        final TestAdapterFactory defaultFactory = new TestAdapterFactory();
        final TestAdapterFactory factory1 = new TestAdapterFactory();
        final TestAdapterFactory factory2 = new TestAdapterFactory();
        {
            final ProviderAdapterFactory adapterFactory = //
                    ProviderAdapterFactory.builder()
                                          .whenMissingAnnotation(defaultFactory)
                                          .add("list", factory1)
                                          .add("stream", factory2)
                                          .buildFactory();
            final GsonConverterFactory converterFactory = GsonConverterFactory.create();
            final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                                   .addCallAdapterFactory(adapterFactory)
                                                   .addConverterFactory(converterFactory)
                                                   .build();
            final GitHubService service = retrofit.create(GitHubService.class);
            service.listRepos("octocat").next();
            assertThat(factory1.isCalled()).isTrue();
            assertThat(factory2.isCalled()).isFalse();
            assertThat(defaultFactory.isCalled()).isFalse();
            factory1.setCalled(false);
            service.streamRepos("octocat").syncCall().close();
            assertThat(factory1.isCalled()).isFalse();
            assertThat(factory2.isCalled()).isTrue();
            assertThat(defaultFactory.isCalled()).isFalse();
            factory2.setCalled(false);
            service.getRepos("octocat");
            assertThat(factory1.isCalled()).isFalse();
            assertThat(factory2.isCalled()).isFalse();
            assertThat(defaultFactory.isCalled()).isTrue();
            defaultFactory.setCalled(false);
        }

        {
            final ProviderAdapterFactory adapterFactory = //
                    ProviderAdapterFactory.builder()
                                          .whenMissingName(defaultFactory)
                                          .add("list", factory1)
                                          .buildFactory();
            final GsonConverterFactory converterFactory = GsonConverterFactory.create();
            final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                                   .addCallAdapterFactory(adapterFactory)
                                                   .addConverterFactory(converterFactory)
                                                   .build();
            final GitHubService service = retrofit.create(GitHubService.class);
            service.listRepos("octocat").next();
            assertThat(factory1.isCalled()).isTrue();
            assertThat(defaultFactory.isCalled()).isFalse();
            factory1.setCalled(false);
            service.streamRepos("octocat");
            assertThat(factory1.isCalled()).isFalse();
            assertThat(defaultFactory.isCalled()).isTrue();
            defaultFactory.setCalled(false);
            try {
                service.getRepos("octocat");
                fail();

            } catch (final IllegalArgumentException ignored) {

            }
        }

        {
            final ProviderAdapterFactory adapterFactory = //
                    ProviderAdapterFactory.builder()
                                          .whenMissing(defaultFactory)
                                          .add("list", factory1)
                                          .buildFactory();
            final GsonConverterFactory converterFactory = GsonConverterFactory.create();
            final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                                   .addCallAdapterFactory(adapterFactory)
                                                   .addConverterFactory(converterFactory)
                                                   .build();
            final GitHubService service = retrofit.create(GitHubService.class);
            service.listRepos("octocat").next();
            assertThat(factory1.isCalled()).isTrue();
            assertThat(defaultFactory.isCalled()).isFalse();
            factory1.setCalled(false);
            service.streamRepos("octocat");
            assertThat(factory1.isCalled()).isFalse();
            assertThat(defaultFactory.isCalled()).isTrue();
            defaultFactory.setCalled(false);
            service.getRepos("octocat");
            assertThat(factory1.isCalled()).isFalse();
            assertThat(defaultFactory.isCalled()).isTrue();
            defaultFactory.setCalled(false);
        }
    }

    @Test
    public void testNotAnnotated() {

        final TestAdapterFactory factory1 = new TestAdapterFactory();
        final TestAdapterFactory factory2 = new TestAdapterFactory();
        final ProviderAdapterFactory adapterFactory = //
                ProviderAdapterFactory.builder()
                                      .add("list", factory1)
                                      .add("stream", factory2)
                                      .buildFactory();
        final GsonConverterFactory converterFactory = GsonConverterFactory.create();
        final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                               .addCallAdapterFactory(adapterFactory)
                                               .addConverterFactory(converterFactory)
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        try {
            service.getRepos("octocat");
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testSingle() {

        final TestAdapterFactory factory = new TestAdapterFactory();
        final ProviderAdapterFactory adapterFactory =
                ProviderAdapterFactory.withFactory("list", factory);
        final GsonConverterFactory converterFactory = GsonConverterFactory.create();
        final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                               .addCallAdapterFactory(adapterFactory)
                                               .addConverterFactory(converterFactory)
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        service.listRepos("octocat").next();
        assertThat(factory.isCalled()).isTrue();
        try {
            service.streamRepos("octocat");
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testWrongName() {

        final TestAdapterFactory factory = new TestAdapterFactory();
        final ProviderAdapterFactory adapterFactory =
                ProviderAdapterFactory.withFactory("test", factory);
        final GsonConverterFactory converterFactory = GsonConverterFactory.create();
        final Retrofit retrofit = new Builder().baseUrl("http://localhost")
                                               .addCallAdapterFactory(adapterFactory)
                                               .addConverterFactory(converterFactory)
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        try {
            service.listRepos("octocat");
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            service.streamRepos("octocat");
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class TestAdapterFactory extends CallAdapter.Factory {

        private boolean mCalled;

        @Override
        public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
                final Retrofit retrofit) {

            mCalled = true;
            final RoutineAdapterFactory factory = RoutineAdapterFactory.buildFactory();
            final Type responseType = factory.get(returnType, annotations, retrofit).responseType();
            return new CallAdapter<Object>() {

                public Type responseType() {

                    return responseType;
                }

                public <R> Object adapt(final Call<R> call) {

                    final StreamBuilder<?, ?> builder = //
                            JRoutineStream.withStream()
                                          .sync()
                                          .let(output((Object) Collections.emptyList()));
                    if (((ParameterizedType) returnType).getRawType() == Channel.class) {
                        return builder.syncCall().close();
                    }

                    return builder;
                }
            };
        }

        public boolean isCalled() {

            return mCalled;
        }

        public void setCalled(final boolean called) {

            mCalled = called;
        }
    }
}
