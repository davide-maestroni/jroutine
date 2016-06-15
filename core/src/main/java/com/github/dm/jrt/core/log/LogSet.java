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

package com.github.dm.jrt.core.log;

import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Log implementation backed by a set of log objects.
 * <p>
 * Created by davide-maestroni on 12/28/2015.
 */
public class LogSet extends CopyOnWriteArraySet<Log> implements Log {

    private static final long serialVersionUID = -1;

    /**
     * Returns a new log set containing the specified elements.
     *
     * @param logs array containing elements to be added to this set.
     * @return the log set.
     */
    @NotNull
    public static LogSet of(@NotNull final Log... logs) {
        return new LogSet().appendAll(logs);
    }

    /**
     * Returns a new log set containing the specified elements.
     *
     * @param logs collection containing elements to be added to this set.
     * @return the log set.
     */
    @NotNull
    public static LogSet of(@NotNull final Collection<? extends Log> logs) {
        return new LogSet().appendAll(logs);
    }

    /**
     * Adds all of the elements in the specified array to this set if they're not already present.
     * <br>
     * If the specified collection is also a set, the <tt>addAll</tt> operation effectively modifies
     * this set so that its value is the <i>union</i> of the two sets.  The behavior of this
     * operation is undefined if the specified collection is modified while the operation is in
     * progress.
     *
     * @param logs array containing elements to be added to this set.
     * @return <tt>true</tt> if this set changed as a result of the call.
     * @see #add(Object)
     */
    public boolean addAll(@NotNull final Log... logs) {
        boolean result = true;
        for (final Log log : logs) {
            result &= add(log);
        }

        return result;
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * <br>
     * More formally, adds the specified element <tt>e</tt> to this set if the set contains no
     * element <tt>e2</tt> such that
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
     * <br>
     * If this set already contains the element, the call leaves the set unchanged and returns
     * <tt>false</tt>.
     *
     * @param log log to be added to this set.
     * @return this log set.
     */
    @NotNull
    public LogSet append(final Log log) {
        add(log);
        return this;
    }

    /**
     * Adds all of the elements in the specified array to this set if they're not already present.
     * <br>
     * If the specified collection is also a set, the <tt>addAll</tt> operation effectively modifies
     * this set so that its value is the <i>union</i> of the two sets.  The behavior of this
     * operation is undefined if the specified collection is modified while the operation is in
     * progress.
     *
     * @param logs array containing elements to be added to this set.
     * @return <tt>true</tt> if this set changed as a result of the call.
     * @see #append(Log)
     */
    @NotNull
    public LogSet appendAll(final Log... logs) {
        addAll(logs);
        return this;
    }

    /**
     * Adds all of the elements in the specified collection to this set if they're not already
     * present.
     * <br>
     * If the specified collection is also a set, the <tt>addAll</tt> operation effectively modifies
     * this set so that its value is the <i>union</i> of the two sets.  The behavior of this
     * operation is undefined if the specified collection is modified while the operation is in
     * progress.
     *
     * @param logs array containing elements to be added to this set.
     * @return <tt>true</tt> if this set changed as a result of the call.
     * @see #append(Log)
     */
    @NotNull
    public LogSet appendAll(final Collection<? extends Log> logs) {
        addAll(logs);
        return this;
    }

    @Override
    public boolean contains(final Object o) {
        if (o instanceof LogSet) {
            return super.containsAll((LogSet) o);
        }

        return super.contains(o);
    }

    @Override
    public boolean remove(final Object o) {
        if (o instanceof LogSet) {
            return super.removeAll((LogSet) o);
        }

        return super.remove(o);
    }

    @Override
    public boolean add(final Log log) {
        ConstantConditions.notNull("log instance", log);
        if (log instanceof LogSet) {
            return super.addAll((LogSet) log);
        }

        return super.add(log);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends Log> c) {
        boolean result = true;
        for (final Log log : c) {
            result &= add(log);
        }

        return result;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean result = true;
        for (final Object o : c) {
            result &= remove(o);
        }

        return result;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final ArrayList<Object> list = new ArrayList<Object>(c.size());
        for (final Object o : c) {
            if (o instanceof LogSet) {
                list.addAll((LogSet) o);

            } else {
                list.add(o);
            }
        }

        return super.retainAll(list);
    }

    /**
     * Returns <tt>true</tt> if this set contains all of the elements of the specified array.
     *
     * @param logs array to be checked for containment in this set.
     * @return <tt>true</tt> if this set contains all of the elements of the specified collection.
     * @see #contains(Object)
     */
    public boolean containsAll(final Log... logs) {
        for (final Log log : logs) {
            if (!contains(log)) {
                return false;
            }
        }

        return true;
    }

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {
        for (final Log log : this) {
            log.dbg(contexts, message, throwable);
        }
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {
        for (final Log log : this) {
            log.err(contexts, message, throwable);
        }
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {
        for (final Log log : this) {
            log.wrn(contexts, message, throwable);
        }
    }

    /**
     * Removes from this set all of its elements that are contained in the specified array.
     *
     * @param logs array containing elements to be removed from this set.
     * @return <tt>true</tt> if this set changed as a result of the call
     * @see #remove(Object)
     */
    public boolean removeAll(final Log... logs) {
        boolean result = true;
        for (final Log log : logs) {
            result &= remove(log);
        }

        return result;
    }

    /**
     * Retains only the elements in this set that are contained in the specified array. In other
     * words, removes from this set all of its elements that are not contained in the specified
     * collection.
     *
     * @param logs array containing elements to be retained in this set.
     * @return <tt>true</tt> if this set changed as a result of the call.
     * @see #remove(Object)
     */
    public boolean retainAll(final Log... logs) {
        final ArrayList<Object> list = new ArrayList<Object>(logs.length);
        for (final Log log : logs) {
            if (log instanceof LogSet) {
                list.addAll((LogSet) log);

            } else {
                list.add(log);
            }
        }

        return super.retainAll(list);
    }
}
