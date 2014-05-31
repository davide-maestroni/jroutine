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
package com.bmd.android.wtf;

import android.os.Handler;
import android.os.Looper;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.src.Pool;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link com.bmd.wtf.crr.Current} employing the Android
 * {@link android.os.Looper} queue to execute the waterfall commands.
 * <p/>
 * Created by davide on 3/5/14.
 */
class LooperCurrent implements Current {

    private final Handler mHandler;

    /**
     * Constructor.
     *
     * @param looper The looper to employ.
     */
    public LooperCurrent(final Looper looper) {

        mHandler = new Handler(looper);
    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final DATA drop) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {

                pool.discharge(drop);
            }
        });
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                pool.discharge(drop);
            }

        }, timeUnit.toMillis(delay));
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                for (final DATA drop : drops) {

                    pool.discharge(drop);
                }
            }

        }, timeUnit.toMillis(delay));
    }

    @Override
    public void drop(final Pool<?> pool, final Object debris) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {

                pool.drop(debris);
            }
        });
    }

    @Override
    public void dropAfter(final Pool<?> pool, final long delay, final TimeUnit timeUnit, final Object debris) {

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                pool.drop(debris);
            }

        }, timeUnit.toMillis(delay));
    }

    @Override
    public void flush(final Pool<?> pool) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {

                pool.flush();
            }
        });
    }
}