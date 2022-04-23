# About
This code corresponds with Chapter 2 in the upcoming O'Reilly book: [Mastering Kafka Streams and ksqlDB][book] by Mitch Seymour. This includes two "Hello, world" style applications: one implemented using the high-level DSL, and the other implemented using the Processor API.

[book]: https://www.kafka-streams-book.com/

# Running Locally
The only dependency for running these examples is [Docker Compose][docker].

[docker]: https://docs.docker.com/compose/install/

Once Docker Compose is installed, you can start the local Kafka cluster using the following command:

```sh
$ docker-compose up
```
Runs a [Confluent Control Center](https://docs.confluent.io/platform/current/control-center/index.html) that exposes a UI at `http://localhost:9021/` .

We can follow the startup by monitoring the output :
```shell
docker-compose logs -f
```



Now, follow either the **DSL example** or **Processor API example** instructions below, depending on which version of the demo you want to run.

## DSL example

You can run the high-level DSL example with the following command:
```sh
$ ./gradlew runDSL --info
```
or
```shell
java -jar ./build/libs/hello-streams-all.jar 
```
or
```shell
java -cp  ./build/libs/hello-streams-all.jar   com.example.DslExample
```

Once the dependencies are downloaded and the application is running (this may take a few minutes the first time you run the app, but will be much faster during subsequent runs), following the instructions under the __Producing Test Data__ section at the bottom of this README.

## Processor API example

You can run the low-level Processor API example with the following command:
```sh
$ ./gradlew runProcessorAPI --info
```
or
```shell
java -cp  ./build/libs/hello-streams-all.jar   com.example.ProcessorApiExample
```

Once the dependencies are downloaded and the application is running (this may take a few minutes the first time you run the app, but will be much faster during subsequent runs), following the instructions under the __Producing Test Data__ section below.

# Producing Test Data
Once the Kafka Streams application is running (either the DSL or Processor API version), open a new shell tab and produce some data to the source topic (`users`).

```sh
$ docker-compose exec kafka bash

$ kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic users
```

This will drop you in a prompt:

```sh
>
```

Now, type a few words, followed by `<ENTER>`.

```sh
>world
>izzy
```

You will see the following output if running the DSL example:
```sh
(DSL) Hello, world
(DSL) Hello, izzy
```

or slightly different output if running the Processor API example:
```sh
(Processor API) Hello, world
(Processor API) Hello, izzy
```
## Introducing Our Tutorial: Hello, Streams
### Creating a New Project

In addition to being able to compile and run your code, Gradle can also be used to quickly bootstrap new Kafka Streams applications that you build outside of this book. This can be accomplished by creating a directory for your project to live in and then by running the gradle init command from within that directory. An example of this workflow is as follows:
```shell
$ mkdir my-project && cd my-project

$ gradle init \
 --type java-application \
 --dsl groovy \
 --test-framework junit-jupiter \
 --project-name my-project \
 --package com.example
```
The source code for this book already contains the initialized project structure for each tutorial, so it’s not necessary to run gradle init unless you are starting a new project for yourself. We simply mention it here with the assumption that you will be writing your own Kafka Streams applications at some point, and want a quick way to bootstrap your next project.

Here is the basic project structure for a Kafka Streams application:
```shell
.
├── build.gradle 1
└── src
    ├── main
    │   ├── java 2
    │   └── resources 3
    └── test
        └── java 4
```
1
This is the project’s build file. It will specify all of the dependencies (including the Kafka Streams library) needed by our application.

2
We will save our source code and topology definitions in src/main/java.

3
src/main/resources is typically used for storing configuration files.

4
Our unit and topology tests, which we will discuss in “Testing Kafka Streams”, will live in src/test/java.

Now that we’ve learned how to bootstrap new Kafka Streams projects and have had an initial look at the project structure, let’s take a look at how to add Kafka Streams to our project.

### Adding the Kafka Streams Dependency
To start working with Kafka Streams, we simply need to add the Kafka Streams library as a dependency in our build file. (In Gradle projects, our build file is called build.gradle.) An example build file is shown here:
```json
plugins {
    id 'java'
    id 'application'
}

repositories {
    jcenter()
}

dependencies {
    implementation 'org.apache.kafka:kafka-streams:2.7.2' 1
}

task runDSL(type: JavaExec) { 2
    main = 'com.example.DslExample'
    classpath sourceSets.main.runtimeClasspath
}

task runProcessorAPI(type: JavaExec) { 3
    main = 'com.example.ProcessorApiExample'
    classpath sourceSets.main.runtimeClasspath
}
```
1
Add the Kafka Streams dependency to our project.

2
This tutorial is unique among others in this book since we will be creating two different versions of our topology. This line adds a Gradle task to execute the DSL version of our application.

3
Similarly, this line adds a Gradle task to execute the Processor API version of our application.

Now, to build our project (which will actually pull the dependency from the remote repository into our project), we can run the following command:

```shell
./gradlew build
```

That’s it! Kafka Streams is installed and ready to use. Now, let’s continue with the tutorial.

### DSL
The DSL example is exceptionally simple. We first need to use a Kafka Streams class called StreamsBuilder to build our processor topology:

```java
StreamsBuilder builder = new StreamsBuilder();
```

Next, as we learned in “``Processor Topologies``”, we need to add a source processor in order to read data from a Kafka topic (in this case, our topic will be called users). There are a few different methods we could use here depending on how we decide to model our data (we will discuss different approaches in “``Streams and Tables``”), but for now, let’s model our data as a stream. The following line adds the source processor:

```java
KStream<Void, String> stream = builder.stream("users");1
```
1
We’ll discuss this more in the next chapter, but the generics in KStream<Void, String> refer to the key and value types. In this case, the key is empty (Void) and the value is a String type.

Now, it’s time to add a stream processor. Since we’re just printing a simple greeting for each message, we can use the foreach operator with a simple lambda like so:
```java
stream.foreach(
    (key, value) -> {
        System.out.println("(DSL) Hello, " + value);
    });
```
Finally, it’s time to build our topology and start running our stream processing application:
```java
KafkaStreams streams = new KafkaStreams(builder.build(), config);
streams.start();
```
The full code, including some boilerplate needed to run the program, is shown in Example 2-1.

Example 2-1. Hello, world—DSL example
```java
class DslExample {

  public static void main(String[] args) {
    StreamsBuilder builder = new StreamsBuilder(); 1

    KStream<Void, String> stream = builder.stream("users"); 2

    stream.foreach( 3
        (key, value) -> {
          System.out.println("(DSL) Hello, " + value);
        });

    // omitted for brevity
    Properties config = ...; 4

    KafkaStreams streams = new KafkaStreams(builder.build(), config); 5
    streams.start();

    // close Kafka Streams when the JVM shuts down (e.g., SIGTERM)
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close)); 6
  }
}
```

1
The builder is used to construct the topology.

2
Add a source processor that reads from the users topic.

3
Use the DSL’s foreach operator to print a simple message. The DSL includes many operators that we will be exploring in upcoming chapters.

4
We have omitted the Kafka Streams configuration for brevity, but will discuss this in upcoming chapters. Among other things, this configuration allows us to specify which Kafka cluster our application should read from and what consumer group this application belongs to.

5
Build the topology and start streaming.

6
Close Kafka Streams when the JVM shuts down.

To run the application, simply execute the following command:
```shell
gradle runDSL --info
```
This is as same as 
```shell
java -jar ./build/libs/hello-streams-all.jar 
```
or
```shell
java -cp  ./build/libs/hello-streams-all.jar   com.example.DslExample
```


Now your Kafka Streams application is running and listening for incoming data. As you may recall from “Hello, Kafka”, we can produce some data to our Kafka cluster using the kafka-console-producer console script. To do this, run the following commands:
```shell
$ docker-compose exec kafka bash 1

[appuser@kafka ~]$ kafka-console-producer --bootstrap-server localhost:9092 --topic users  2
```
1
The console scripts are available in the kafka container, which is running the broker in our development cluster. You can also download these scripts as part of the official Kafka distribution.

2
Start a local producer that will write data to the users topic.

Once you are in the producer prompt, create one or more records by typing the name of the user, followed by the Enter key. When you are finished, press Control-C on your keyboard to exit the prompt:
```shell
>angie
>guy
>kate
>mark
```
Your Kafka Streams application should emit the following greetings:
```shell
(DSL) Hello, angie
(DSL) Hello, guy
(DSL) Hello, kate
(DSL) Hello, mark
```

We have now verified that our application is working as expected. We will explore some more interesting use cases over the next several chapters, but this process of defining a topology and running our application is a foundation we can build upon. Next, let’s look at how to create the same Kafka Streams topology with the lower-level Processor API.

### Processor API
The Processor API lacks some of the abstractions available in the high-level DSL, and its syntax is more of a direct reminder that we’re building processor topologies, with methods like Topology.addSource, Topology.addProcessor, and Topology.addSink (the latter of which is not used in this example). The first step in using the processor topology is to instantiate a new Topology instance, like so:

Topology topology = new Topology();
Next, we will create a source processor to read data from the users topic, and a stream processor to print a simple greeting. The stream processor references a class called SayHelloProcessor that we’ll implement shortly:
```java
topology.addSource("UserSource", "users"); 1
topology.addProcessor("SayHello", SayHelloProcessor::new, "UserSource"); 2
```

1
The first argument for the addSource method is an arbitrary name for this stream processor. In this case, we simply call this processor UserSource. We will refer to this name in the next line when we want to connect a child processor, which in turn defines how data should flow through our topology. The second argument is the topic name that this source processor should read from (in this case, users).

2
This line creates a new downstream processor called SayHello whose processing logic is defined in the SayHelloProcessor class (we will create this in the next section). In the Processor API, we can connect one processor to another by specifying the name of the parent processor. In this case, we specify the UserSource processor as the parent of the SayHello processor, which means data will flow from the UserSource to SayHello.

As we saw before, in the DSL tutorial, we now need to build the topology and call streams.start() to run it:
```java
KafkaStreams streams = new KafkaStreams(topology, config);
streams.start();
```
Before running the code, we need to implement the SayHelloProcessor class. Whenever you build a custom stream processor using the Processor API, you need to implement the Processor interface. The interface specifies methods for initializing the stream processor (init), applying the stream processing logic to a single record (process), and a cleanup function (close). The initialization and cleanup function aren’t needed in this example.

The following is a simple implementation of SayHelloProcessor that we will use for this example. We will explore more complex examples, and all of the interface methods in the Processor interface (init, process, and close), in more detail in [Chapter 7](../chapter-07/).
```java
public class SayHelloProcessor implements Processor<Void, String, Void, Void> { 1
  @Override
  public void init(ProcessorContext<Void, Void> context) {} 2

  @Override
  public void process(Record<Void, String> record) { 3
    System.out.println("(Processor API) Hello, " + record.value());
  }

  @Override
  public void close() {} 4
}
```
1
The first two generics in the Processor interface (in this example, Processor<Void, String, ..., ...>) refer to the input key and value types. Since our keys are null and our values are usernames (i.e., text strings), Void and String are the appropriate choices. The last two generics (Processor<..., ..., Void, Void>) refer to the output key and value types. In this example, our SayHelloProcessor simply prints a greeting. Since we aren’t forwarding any output keys or values downstream, Void is the appropriate type for the final two generics.24

2
No special initialization is needed in this example, so the method body is empty. The generics in the ProcessorContext interface (ProcessorContext<Void, Void>) refer to the output key and value types (again, as we’re not forwarding any messages downstream in this example, both are Void).

3
The processing logic lives in the aptly named process method in the Processor interface. Here, we print a simple greeting. Note that the generics in the Record interface refer to the key and value type of the input records.

4
No special cleanup needed in this example.

We can now run the code using the same command we used in the DSL example:
```shell
./gradlew runProcessorAPI --info
```
or
```shell
java -cp  ./build/libs/hello-streams-all.jar   com.example.ProcessorApiExample
```

You should see the following output to indicate your Kafka Streams application is working as expected:

```shell
(Processor API) Hello, angie
(Processor API) Hello, guy
(Processor API) Hello, kate
(Processor API) Hello, mark
```

Now, despite the Processor API’s power, which we will see in Chapter 7, using the DSL is often preferable because, among other benefits, it includes two very powerful abstractions: streams and tables. We will get our first look at these abstractions in the next section.

### Streams and Tables
If you look closely at Example 2-1, you will notice that we used a DSL operator called stream to read a Kafka topic into a stream. The relevant line of code is:
```java
KStream<Void, String> stream = builder.stream("users");
```
However, kafka streams also supports an additional way to view our data: as a table. in this section, we’ll take a look at both options and learn when to use streams and when to use tables.

As discussed in “Processor Topologies”, designing a processor topology involves specifying a set of source and sink processors, which correspond to the topics your application will read from and write to. However, instead of working with Kafka topics directly, the Kafka Streams DSL allows you to work with different representations of a topic, each of which are suitable for different use cases. There are two ways to model the data in your Kafka topics: as a stream (also called a record stream) or a table (also known as a changelog stream). The easiest way to compare these two data models is through an example.

Say we have a topic containing ssh logs, where each record is keyed by a user ID as shown in Table 2-2.

Table 2-2. Keyed records in a single topic-partition
| Key      | Value                  | Offset |
|----------|------------------------|--------|
| mitch    | { "action": "login" }  |  0     |
| mitch    | { "action": "logout" } | 1 |
| elyse    | { "action": "login" }  | 2 |
| isabelle | { "action": "login" }  | 3 |

Before consuming this data, we need to decide which abstraction to use: a stream or a table. When making this decision, we need to consider whether or not we want to track only the latest state/representation of a given key, or the entire history of messages. Let’s compare the two options side by side:

#### Streams
        These can be thought of as inserts in database parlance. Each distinct record remains in this view of the log. The stream representation of our topic can be seen in Table 2-3.

        Table 2-3. Stream view of ssh logs
| Key      | Value                  | Offset |
|----------|------------------------|--------|
| mitch    | { "action": "login" }  |  0     |
| mitch    | { "action": "logout" } | 1 |
| elyse    | { "action": "login" }  | 2 |
| isabelle | { "action": "login" }  | 3 |

#### Tables
        Tables can be thought of as updates to a database. In this view of the logs, only the current state (either the latest record for a given key or some kind of aggregation) for each key is retained. Tables are usually built from compacted topics (i.e., topics that are configured with a cleanup.policy of compact, which tells Kafka that you only want to keep the latest representation of each key). The table representation of our topic can be seen in Table 2-4.

        Table 2-4. Table view of ssh logs
| Key      | Value                  | Offset |
|----------|------------------------|--------|
| mitch    | { "action": "logout" } | 1 |
| elyse    | { "action": "login" }  | 2 |
| isabelle | { "action": "login" }  | 3 |

Tables, by nature, are stateful, and are often used for performing aggregations in Kafka Streams.25 In Table 2-4, we didn’t really perform a mathematical aggregation, we just kept the latest ssh event for each user ID. However, tables also support mathematical aggregations. For example, instead of tracking the latest record for each key, we could have just as easily calculated a rolling count. In this case, we would have ended up with a slightly different table, where the values contain the result of our count aggregation. You can see a count-aggregated table in Table 2-5.

Table 2-5. Aggregated table view of ssh logs
| Key      | Value                  | Offset |
|----------|------------------------|--------|
| mitch    | 2 | 1 |
| elyse    | 1  | 2 |
| isabelle | 1  | 3 |

Careful readers may have noticed a discrepancy between the design of Kafka’s storage layer (a distributed, append-only log) and a table. Records that are written to Kafka are immutable, so how is it possible to model data as updates, using a table representation of a Kafka topic?

The answer is simple: the table is materialized on the Kafka Streams side using a ``key-value store`` which, by default, is implemented using RocksDB.26 By consuming an ordered stream of events and keeping only the latest record for each key in the client-side ``key-value store`` (more commonly called a state store in Kafka Streams terminology), we end up with a table or map-like representation of the data. In other words, the table isn’t something we consume from Kafka, but something we build on the client side.

You can actually write a few lines of Java code to implement this basic idea. In the following code snippet, the List represents a stream since it contains an ordered collection of records,27 and the table is constructed by iterating through the list (stream.forEach) and only retaining the latest record for a given key using a Map. The following Java code demonstrates this basic idea:

```java
import java.util.Map.Entry;

var stream = List.of(
    Map.entry("a", 1),
    Map.entry("b", 1),
    Map.entry("a", 2));

var table = new HashMap<>();

stream.forEach((record) -> table.put(record.getKey(), record.getValue()));
```
If you were to print the stream and table after running this code, you would see the following output:
```shell
stream ==> [a=1, b=1, a=2]

table ==> {a=2, b=1}
```
Of course, the Kafka Streams implementation of this is more sophisticated, and can leverage fault-tolerant data structures as opposed to an in-memory Map. But this ability to construct a table representation of an unbounded stream is only one side of a more complex relationship between streams and tables, which we will explore next.

### Stream/Table Duality
The ``duality`` of tables and streams comes from the fact that tables can be represented as streams, and streams can be used to reconstruct tables. We saw the latter transformation of a stream into a table in the previous section, when discussing the discrepancy between Kafka’s append-only, immutable log and the notion of a mutable table structure that accepts updates to its data.

This ability to reconstruct tables from streams isn’t unique to Kafka Streams, and is in fact pretty common in various types of storage. For example, MySQL’s replication process relies on the same notion of taking a stream of events (i.e., row changes) to reconstruct a source table on a downstream replica. Similarly, Redis has the notion of an append-only file (AOF) that captures every command that is written to the in-memory key-value store. If a Redis server goes offline, then the stream of commands in the AOF can be replayed to reconstruct the dataset.

What about the other side of the coin (representing a table as a stream)? When viewing a table, you are viewing a single point-in-time representation of a stream. As we saw earlier, tables can be updated when a new record arrives. By changing our view of the table to a stream, we can simply process the update as an insert, and append the new record to the end of the log instead of updating the key. Again, the intuition behind this can be seen using a few lines of Java code:
```java
var stream = table.entrySet().stream().collect(Collectors.toList());

stream.add(Map.entry("a", 3));
```
This time, if you print the contents of the stream, you’ll see we’re no longer using update semantics, but instead insert semantics:
```shell
stream ==> [a=2, b=1, a=3]
```

So far, we’ve been working with the standard libraries in Java to build intuition around streams and tables. However, when working with streams and tables in Kafka Streams, you’ll use a set of more specialized abstractions. We’ll take a look at these abstractions next.

### KStream, KTable, GlobalKTable
One of the benefits of using the high-level DSL over the lower-level Processor API in Kafka Streams is that the former includes a set of abstractions that make working with streams and tables extremely easy.

The following list includes a high-level overview of each:

#### KStream
        A KStream is an abstraction of a partitioned record stream, in which data is represented using insert semantics (i.e., each event is considered to be independent of other events).

#### KTable
        A KTable is an abstraction of a partitioned table (i.e., changelog stream), in which data is represented using update semantics (the latest representation of a given key is tracked by the application). Since KTables are partitioned, each Kafka Streams task contains only a subset of the full table.28

#### GlobalKTable
        This is similar to a KTable, except each GlobalKTable contains a complete (i.e., unpartitioned) copy of the underlying data. We’ll learn when to use KTables and when to use GlobalKTables in Chapter 4.

Kafka Streams applications can make use of multiple stream/table abstractions, or just one. It’s entirely dependent on your use case, and as we work through the next few chapters, you will learn when to use each one. This completes our initial discussion of streams and tables, so let’s move on to the next chapter and explore Kafka Streams in more depth.

# Summary
Congratulations, you made it through the end of your first date with Kafka Streams. Here’s what you learned:

* Kafka Streams lives in the stream processing layer of the Kafka ecosystem. This is where sophisticated data processing, transformation, and enrichment happen.
* Kafka Streams was built to simplify the development of stream processing applications with a simple, functional API and a set of stream processing primitives that can be reused across projects. When more control is needed, a lower-level Processor API can also be used to define your topology.
* Kafka Streams has a friendlier learning curve and a simpler deployment model than cluster-based solutions like Apache Flink and Apache Spark Streaming. It also supports event-at-a-time processing, which is considered true streaming.
* Kafka Streams is great for solving problems that require or benefit from real-time decision making and data processing. Furthermore, it is reliable, maintainable, scalable, and elastic.
* Installing and running Kafka Streams is simple, and the code examples in this chapter can be found at https://github.com/mitch-seymour/mastering-kafka-streams-and-ksqldb.

In the next chapter, we’ll learn about stateless processing in Kafka Streams. We will also get some hands-on experience with several new DSL operators, which will help us build more advanced and powerful stream processing applications.

---------------------------------------------------------------------------------------------------------------------
We are referring to the official ecosystem here, which includes all of the components that are maintained under the Apache Kafka project.

2 Jay Kreps, one of the original authors of Apache Kafka, discussed this in detail in an O’Reilly blog post back in 2014.

3 This includes aggregated streams/tables, which we’ll discuss later in this chapter.

4 We have an entire chapter dedicated to time, but also see Matthias J. Sax’s great presentation on the subject from Kafka Summit 2019.

5 Guozhang Wang, who has played a key role in the development of Kafka Streams, deserves much of the recognition for submitting the original KIP for what would later become Kafka Streams. See https://oreil.ly/l2wbc.

6 Kafka Connect veered a little into event processing territory by adding support for something called single message transforms, but this is extremely limited compared to what Kafka Streams can do.

7 Kafka Streams will work with other JVM-based languages as well, including Scala and Kotlin. However, we exclusively use Java in this book.

8 Multiple consumer groups can consume from a single topic, and each consumer group processes messages independently of other consumer groups.

9 While partitions can be added to an existing topic, the recommended pattern is to create a new source topic with the desired number of partitions, and to migrate all of the existing workloads to the new topic.

10 Including some features that are specific to stateful applications, which we will discuss in Chapter 4.

11 The ever-growing nature of the stream processing space makes it difficult to compare every solution to Kafka Streams, so we have decided to focus on the most popular and mature stream processing solutions available at the time of this writing.

12 Although there is an open, albeit dated, proposal to support batch processing in Kafka Streams.

13 Including a Kafka-backed relational database called KarelDB, a graph analytics library built on Kafka Streams, and more. See https://yokota.blog.

14 At the time of this writing, Apache Flink had recently released a beta version of queryable state, though the API itself was less mature and came with the following warning in the official Flink documentation: “The client APIs for queryable state are currently in an evolving state and there are no guarantees made about stability of the provided interfaces. It is likely that there will be breaking API changes on the client side in the upcoming Flink versions.” Therefore, while the Apache Flink team is working to close this gap, Kafka Streams still has the more mature and production-ready API for querying state.

15 The exception to this is when topics are joined. In this case, a single topology will read from each source topic involved in the join without further dividing the step into sub-topologies. This is required for the join to work. See “Co-Partitioning” for more information.

16 In this example, we write to two intermediate topics (valid-mentions and invalid-mentions) and then immediately consume data from each. Using intermediate topics like this is usually only required for certain operations (for example, repartitioning data). We do it here for discussion purposes only.

17 A Java application may execute many different types of threads. Our discussion will simply focus on the stream threads that are created and managed by the Kafka Streams library for running a processor topology.

18 Remember, a Kafka Streams topology can be composed of multiple sub-topologies, so to get the number of tasks for the entire program, you should sum the task count across all sub-topologies.

19 This doesn’t mean a poorly implemented stream processor is immune from concurrency issues. However, by default, the stream threads do not share any state.

20 For example, the number of cores that your application has access to could inform the number of threads you decide to run with. If your application instance is running on a 4-core machine and your topology supports 16 tasks, you may want to configure the thread count to 4, which will give you a thread for each core. On the other hand, if your 16-task application was running on a 48-core machine, you may want to run with 16 threads (you wouldn’t run with 48 since the upper bound is the task count, or in this case: 16).

21 From First Principles: Elon Musk on the Power of Thinking for Yourself.

22 If you want to verify and have telnet installed, you can run echo 'exit' | telnet localhost 29092. If the port is open, you should see “Connected to localhost” in the output.

23 Instructions for installing Gradle can be found at https://gradle.org. We used version 6.6.1 for the tutorials in this book.

24 This version of the Processor interface was introduced In Kafka Streams version 2.7 and deprecates an earlier version of the interface that was available in Kafka Streams 2.6 and earlier. In the earlier version of the Processor interface, only input types are specified. This presented some issues with type-safety checks, so the newer form of the Processor interface is recommended.

25 In fact, tables are sometimes referred to as aggregated streams. See “Of Streams and Tables in Kafka and Stream Processing, Part 1” by Michael Noll, which explores this topic further.

26 RocksDB is a fast, embedded key-value store that was originally developed at Facebook. We will talk more about RocksDB and key-value stores in Chapters 4–6.

27 To go even deeper with the analogy, the index position for each item in the list would represent the offset of the record in the underlying Kafka topic.

28 Assuming your source topic contains more than one partition.

