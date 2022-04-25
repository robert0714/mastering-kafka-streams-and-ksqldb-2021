# Chapter 4. Stateful Processing
Kafka Streams gives us the ability to capture and remember information about the events we consume. The captured information, or state, allows us to perform more advanced stream processing operations, including joining and aggregating data. In this chapter, we will explore stateful stream processing in detail. Some of the topics we will cover include:
* The benefits of stateful stream processing
* The differences between facts and behaviors
* What kinds of stateful operators are available in Kafka Streams
* How state is captured and queried in Kafka Streams
* How the KTable abstraction can be used to represent local, partitioned state
* How the GlobalKTable abstraction can be used to represent global, replicated state
* How to perform stateful operations, including joining and aggregating data
* How to use interactive queries to expose state

## Benefits of Stateful Processing
Stateful processing helps us understand the ``relationships between events`` and leverage these relationships for more advanced stream processing use cases. When we are able to understand how an event relates to other events, we can:
* Recognize patterns and behaviors in our event streams
* Perform aggregations
* Enrich data in more sophisticated ways using joins

Another benefit of stateful stream processing is that it gives us an additional abstraction for representing data. By replaying an event stream one event at a time, and saving the latest state of each key in an embedded key-value store, we can build a point-in-time representation of continuous and unbounded record streams. These point-in-time representations, or snapshots, are referred to as **tables**, and Kafka Streams includes different types of table abstractions that we’ll learn about in this chapter.

