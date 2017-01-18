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

package com.github.dm.jrt.android.reflect.builder;

import android.content.Context;
import android.content.ContextWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of a Context wrapper implementing a factory context.
 * <br>
 * The class wraps the application Context of the specified base Context.
 * <p>
 * Created by davide-maestroni on 07/09/2015.
 */
public abstract class FactoryContextWrapper extends ContextWrapper implements FactoryContext {

  /**
   * Constructor.
   *
   * @param base the base Context.
   */
  public FactoryContextWrapper(@NotNull final Context base) {
    super(base.getApplicationContext());
  }

  @Override
  public Context getApplicationContext() {
    return this;
  }
}
