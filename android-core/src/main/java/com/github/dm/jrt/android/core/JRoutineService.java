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

import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class supporting the creation of routine builders specific to the Android platform.
 * <br>
 * Routine invocations created through the returned builder will be executed inside a Service
 * specified by the Service context.
 * <p>
 * It is up to the caller to properly declare the Service in the manifest file. Note also that it is
 * possible to manage the Service lifecycle starting it through the
 * {@link android.content.Context#startService(android.content.Intent)} method. Normally the Service
 * will stay active only during a routine invocation. In fact, it is responsibility of the caller
 * to ensure that the started invocations have completed or have been aborted when the relative
 * Context (for example the Activity) is destroyed, so to avoid the leak of IPC connections.
 * <br>
 * The Service can be also made run in a different process, however, in such case, the data passed
 * through the routine input and output channels, as well as the factory arguments, must comply with
 * the {@link android.os.Parcel#writeValue(Object)} method. Be aware though, that issues may arise
 * when employing {@link java.io.Serializable} objects on some OS versions, so, it is advisable to
 * use {@link android.os.Parcelable} objects instead.
 * <p>
 * For example, in order to get a resource from the network, needed to fill an Activity UI:
 * <pre><code>
 * &#64;Override
 * protected void onCreate(final Bundle savedInstanceState) {
 *   super.onCreate(savedInstanceState);
 *   setContentView(R.layout.my_activity_layout);
 *   final Routine&lt;URI, MyResource&gt; routine =
 *       JRoutineService.routineOn(serviceOf(this))
 *                      .of(factoryOf(LoadResourceUri.class));
 *   routine.invoke()
 *          .consume(new TemplateChannelConsumer&lt;MyResource&gt;() {
 *
 *              &#64;Override
 *              public void onError(&#64;NotNull final RoutineException error) {
 *                displayError(error);
 *              }
 *
 *              &#64;Override
 *              public void onOutput(final MyResource resource) {
 *                displayResource(resource);
 *              }
 *          })
 *          .pass(RESOURCE_URI)
 *          .close();
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 01/08/2015.
 */
public class JRoutineService {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineService() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a Context based builder of Service routines.
   *
   * @param serviceSource the Service source.
   * @return the routine builder.
   */
  @NotNull
  public static ServiceRoutineBuilder routineOn(@NotNull final ServiceSource serviceSource) {
    return new DefaultServiceRoutineBuilder(serviceSource);
  }

  static {
    // Sets the Android log as default
    Logger.setDefaultLog(AndroidLogs.androidLog());
  }
}
