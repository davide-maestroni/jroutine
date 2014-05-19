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
package com.bmd.wtf.xtr.ppl;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;
import com.bmd.wtf.xtr.bsn.BlockingBasin;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for {@link com.bmd.wtf.xtr.ppl} package classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class PipelineTest extends TestCase {

    public void testError() {

        try {

            Pipeline.binding(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Pipeline.binding(Waterfall.fallingFrom(Dams.openDam())).thenConnecting(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Pipeline.binding(Waterfall.fallingFrom(Dams.openDam())).thenFlowingThrough(null);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            final AbstractDuct<Object, Object> duct = new AbstractDuct<Object, Object>() {

                @Override
                public Object onDischarge(final Spring<Object> spring, final Object drop) {

                    return null;
                }
            };

            Pipeline.binding(Waterfall.fallingFrom(Dams.openDam())).thenConnecting(duct)
                    .thenFlowingThrough(duct);

            fail();

        } catch (final Exception ignored) {

        }
    }

    public void testPipe() {

        final ArrayList<Object> debris = new ArrayList<Object>(2);
        assertThat(BlockingBasin.collect(
                           Pipeline.binding(Waterfall.fallingFrom(new OpenDam<String>()))
                                   .thenConnecting(new AbstractDuct<String, String>() {

                                       @Override
                                       public Object onDischarge(final Spring<String> spring,
                                               final String drop) {

                                           spring.discharge(drop.toLowerCase());

                                           return null;
                                       }
                                   }).thenFlowingThrough(new AbstractDuct<String, Integer>() {

                               @Override
                               public Object onDischarge(final Spring<Integer> spring,
                                       final String drop) {

                                   new Thread(new Runnable() {

                                       @Override
                                       public void run() {

                                           try {

                                               Thread.sleep(100);

                                               spring.discharge(Integer.parseInt(drop));

                                           } catch (final InterruptedException e) {

                                               Thread.currentThread().interrupt();
                                           }
                                       }

                                   }).run();

                                   return null;
                               }

                           }).thenFlowingThrough(new AbstractDam<Integer, Integer>() {

                               private int mSum = 0;

                               @Override
                               public Object onDischarge(final Floodgate<Integer, Integer> gate,
                                       final Integer drop) {

                                   mSum += drop;

                                   return null;
                               }

                               @Override
                               public Object onFlush(final Floodgate<Integer, Integer> gate) {

                                   gate.discharge(mSum).flush();

                                   return null;
                               }

                           })
                   ).thenFeedWith("test", "1", "ciao", "2", "5", null).afterMax(1, TimeUnit.MINUTES)
                                .collectPushedDebrisInto(debris).collectOutput()
        ).containsExactly(8);
        assertThat(debris).hasSize(3);
        assertThat(debris.get(0)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris.get(1)).isExactlyInstanceOf(NumberFormatException.class);
        assertThat(debris.get(2)).isExactlyInstanceOf(NullPointerException.class);
    }
}