Tables are not only at the heart of stateful stream processing, but when they are materialized, they can also be queried. This ability to query a real-time snapshot of a fast-moving event stream is what makes Kafka Streams a stream-relational processing platform,[1](#reference) and enables us to not only build stream processing applications, but also low-latency, event-driven microservices as well.

Finally, stateful stream processing allows us to understand our data using more sophisticated mental models. One particularly interesting view comes from Neil Avery, who discusses the differences between facts and behaviors in his discussion of [event-first thinking](https://www.confluent.io/blog/journey-to-event-driven-part-1-why-event-first-thinking-changes-everything/):

```shell
An event represents a fact, something happened; it is immutable…
```
Stateless applications, like the ones we discussed in the previous chapter, are fact-driven. Each event is treated as an independent and atomic fact, which can be processed using immutable semantics (think of inserts in a never-ending stream), and then subsequently forgotten.

However, in addition to leveraging stateless operators to filter, branch, merge, and transform facts, we can ask even more advanced questions of our data if we learn how to model **behaviors** using stateful operators. So what are behaviors? According to Neil:

```shell
The accumulation of facts captures behavior.
```

You see, events (or facts) rarely occur in isolation in the real world. Everything is interconnected, and by capturing and remembering facts, we can begin to understand their meaning. This is possible by understanding events in their larger historical context, or by looking at other, related events that have been captured and stored by our application.

You see, events (or facts) rarely occur in isolation in the real world. Everything is interconnected, and by capturing and remembering facts, we can begin to understand their meaning. This is possible by understanding events in their larger historical context, or by looking at other, related events that have been captured and stored by our application.

A popular example is shopping cart abandonment, which is a behavior comprised of multiple facts: ``a user adds one or more items to a shopping cart, and then a session is terminated either manually (e.g., the user logs off) or automatically (e.g., due to a long period of inactivity)``. Processing either fact independently tells us very little about where the user is in the checkout process. However, collecting, remembering, and analyzing each of the facts (which is what stateful processing enables) allows us to recognize and react to the **behavior**, and provides much greater business value than viewing the world as a series of unrelated events.

Now that we understand the benefits of stateful stream processing, and the differences between facts and behaviors, let’s get a preview of the stateful operators in Kafka Streams.

## Preview of Stateful Operators
Kafka Streams includes several stateful operators that we can use in our processor topologies. Table 4-1 includes an overview of several operators that we will be working with in this book.

Table 4-1. Stateful operators and their purpose
<table>
    <tr>
        <td>Use case</td>
        <td>Purpose</td>
        <td>Operators</td>
    </tr>
    <tr>
        <td>Joining data</td>
        <td>Enrich an event with additional&nbsp;information or context that was captured in a separate stream or table</td>
        <td>join(inner join)<br/>leftJoin<br/>outerJoin</td>
    </tr>
    <tr>
        <td>Aggregating data</td>
        <td>Compute a&nbsp;continuously updating mathematical or combinatorial transformation of related events</td>
        <td>aggregate<br/>count<br/>reduce</td>
    </tr>
    <tr>
        <td>Windowing data</td>
        <td>Group events that have close temporal proximity</td>
        <td>windowedBy</td>
    </tr>
</table>
Furthermore, we can combine stateful operators in Kafka Streams to understand even more complex relationships/behaviors between events. For example, performing a ``windowed join`` allows us to understand how discrete event streams relate during a certain period of time. As we’ll see in the next chapter, ``windowed aggregations`` are another useful way of combining stateful operators.

Now, compared to the stateless operators we encountered in the previous chapter, stateful operators are more complex under the hood and have additional compute and storage[2](#reference) requirements. For this reason, we will spend some time learning about the inner workings of stateful processing in Kafka Streams before we start using the stateful operators listed in Table 4-1.

Perhaps the most important place to begin is by looking at how state is stored and queried in Kafka Streams.
## State Stores
We have already established that stateful operations require our application to maintain some memory of previously seen events. For example, an application that counts the number of error logs it sees needs to keep track of a single number for each key: a rolling count that gets updated whenever a new error log is consumed. This count represents the historical context of a record and, along with the record key, becomes part of the application’s state.

To support stateful operations, we need a way of storing and retrieving the remembered data, or state, required by each stateful operator in our application (e.g., **count**, **aggregate**, **join**, etc.). ``The storage abstraction that addresses these needs in Kafka Streams`` is called a ``state store``, and since a single Kafka Streams application can leverage many stateful operators, a single application may contain several state stores.

> **⚠ NOTE:**   This section provides some lower-level information about how state is captured and stored in Kafka Streams. If you are eager to get started with the tutorial, feel free to skip ahead to “[Introducing Our Tutorial: Video Game Leaderboard](#introducing-our-tutorial-video-game-leaderboard)” and revisit this section later.

There are many state store implementations and configuration possibilities available in Kafka Streams, each with specific advantages, trade-offs, and use cases. Whenever you use a stateful operator in your Kafka Streams application, it’s helpful to consider which type of state store is needed by the operator, and also how to configure the state store based on your optimization criteria (e.g., are you optimizing for high throughput, operational simplicity, fast recovery times in the event of failure, etc.). In most cases, Kafka Streams will choose a sensible default if you don’t explicitly specify a state store type or override a state store’s configuration properties.

Since the variation in state store types and configurations makes this quite a deep topic, we will initially focus our discussion on the common characteristics of all of the default state store implementations, and then take a look at the two broad categories of state stores: persistent and in-memory stores. More in-depth discussions of state stores will be covered in [Chapter 6](./../chapter-06/), and as we encounter specific topics in our tutorial.

### Common Characteristics
The default state store implementations included in Kafka Streams share some common properties. We will discuss these commonalities in this section to get a better idea of how state stores work.

#### Embedded
The default state store implementations that are included in Kafka Streams are ``embedded`` within your Kafka Streams application at the task level (we first discussed tasks in “[Tasks and Stream Threads](../chapter-02/README.md#task-and-streams-threads)”). The advantage of embedded state stores, as opposed to using an external storage engine, is that the latter would require a network call whenever state needed to be accessed, and would therefore introduce unnecessary latency and processing bottlenecks. Furthermore, since state stores are embedded at the task level, a whole class of concurrency issues for accessing shared state are eliminated.

Additionally, if state stores were remote, you’d have to worry about the availability of the remote system separately from your Kafka Streams application. Allowing Kafka Streams to manage a local state store ensures it will always be available and reduces the error surface quite a bit. A centralized remote store would be even worse, since it would become a single point of failure for all of your application instances. Therefore, Kafka Streams’ strategy of colocating an application’s state alongside the application itself not only improves performance (as discussed in the previous paragraph), but also availability.

All of the default state stores leverage RocksDB under the hood. RocksDB is a fast, embedded key-value store that was originally developed at Facebook. Since it supports arbitrary byte streams for storing key-value pairs, it works well with Kafka, which also decouples serialization from storage. Furthermore, both reads and writes are extremely fast, thanks to a rich set of optimizations that were made to the forked LevelDB code.[3](#reference)

#### Multiple access modes
State stores support multiple access modes and query patterns. Processor topologies require read and write access to state stores. However, when building microservices using Kafka Streams’ interactive queries feature, which we will discuss later in “Interactive Queries”, clients require only read access to the underlying state. This ensures that state is never mutable outside of the processor topology, and is accomplished through a dedicated read-only wrapper that clients can use to safely query the state of a Kafka Streams application.

### Fault tolerant
By default, state stores are backed by changelog topics in Kafka.[4](#reference) In the event of failure, state stores can be restored by replaying the individual events from the underlying changelog topic to reconstruct the state of an application. Furthermore, Kafka Streams allows users to enable standby replicas for reducing the amount of time it takes to rebuild an application’s state. These standby replicas (sometimes called shadow copies) make state stores redundant, which is an important characteristic of highly available systems. In addition, applications that allow their state to be queried can rely on standby replicas to serve query traffic when other application instances go down, which also contributes to high availability.

#### Key-based
Operations that leverage state stores are key-based. A record’s key defines the relationship between the current event and other events. The underlying data structure will vary depending on the type of state store you decide to use,[5](#reference) but each implementation can be conceptualized as some form of key-value store, where keys may be simple, or even compounded (i.e., multidimensional) in some cases.[6](#reference)

> **⚠ NOTE:**  To complicate things slightly, Kafka Streams explicitly refers to certain types of state stores as key-value stores, even though all of the default state stores are key-based. When we refer to key-value stores in this chapter and elsewhere in this book, we are referring to nonwindowed state stores (windowed state stores will be discussed in the next chapter).

Now that we understand the commonalities between the default state stores in Kafka Streams, let’s look at two broad categories of state stores to understand the differences between certain implementations.

### Persistent Versus In-Memory Stores
One of the most important differentiators between various state store implementations is whether or not the state store is **persistent**, or if it simply stores remembered information **in-memory** (RAM). Persistent state stores flush state to disk asynchronously (to a configurable **state directory**), which has two primary benefits:

* State can exceed the size of available memory.
* In the event of failure, persistent stores can be restored quicker than in-memory stores.

To clarify the first point, a persistent state store may keep some of its state in-memory, while writing to disk when the size of the state gets too big (this is called ``spilling to disk``) or when the write buffer exceeds a configured value. Second, since the application state is persisted to disk, Kafka Streams does not need to replay the entire topic to rebuild the state store whenever the state is lost (e.g., due to system failure, instance migration, etc.). It just needs to replay whatever data is missing between the time the application went down and when it came back up.

> **⚠ TIP:**  The state store directory used for persistent stores can be set using the ``StreamsConfig.STATE_DIR_CONFIG`` property. The default location is ``/tmp/kafka-streams``, but it is highly recommended that you override this to a directory outside of ``/tmp``.

The downside is that persistent state stores are operationally more complex and can be slower than a pure in-memory store, which always pulls data from RAM. The additional operational complexity comes from the secondary storage requirement (i.e., disk-based storage) and, if you need to tune the state store, understanding RocksDB and its configurations (the latter may not be an issue for most applications).

Regarding the performance gains of an in-memory state store, these may not be drastic enough to warrant their use (since failure recovery takes longer). Adding more partitions to parallelize work is always an option if you need to squeeze more performance out of your application. Therefore, my recommendation is to start with persistent stores and only switch to in-memory stores if you have measured a noticeable performance improvement and, when quick recovery is concerned (e.g., in the event your application state is lost), you are using standby replicas to reduce recovery time.

Now that we have some understanding of what state stores are, and how they enable stateful/behavior-driven processing, let’s take a look at this chapter’s tutorial and see some of these ideas in action.



# Introducing Our Tutorial: Video Game Leaderboard
We will learn about ``stateful processing`` by implementing a video game leaderboard with Kafka Streams. The video game industry is a prime example of where stream processing excels, since both gamers and game systems require low-latency processing and immediate feedback. This is one reason why companies like Activision (the company behind games like **Call of Duty** and remasters of **Crash Bandicoot and Spyro**) use Kafka Streams for processing video game telemetry.[7](#reference)

The leaderboard we will be building will require us to model data in ways that we haven’t explored yet. Specifically, we’ll be looking at how to use Kafka Streams’ table abstractions to model data as a sequence of updates. Then, we’ll dive into the topics of joining and aggregating data, which are useful whenever you need to understand or compute the relationship between multiple events. This knowledge will help you solve more complicated business problems with Kafka Streams.

Once we’ve created our real-time leaderboard using a new set of stateful operators, we will demonstrate how to query Kafka Streams for the latest leaderboard information using interactive queries. Our discussion of this feature will teach you how to build event-driven microservices with Kafka Streams, which in turn will broaden the type of clients we can share data with from our stream processing applications.8

Without further ado, let’s take a look at the architecture of our video game leaderboard. Figure 4-1 shows the topology design we’ll be implementing in this chapter. Additional information about each step is included after the diagram.

![The topology that we will be implementing in our stateful video game leaderboard application](./material/mksk_0401.png)  
Figure Figure 4-1. The topology that we will be implementing in our stateful video game leaderboard application

1. Our Kafka cluster contains three topics:
    * The score-events topic contains game scores. The records are unkeyed and are therefore distributed in a round-robin fashion across the topic’s partitions.
    * The players topic contains player profiles. Each record is keyed by a player ID.
    * The products topic contains product information for various video games. Each record is keyed by a product ID.
2. We need to enrich our score events data with detailed player information. We can accomplish this using a join.
3. Once we’ve enriched the score-events data with player data, we need to add detailed product information to the resulting stream. This can also be accomplished using a join.
4. Since grouping data is a prerequisite for aggregating, we need to group the enriched stream.
5. We need to calculate the top three high scores for each game. We can use Kafka Streams’ aggregation operators for this purpose.
6. Finally, we need to expose the high scores for each game externally. We will accomplish this by building a RESTful microservice using the interactive queries feature in Kafka Streams.

With our topology design in hand, we can now move on to the project setup.

# Video game leader board
This code corresponds with Chapter 4 in the upcoming O'Reilly book: [Mastering Kafka Streams and ksqlDB][book] by Mitch Seymour. This tutorial covers **Stateful processing** in Kafka Streams. Here, we demonstrate many stateful operators in Kafka Streams' high-level DSL by building an application a video game leaderboard.


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
Now, to run the Kafka Streams application, simply run:

```
./gradlew run --info
```

# Data Models
As always, we’ll start by defining our data models. Since the source topics contain JSON data, we will define our data models using POJO data classes, which we will serialize and deserialize using our JSON serialization library of choice (throughout this book, we use Gson, but you could easily use Jackson or another library).[9](#reference)

I like to group my data models in a dedicated package in my project, for example, c``om.magicalpipelines.model``. A filesystem view of where the data classes for this tutorial are located is shown here:
```shell
src/
└── main
    └── java
        └── com
            └── magicalpipelines
                └── model
                    ├── ScoreEvent.java 1
                    ├── Player.java 2
                    └── Product.java 3
```
1. The ScoreEvent.java data class will be used to represent records in the score-events topic.
2. The Player.java data class will be used to represent records in the players topic.
3. The Product.java data class will be used to represent records in the products topic.

Now that we know which data classes we need to implement, let’s create a data class for each topic. Table 4-2 shows the resulting POJOs that we have implemented for this tutorial.

Table 4-2. Example records and data classes for each topic
<table>
    <tr>
        <td>Kafka topic</td>
        <td>Example record</td>
        <td>Data class</td>
    </tr>
    <tr>
        <td>score-events</td>
        <td>{<br/>
&nbsp;&nbsp;"score": 422,<br/>
&nbsp;&nbsp;"product_id": 6,<br/>
&nbsp;&nbsp;"player_id": 1<br/>
}<br/>
</td>
        <td>public class ScoreEvent {<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private Long playerId;<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private Long productId;<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private Double score;<br/>
}</td>
    </tr>
    <tr>
        <td>players</td>
        <td>{<br/>
&nbsp;&nbsp;&nbsp;&nbsp;"id": 2,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;"name": "Mitch"<br/>
}</td>
        <td>public class Player {<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private Long id;<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private String name;<br/>
}</td>
    </tr>
    <tr>
        <td>products</td>
        <td>{<br/>
&nbsp;&nbsp;&nbsp;&nbsp;"id": 1,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;"name": "Super Smash Bros"<br/>
}</td>
        <td>public class Product {<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private Long id;<br/>
&nbsp;&nbsp;&nbsp;&nbsp;private String name;<br/>
}</td>
    </tr>
</table>

> **⚠ NOTE:**  We have already discussed serialization and deserialization in detail in “Serialization/Deserialization”. In that chapter’s tutorial, we implemented our own custom serializer, deserializer, and Serdes. We won’t spend more time on that here, but you can check out the code for this tutorial to see how we’ve implemented the Serdes for each of the data classes shown in Table 4-2.

# Producing Test Data
Once your application is running, you can produce some test data to see it in action. Since our video game leaderboard application reads from multiple topics (`players`, `products`, and `score-events`), we have saved example records for each topic in the `data/` directory. To produce data into each of these topics, open a new tab in your shell and run the following commands.

```sh
# log into the broker, which is where the kafka console scripts live
$ docker-compose exec kafka bash

# produce test data to players topic
$ kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic players \
  --property 'parse.key=true' \
  --property 'key.separator=|' < players.json

# produce test data to products topic
$ kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic products \
  --property 'parse.key=true' \
  --property 'key.separator=|' < products.json

# produce test data to score-events topic
$ kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic score-events < score-events.json
```

# Query the API
This application exposes the video game leaderboard results using Kafka Streams interactive queries feature. The API is listening on port `7000`. Note the following examples use `jq` to prettify the output. If you don't have `jq` installed, either [install it][jq] or remove that part of the command.

[jq]: https://stedolan.github.io/jq/download/

### Get all leaderboard entries, grouped by game (i.e. _productId_)

```sh
curl -s localhost:7000/leaderboard | jq '.'

# example output (truncated)
{
  "1": [
    {
      "playerId": 3,
      "productId": 1,
      "playerName": "Isabelle",
      "gameName": "Super Smash Bros",
      "score": 4000
    },
    ...
  ],
  "6": [
    {
      "playerId": 3,
      "productId": 6,
      "playerName": "Isabelle",
      "gameName": "Mario Kart",
      "score": 9000
    },
    ...
  ]
}
```

### Get the leaderboard for a specific game (i.e. _productId_)
```sh
curl -s localhost:7000/leaderboard/1 | jq '.'

# example output
[
  {
    "playerId": 3,
    "productId": 1,
    "playerName": "Isabelle",
    "gameName": "Super Smash Bros",
    "score": 4000
  },
  {
    "playerId": 2,
    "productId": 1,
    "playerName": "Mitch",
    "gameName": "Super Smash Bros",
    "score": 2000
  },
  {
    "playerId": 1,
    "productId": 1,
    "playerName": "Elyse",
    "gameName": "Super Smash Bros",
    "score": 1000
  }
]
```

# Reference