#jroutine
[![Build Status](https://travis-ci.org/davide-maestroni/jroutine.svg?branch=master)](https://travis-ci.org/davide-maestroni/jroutine)

Parallel programming on the go.

##Overview

Inspired by the Go routines, this library provides a powerful, flexible, yet familiar concurrency framework, designed to make multi-threads programming simple and funny.

The main paradigm is based on routines and channels. A routine is the container in which a piece of code is executed asynchronously. It takes care of the synchronization and manages the invocation lifecycle. The channels are means of communication between the routine and the outside world.

A routine can be invoked in a synchronous, asynchronous and parallel way. The main difference between the asynchronous and the parallel invocation is that, in the former, all the input data are processed by the same invocation instance, while, in the latter, each input item is (potentially) processed by a different one. For example, if the sum of the inputs has to be computed by the routine, an asynchronous invocation is the way to go, while, when the routine is used to compute, for instance, the square of a number, all the inputs can be safely processed in parallel.

After the invocation, the routine returns an input channel which is used to pass the input parameters. Input data can be passed in bulk or streamed, delayed or fetched asynchronously from another channel. When done with the input, the channel is closed and returns an output channel used to read the invocation results.

The main way to define a routine is to implement an invocation object. Though, the library provides several other ways (always backed by invocations) to call any method of any object (even defined in third party source code) asynchronously in separate threads.

##Why not RxJava?

Among the many open source libraries, [RxJava][6] is one of the preferred choices when it comes to handle background tasks.
Although [Reactive Extensions][7] is a great tool for managing events and composing event listeners, it has not been designed with parallel programming in mind.
In fact, RxJava shows a few limits in this respect.
The framework has once been compared, with a fitting example, to a line of domino pieces: once the first tile falls down, all the others are to follow, and there is no way to stop them other than to cut the line.
Each time a background operation is required, the whole chain of observables/subscribers must be rebuilt from scratch, the chained functions are called recursively and many objects are created in the while.
While such design works great for common events, it does not adapt so well to background tasks, where, each time, everything is recreated and discarded at the end.
After all, a network request does not really fit in the definition of "event".

RxJava is still one of the best library for handling events, though, something specifically designed is needed to make parallel programming easily accessible and manageable.

##Why JRoutine?

The JRoutine library is based on a single paradigm, at the same time simple, but flexible and powerful enough to provide all the features needed to perform, manage and combine asynchronous tasks in any environment and on any platform.
This paradigm is nothing but what any developer is already familiar to, that is, a function call.

What the library has to offer is:

* *Flat learning curve*: a single paradigm to make everything: invoke the routine, pass the inputs, read the results
* *Extreme configurability*: each routine instance may have its own set of configurations, and its own source of concurrency
* *Ease of integration*: no need to modify existing code to make a method asynchronous
* *Memory optimization*: maximum number of invocations running and retained in the instance pool, and maximum number of data passed through the channels, are just some of the parameters that can be tuned to lower memory consumption
* *Data streaming*: not all the inputs might be known at the time of invocation, and outputs might need to be fetched at specific points of the code execution
* *Real processing abort*: invocations can be interrupted at any moment between two data are passed to the input or output channels, thus achieving real abortion of the processing and not a mere removal of a listener
* *Non-recursive calls*: even during synchronous invocations, recursion is broken up in a sequential sequence of operations
* *Automatic code generation*: as an alternative to reflection, existing methods can be made asynchronous through annotation pre-processing and compile-time code generation

And more:

* *< 175KB Jar*
* *Java 5+** *and Android 1.5+*
* *@Nullable and @Nonnull annotations*

(*) for older Java versions please have a look at [Retrotranslator][15].

##Why not JRoutine?

The JRoutine library is designed for parallel programming and nothing else. It's no golden hammer and does not pretend to.
If you need event handling, please use [RxJava][6], it's one of the best library out there. If you need distributed scalable computing, consider using [Akka][8].
For anything else [GitHub][9] is a great source of inspiration.

##Usage examples

TBD

##Documentation

Complete Javadoc with insights and examples is available:

* [JRoutine][12]
* [JRoutine Android][13]

The project contains an additional sample module showing how to implement a file downloader with just 4 classes.

##Further development

Feel free to contribute with your own [Runner][14] so to support more platforms other than Android.

##Build instructions

To generate the library JAR just run on a command line:
```sh
gradlew jar
```

For the Javadocs run:
```sh
gradlew javadoc
```

To run the tests:
```sh
gradlew test
```
on a connected device:
```sh
gradlew cAT
```

To run the tests with code coverage:
```sh
gradlew jT
```

To run the FindBugs analyzer:
```sh
gradlew fM
```

To run the Android lint:
```sh
gradlew lint
```

For additional commands please refer to the [Gradle Android Plugin User Guide][4].

##Dependencies

####Runtime dependencies

- None

####Compile dependencies

- [FindBugs][10] annotations ([Lesser GNU Public License][3])
- Android SDK ([Terms and Condition][1])
- Android Support Library ([Apache License v2.0][2])

####Test dependencies

- JUnit ([Eclipse Public License v1.0][5])
- [AssertJ][11] ([Apache License v2.0][2])

##License

[The Apache Software License, Version 2.0][2]

**IT'S OPEN DUDE!**

[1]:http://developer.android.com/sdk/terms.html
[2]:http://www.apache.org/licenses/LICENSE-2.0
[3]:http://www.gnu.org/licenses/lgpl.html
[4]:http://tools.android.com/tech-docs/new-build-system/user-guide
[5]:https://www.eclipse.org/legal/epl-v10.html
[6]:https://github.com/ReactiveX/RxJava
[7]:http://reactivex.io/
[8]:http://akka.io/
[9]:https://github.com/
[10]:http://findbugs.sourceforge.net/
[11]:http://joel-costigliola.github.io/assertj/
[12]:http://davide-maestroni.github.io/jroutine/javadocs/
[13]:http://davide-maestroni.github.io/jroutine/android/javadocs/
[14]:http://davide-maestroni.github.io/jroutine/javadocs/com/bmd/jrt/runner/Runner.html
[15]:http://retrotranslator.sourceforge.net/
