# Concurrency4D 
[![](https://jitpack.io/v/Furetur/Concurrency4D.svg)](https://jitpack.io/#Furetur/Concurrency4D) ![Build status](https://github.com/Furetur/Concurrency4D/actions/workflows/build.yml/badge.svg) ![Test coverage](https://raw.githubusercontent.com/Furetur/Concurrency4D/master/.github/badges/jacoco.svg) ![Branch coverage](https://raw.githubusercontent.com/Furetur/Concurrency4D/master/.github/badges/branches.svg)


## Table of Contents

1. [Motivation](#motivation)
2. [Add as a Dependency](#add-as-a-dependency)
3. [Tutorial](#tutorial)
   1. [Simple deterministic graphs](#simple-deterministic-graphs)
   2. [Join: receive from multiple channels](#join--receive-from-multiple-channels)
   3. [Adding non-determinism](#adding-non-determinism)
   4. [Select: merge channels](#select--merge-channels)

## Motivation

* Multithreaded programming is hard!
* This is why programming languages implement approaches that simplify multithreaded programming by protecting the user from various multithreaded errors:
  * async/await, coroutines, actors, asynchronous streams, etc.
* However, existing approaches do not address one of the principal complexities of multithreading: **non-determinism**.
  * Non-determinism can _even_ be introduced when rewriting deterministic sequential algorithms with actors or coroutines.
* Non-determinism increases the complexity of development and testing:
  * Every bug is harder to reproduce, thus, to debug and fix.

**Our approach**

* This library offers a way to isolate deterministic code from non-determinism.
* You can mark the _core logic_ of your application as _deterministic_.
  * The surrounding non-deterministic code communicates with core logic via channels.
* The deterministic core can be easily tested separately.
* Just like async/await lets you isolate core (sync) functions from non-deterministic (async) functions.



## Add as a Dependency

The library is published via Jitpack. To use it as a dependency in your Gradle project, add the following to the `build.gradle` file:

```groovy
// Add the Jitpack repository 
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// Add the library as a dependency
dependencies {
    // ...
    implementation 'com.github.Furetur:Concurrency4D:<latest version>'
}

// Our library uses Java preview features, namely virtual threads.
// Thus, '--enable-preview' flag is required
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ["--enable-preview"]
}
tasks.withType(Test).configureEach {
    jvmArgs += ["--enable-preview"]
}
tasks.withType(JavaExec).configureEach {
    jvmArgs += ['--enable-preview']
}
```

Then, just import it in your Java code as:

```java
import com.github.furetur.concurrency4d.*;
```

## Tutorial

In this section, a brief introduction to the library API is presented, accompanied by examples of code. 
Firstly, it focuses on the creation of deterministic coroutines and graphs. 
Subsequently, a demonstration is provided of how non-deterministic code can be written by reusing 
the previously defined deterministic graphs.

### Simple deterministic graphs

One of the simplest examples of a deterministic coroutine is a coroutine that calculates a sequence of squares.
The code for this coroutine is provided below.

```java
class Squares extends Coroutine {
    SendChannel<Integer> channel;
    
    Squares(SendChannel<Integer> channel) {
        super(List.of(), List.of(channel));
        this.channel = channel;
    }
    
    @Override
    protected void run() {
        for (int i = 0; i < 5; i++) {
            channel.send(i * i);
        }
    }
}
```

Each coroutine must extend from the `Coroutine` class.
A coroutine's constructor must register all its input and output channels by calling `super()` and passing the lists of the channels respectively.
The `run()` method is called when the coroutine is scheduled.
In the given example, this method sends into the channel squares of integers from 0 to 5. 
Each channel operation like `send()` or `receive()` blocks the calling coroutine and reuses the current thread for another one.

`SendChannel` is the channel interface that allows sending. The interface for receiving is `ReceiveChannel`.

To use this coroutine, a graph must be created.

```java
var graph = Graph.create();

var channel = graph.<Integer>channel();
graph.coroutine(new Squares(channel));

graph.build();

for (int i = 0; i < 5; i++) {
    System.out.println(channel.receive());
}

// Prints:
// 0 1 4 9 16
```

The `Graph.create()` method creates a graph builder that is used to construct the graph.
It consists of an integer channel that is created by the `.<Integer>channel()` call and an instance of the `Squares` coroutine.
The graph description must be finalized by the `.build()` call.

In this example, the `receive()` call blocks the current thread and schedules the `Squares` coroutine.
The coroutines are scheduled lazily by receiving and sending values.
A coroutine is initially run only when it is expected to consume or produce values.

### Join: receive from multiple channels

In many scenarios, coroutines need to receive values from multiple channels concurrently.
However, the associated complexity is often underestimated.
Each `receive()` call is blocking and, similar to situations involving locks, the potential for deadlock arises if the order of receive operations differs between coroutines.

This issue is solved by the `Graph.join()` method, which is similar to the `zip` function commonly used with lists.
It accepts two channels and returns a channel that can be used to receive pairs of values from the original channels.

The code below creates a graph of two coroutines and joins their output channels.
The resulting channel contains pairs of squares and cubes of integers from 0 to 5.

```java
var graph = Graph.create();

var squares = graph.<Integer>channel();
graph.coroutine(new Squares(squares));

var cubes = graph.<Integer>channel();
graph.coroutine(new Cubes(cubes));

var result = graph.join(squares, cubes);

graph.build();

for (int i = 0; i < 5; i++) {
    System.out.println(result.receive());
}
// Prints
// (0, 0) (1, 1) (4, 8) (9, 27) (16, 64)
```

### Adding non-determinism

Real applications perform non-deterministic operations.
This library aims to improve the developer experience and facilitate testing by encouraging a clear distinction between core deterministic logic and non-deterministic code.
However, it is the developers' responsibility to correctly identify non-deterministic coroutines and to use non-deterministic graph builders only when it is necessary.

Non-deterministic coroutines and graphs are called _async_ following the *async/await* pattern that similarly identifies non-deterministic I/O operations. 

The following code implements a non-deterministic coroutine that reads the file and sends its contents line-by-line into a channel.

```java
class FileReader extends AsyncCoroutine {
    SendChannel<String> output;
    String path;

    FileReader(String path, SendChannel<String> output) {
        super(List.of(), List.of(output));
        this.path = path;
        this.output = output;
    }

    @Override
    protected void run() {
        var lines = ... // read file contents as String
        for (String line : lines) {
            output.send(line);
        }
    }
}
```

Non-deterministic coroutines must extend from the `AsyncCoroutine` class and not from the `Coroutine`.
This is important since the latter extends from the former.
In other words, a deterministic coroutine is a special kind of non-deterministic one and can be reused in non-deterministic graphs.
However, non-deterministic coroutines cannot be added to deterministic graphs.

The next example combines the previous ones by numbering lines of a file with squares of numbers.

```java
// deterministic graph
var graph = Graph.create();
var squares = graph.<Integer>channel();
graph.coroutine(new Squares(squares));
graph.build();

// non-deterministic graph
var asyncGraph = AsyncGraph.create();
var lines = asyncGraph.<String>channel();
asyncGraph.coroutine(new FileReader("file.txt", lines));

var result = asyncGraph.join(squares, lines);

asyncGraph.build();

// start
for (int i = 0; i < 5; i++) {
    System.out.println(result.receive());
}
// Prints
// (0, line0) (1, line1) (4, line2) (9, line3) (16, line4)
```

Non-deterministic graphs may have deterministic subgraphs.
This example creates a deterministic subgraph with a `Squares` coroutine and connects it to the non-deterministic `FileReader` coroutine that belongs to the non-deterministic graph.
By initially using a deterministic graph builder we let the library verify all the constraints and use more performant deterministic channels.

### Select: merge channels

Suppose a resource is stored at two different locations.  
The objective is to fetch the resource from both servers simultaneously and return the response that is received first.

Assuming the coroutine `Fetch` fetches the resource from the given server this can be achieved with the following code.

```java
var graph = AsyncGraph.create();

var channel1 = graph.<Response>channel();
graph.coroutine(new Fetch(url1, channel1));

var channel2 = graph.<Response>channel();
graph.coroutine(new Fetch(url2, channel2));

var result = graph.select(channel1, channel2);

System.out.println(result.receive());
```

Method `AsyncGraph.select()` merges two channels into a resulting channel without preserving the order.


