# Mastering Kafka Streams and ksqlDB
Code repository for the upcoming O'Reilly book: [Mastering Kafka Streams and ksqlDB][book] by Mitch Seymour

<a href="https://www.kafka-streams-book.com/"><img src="https://mcusercontent.com/987def4caf0bb040419d778f2/images/81c6be7f-c833-4e12-a893-22545aaf7304.jpg" width="300"></a>

[book]: https://www.kafka-streams-book.com/

# Available Editions and Versions
| Edition | Kafka Streams version | ksqlDB version | Publication Date | Branch |
| --------| --------------------- | ---------------| -----------------| -------|
| Early Release| 2.6.0 | 0.12.0| May 2020 | [early-release][early-release] |
| [1st Edition][amzn]| 2.7.0 | 0.14.0| February 2021 | [1st-edition][1st-edition] |

[amzn]: https://www.amazon.com/gp/product/1492062499/ref=as_li_tl?ie=UTF8&camp=1789&creative=9325&creativeASIN=1492062499&linkCode=as2&tag=mitchseymour-20&linkId=28979f4bdca5bc57af5ed2f8962d4d12
[early-release]: https://github.com/mitch-seymour/mastering-kafka-streams-and-ksqldb/tree/early-release
[1st-edition]: https://github.com/mitch-seymour/mastering-kafka-streams-and-ksqldb/tree/1st-edition


