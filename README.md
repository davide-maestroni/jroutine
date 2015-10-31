# JRoutine
[![Build Status](https://travis-ci.org/davide-maestroni/jroutine.svg?branch=master)](https://travis-ci.org/davide-maestroni/jroutine)
[![Coverage Status](https://img.shields.io/coveralls/davide-maestroni/jroutine.svg)](https://coveralls.io/r/davide-maestroni/jroutine?branch=master)
[![GitHub License](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Parallel programming on the go.

## Overview

Inspired by the Go routines, this library provides a powerful, flexible, yet familiar concurrency framework, designed to make multi-threads programming simple and funny.

The main paradigm is based on routines and channels. A routine is the container in which a piece of code is executed asynchronously. It takes care of the synchronization and manages the invocation lifecycle. The channels are means of communication between the routine and the outside world.

A routine can be invoked in a synchronous, asynchronous and parallel way. The main difference between the asynchronous and the parallel invocation is that, in the former, all the input data are processed by the same invocation instance, while, in the latter, each input item is (potentially) processed by a different one. For example, if the sum of the inputs has to be computed by the routine, an asynchronous invocation is the way to go, while, when the routine is used to compute, for instance, the square of a number, all the inputs can be safely processed in parallel.

After the invocation, the routine returns an input channel which is used to pass the input parameters. Input data can be passed in bulk or streamed, delayed or fetched asynchronously from another channel. When done with the input, the channel is closed and returns an output channel used to read the invocation results.

The main way to define a routine is to implement an invocation object. Though, the library provides several other ways (always backed by invocations) to call any method of any object (even defined in third party source code) asynchronously.

## It's reactive!

The library adheres to [The Reactive Manifesto][reactive manifesto]. It's *responsive*, *resilient*, *elastic* and *message driven*.

It's *responsive*, since commands are enqueued to be executed asynchronously so to never block the calling thread. Computation of asynchronous invocations happens only in response of inputs and resources are freed as soon as the input is consumed. Resources are allocated only when needed, and reused when possible.<br/>
It's *resilient*, since errors are gracefully handled and notified through the proper methods implemented by the specific invocation classes.<br/>
It's *elastic*, since the needed number of threads is allocated in response to higher loads.<br/>
It's *message driven*, since both ouputs and errors are dispatched along the chain of invocations.

## Why not RxJava?

Among the many open source libraries, [RxJava][rxjava] is one of the preferred choices when it comes to handle background tasks.
Although [Reactive Extensions][reactivex] is a great tool for managing events and composing event listeners, it has not been clearly designed with parallel programming in mind.
In fact, RxJava shows a few limits in this respect.
The framework has once been compared, with a fitting example, to a line of domino pieces: once the first tile falls down, all the others are to follow, and there is no way to stop them other than to cut the line.
Each time a background operation is required, the whole chain of observables/subscribers must be rebuilt from scratch, the chained functions are called recursively and many objects are created in the while.
While such design works great for common events, it does not adapt so well to background tasks.
After all, a network request does not really fit in the definition of "event".

RxJava is still one of the best library for handling events, though, something specifically designed is needed to make parallel programming easily accessible and manageable.

## Why JRoutine?

The JRoutine library is based on a single paradigm, at the same time simple, but flexible and powerful enough to provide all the features needed to perform, manage and combine asynchronous tasks in any environment and on any platform.
This paradigm is nothing but what any developer is already familiar to, that is, a function call.

What the library has to offer is:

* ***Flat learning curve***: a single paradigm to make everything: invoke the routine, pass the inputs, read the results.
* ***Extreme configurability***: each routine instance may have its own set of configurations, and its own source of concurrency.
* ***Ease of integration***: no need to modify existing code to make a method asynchronous.
* ***Memory optimization***: maximum number of invocations running and retained in the instance pool, and maximum number of data passed through the channels, are just some of the parameters that can be tuned to lower memory consumption.
* ***Data streaming***: not all the inputs might be known at the time of invocation, and outputs might need to be fetched at specific points of the code execution.
* ***Real processing abort***: invocations can be interrupted at any moment between two data are passed to the input or output channels, thus achieving real abortion of the processing and not a mere removal of a listener.
* ***Non-recursive calls***: even during synchronous invocations, recursion is broken up in a sequential array of operations.
* ***Automatic code generation***: as an alternative to reflection, existing methods can be made asynchronous through annotation pre-processing and compile-time code generation.
* ***Nice handling of Android configuration changes***: the same paradigm is applied to the Android platform so to support background tasks surviving changes in the configuration of Activities or Fragments.
* ***Seamlessly run in a remote Service***: invocations can be easily configured to run in a dedicated Android service.
* ***Functional builder***: it is also possible to build routines by employing functional programming paradigms.

And more:

* ***350KB Jar***
* ***Java 5+**** ***and Android 1.6+***
* ***Nullity annotations***

(*) for older Java versions please have a look at [Retrotranslator][retrotranslator].

## Why not JRoutine?

The JRoutine library is designed for parallel programming and nothing else. It's no golden hammer and does not pretend to.
If you need event handling, please use [RxJava][rxjava], it's one of the best library out there. If you need distributed scalable computing, consider using [Akka][akka].
For anything else [GitHub][github] is a great source of inspiration.

## Usage

Please have a look at the [Wiki][wiki].

## Documentation

Complete Javadoc with insights and examples is available for:

* [JRoutine][javadoc]
* [JRoutine-Proxy][javadoc proxy]
* [JRoutine-Android][javadoc android]
* [JRoutine-AndroidProxy][javadoc androidproxy]

The project contains an additional [sample][sample] module showing how to implement a file downloader with just 3 classes.

## Versioning Convention

The library artifacts will follow a specific versioning convention. Each version will be identified by a major, a minor and a revision number.

* ***Major Version***: the major version number will update when a stuctural change (more than some class renaming) takes place. All the dependent artifact major versions will be updated as well.
* ***Minor Version***: the minor version number will update when backward compatibility is broken. All the dependent artifact minor versions will be updated as well.
* ***Revision Number***: the revision number will update when non-breaking changes are made (typically bug fixing). The dependent artifacts will not be impacted.

In terms of backward compatibility, it is hence safe to depend on a specific major and minor version, ignoring the revision number (like, for instance: *4.2.+*).

## Further development

Feel free to contribute with your own [Routine][javadoc routine] or [Runner][javadoc runner] implementations, in order to support more platforms other than Android.

## Build instructions

Please refer to the dedicated [Wiki page][wiki build].

## Dependencies

#### Runtime dependencies

- None

#### Compilation dependencies

- IntelliJ IDEA Annotations ([Apache License v2.0][apache license])
- [FindBugs][findbugs] annotations ([Lesser GNU Public License][lgpl license])
- Android SDK ([Terms and Condition][android sdk])
- Android Support Library ([Apache License v2.0][apache license])

#### Test dependencies

- JUnit ([Eclipse Public License v1.0][eclipse license])
- [AssertJ][assertj] ([Apache License v2.0][apache license])

## Artifacts

Module | Latest Version
--- | ---
JRoutine | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine)
JRoutine-Proxy | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-proxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-proxy)
JRoutine-Android | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-android/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-android)
JRoutine-AndroidProxy | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-androidproxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.davide-maestroni/jroutine-androidproxy)

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

&nbsp;&nbsp;&nbsp;&nbsp;[http://www.apache.org/licenses/LICENSE-2.0][apache license]

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

**IT'S OPEN DUDE!**

[apache license]:http://www.apache.org/licenses/LICENSE-2.0
[eclipse license]:https://www.eclipse.org/legal/epl-v10.html
[lgpl license]:http://www.gnu.org/licenses/lgpl.html
[reactive manifesto]:http://www.reactivemanifesto.org/
[android sdk]:http://developer.android.com/sdk/terms.html
[findbugs]:http://findbugs.sourceforge.net/
[assertj]:http://joel-costigliola.github.io/assertj/
[retrotranslator]:http://retrotranslator.sourceforge.net/
[github]:https://github.com/
[rxjava]:https://github.com/ReactiveX/RxJava
[reactivex]:http://reactivex.io/
[akka]:http://akka.io/
[wiki]:https://github.com/davide-maestroni/jroutine/wiki
[wiki build]:https://github.com/davide-maestroni/jroutine/wiki/Build-Instructions
[sample]:https://github.com/davide-maestroni/jroutine/tree/master/sample
[javadoc]:http://davide-maestroni.github.io/jroutine/javadoc/library
[javadoc proxy]:http://davide-maestroni.github.io/jroutine/javadoc/proxy
[javadoc android]:http://davide-maestroni.github.io/jroutine/javadoc/android
[javadoc androidproxy]:http://davide-maestroni.github.io/jroutine/javadoc/androidproxy
[javadoc routine]:http://davide-maestroni.github.io/jroutine/javadoc/library/com/github/dm/jrt/routine/Routine.html
[javadoc runner]:http://davide-maestroni.github.io/jroutine/javadoc/library/com/github/dm/jrt/runner/Runner.html