[Confluent Platform and Apache Kafka Compatibility](https://docs.confluent.io/platform/current/installation/versions-interoperability.html#cp-and-apache-ak-compatibility)
| Confluent Platform | Apache Kafka® | Release Date       | Standard End of Support | Platinum End of Support |
|--------------------|---------------|--------------------|-------------------------|-------------------------|
| 7.1.x              | 3.1.x         | April 5, 2022      | April 5, 2024           | April 5, 2025           |
| 7.0.x              | 3.0.x         | October 27, 2021   | October 27, 2023        | October 27, 2024        |
| 6.2.x              | 2.8.x         | June 8, 2021       | June 8, 2023            | June 8, 2024            |
| 6.1.x              | 2.7.x         | February 9, 2021   | February 9, 2023        | February 9, 2024        |
| 6.0.x              | 2.6.x         | September 24, 2020 | September 24, 2022      | September 24, 2023      |
| 5.5.x              | 2.5.x         | April 24, 2020     | April 24, 2022          | April 24, 2023          |
| 5.4.x              | 2.4.x         | January 10, 2020   | January 10, 2022        | January 10, 2023        |
| 5.3.x              | 2.3.x         | July 19, 2019      | July 19, 2021           | July 19, 2022           |
| 5.2.x              | 2.2.x         | March 28, 2019     | March 28, 2021          | March 28, 2022          |
| 5.1.x              | 2.1.x         | December 14, 2018  | December 14, 2020       | December 14, 2021       |
| 5.0.x              | 2.0.x         | July 31, 2018      | July 31, 2020           | July 31, 2021           |
| 4.1.x              | 1.1.x         | April 16, 2018     | April 16, 2020          | April 16, 2021          |
| 4.0.x              | 1.0.x         | November 28, 2017  | November 28, 2019       | November 28, 2020       |
| 3.3.x              | 0.11.0.x      | August 1, 2017     | August 1, 2019          | August 1, 2020          |
| 3.2.x              | 0.10.2.x      | March 2, 2017      | March 2, 2019           | March 2, 2020           |
| 3.1.x              | 0.10.1.x      | November 15, 2016  | November 15, 2018       | November 15, 2019       |
| 3.0.x              | 0.10.0.x      | May 24, 2016       | May 24, 2018            | May 24, 2019            |
| 2.0.x              | 0.9.0.x       | December 7, 2015   | December 7, 2017        | December 7, 2018        |
| 1.0.0              | –             | February 25, 2015  | February 25, 2017       | February 25, 2018       |

[Confluent for Kubernetes(CFK)](https://docs.confluent.io/platform/current/installation/versions-interoperability.html#operator-cp-compatibility)
| CFK Version | Compatible Confluent Platform Versions | Compatible Kubernetes Versions     | Release Date  | End of Support |
|-------------|----------------------------------------|------------------------------------|---------------|----------------|
| 2.3.x       | 7.0.x, 7.1.x                           | 1.18 - 1.23 (OpenShift 4.6 - 4.10) | April 5, 2022 | April 5, 2023  |
| 2.2.x       | 6.2.x, 7.0.x                           | 1.17 - 1.22 (OpenShift 4.6 - 4.9)  | Nov 3, 2021   | Nov 3, 2022    |
| 2.1.x       | 6.0.x, 6.1.x, 6.2.x                    | 1.17 - 1.22 (OpenShift 4.6 - 4.9)  | Oct 12, 2021  | Oct 12, 2022   |
| 2.0.x       | 6.0.x, 6.1.x, 6.2.x                    | 1.15 - 1.20                        | May 12, 2021  | May 12, 2022   |

[Kafka Client Compatibility](https://github.com/spring-cloud/spring-cloud-stream/wiki/Kafka-Client-Compatibility)
| Spring Cloud Stream Version | Spring for Apache Kafka Version | Spring Integration for Apache Kafka Version | kafka-clients | Spring Boot  | Spring Cloud |
|-----------------------------|---------------------------------|---------------------------------------------|---------------|--------------|--------------|
| 3.1.x (2020.0.x)            | 2.6.x                           | 5.4.x                                       | 2.6.x         | 2.4.x        | 2020.0.x     |
| 3.0.x (Horsham)*            | 2.5.x, 2.3.x                    | 3.3.x, 3.2.x                                | 2.5.x, 2.3.x  | 2.3.x, 2.2.x | Hoxton*      |


[upgrade-guide](https://kafka.apache.org/31/documentation/streams/upgrade-guide)

# Chapter Tutorials
  - [Chapter 1](./chapter-01) - _A Rapid Introduction to Kafka_
  - [Chapter 2](./chapter-02) - _Getting Started with Kafka Streams_
  - [Chapter 3](./chapter-03) - _Stateless Processing (Sentiment Analysis of Cryptcurreny Tweets)_
  - [Chapter 4](./chapter-04) - _Stateful Processing (Video game leaderboard)_
  - [Chapter 5](./chapter-05) - _Windows and Time (Patient Monitoring / Infection detection application)_
  - [Chapter 6](./chapter-06) - _Advanced State Management_
  - [Chapter 7](./chapter-07) - _Processor API (Digital Twin / IoT application)_
  - [Chapter 8](./chapter-08) - _Getting Started with ksqlDB_
  - [Chapter 9](./chapter-09) - _Data Integration with ksqlDB and Kafka Connect_
  - [Chapter 10](./chapter-10) - _Stream Processing Basics with ksqlDB (Netflix Change Tracking - Part I)_
  - [Chapter 11](./chapter-11) - _Intermediate Stream Processing with ksqlDB (Netflix Change Tracking - Part II)_
  - [Chapter 12](./chapter-12) - _The Road to Production_

# Why read this book?

- Kafka Streams and ksqlDB greatly simplify the process of building stream processing applications
- As an added benefit, they are also both extremely fun to use
- Kafka is the [fourth fastest growing tech skill][indeed] mentioned in job postings from 2014-2019. Sharpening your skills in this area has career benefits
- By learning Kafka Streams and ksqlDB, you will be well prepared for tackling a wide-range of business problems, including: streaming ETL, data enrichment, anomaly detection, data masking, data filtering, and more


[indeed]: https://www.techrepublic.com/article/the-20-fastest-rising-and-sharpest-declining-tech-skills-of-the-past-5-years/


# Support this book
- Star this repo
- Follow [@kafka_book][twitter] on Twitter (character limits... sigh)
- Provide feedback on the book and code by either:
  - [filling out a 6 question survey][survey], or
  - emailing author@kafka-streams-book.com
- Subscribe to an early preview of additional chapters from the website, [kafka-streams-book.com][website]
- Share the book, website, and/or code with your friends

[survey]: https://kafka-streams-book.typeform.com/to/TWuRwK
[twitter]: https://twitter.com/kafka_book
[website]: https://www.kafka-streams-book.com/

# A proposed Kafka maturity model
For a comparison, check out the Confluent white paper titled, “[Five Stages to Streaming Platform Adoption](https://assets.confluent.io/m/41f3c9186d4adb03/original/20180927-WP-Five-Stages_to_Streaming_Platform_Adoption.pdf?ajs_aid=4224d8d2-95b7-4b07-92d7-0dba251be61e&_ga=2.84813978.2024891929.1650607704-1763164608.1648258250)” , which presents a different perspective that encompasses five stages of their streaming maturity model with distinct criteria for each stage . 
