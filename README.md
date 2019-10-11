[![Build Status](https://travis-ci.com/Blazebit/blaze-actor.svg?branch=master)](https://travis-ci.org/Blazebit/blaze-actor)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.blazebit/blaze-actor-core-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.blazebit/blaze-actor-core-api)
[![Slack Status](https://blazebit.herokuapp.com/badge.svg)](https://blazebit.herokuapp.com)

[![Javadoc - Actor](https://www.javadoc.io/badge/com.blazebit/blaze-actor-core-api.svg?label=javadoc%20-%20actor-api)](http://www.javadoc.io/doc/com.blazebit/blaze-actor-core-api)

Blaze-Actor
==========
Blaze-Actor is a lightweight framework that can be used for implementing actors that are optionally cluster aware.

What is it?
===========

Blaze-Actor provides a small runtime for implementing actors with clustering support.

Although message passing is usually a central aspect for actor frameworks, Blaze-Actor does not try to force a messaging interface on actors.
The only message passing that is possible is through the clustering API which allows to fire cluster wide events that can be processed.
You can see Blaze-Actor as something like a scheduler API that allows for dynamic rescheduling of named single-threaded tasks within a cluster.
It has a notion of _consuming actors_ that represent message consumers and provides a few connectors, but consuming actors is still very early.

Blaze-Actor is the foundation of [Blaze-Job](https://github.com/Blazebit/blaze-job) and thus also for [Blaze-Notify](https://github.com/Blazebit/blaze-notify).

Features
==============

Blaze-Actor has support for

* Scheduling of named actors
* Cluster events for actor rescheduling or custom events
* Integration with Spring or Java EE schedulers
* Message consumer implementations for PostgreSQL LISTEN/NOTIFY, AWS SQS

How to use it?
==============

Blaze-Actor is split up into different modules. We recommend that you define a version property in your parent pom that you can use for all artifacts. Modules are all released in one batch so you can safely increment just that property. 

```xml
<properties>
    <blaze-actor.version>1.0.0-SNAPSHOT</blaze-actor.version>
</properties>
```

Alternatively you can also use our BOM in the `dependencyManagement` section.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.blazebit</groupId>
            <artifactId>blaze-actor-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>    
    </dependencies>
</dependencyManagement>
```

## Manual setup

For compiling you will only need API artifacts and for the runtime you need impl and integration artifacts.

Blaze-Actor Core module dependencies

```xml
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-core-api</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-core-impl</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>runtime</scope>
</dependency>
```

Blaze-Actor Declarative module dependencies

```xml
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-declarative-api</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-declarative-impl</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>runtime</scope>
</dependency>
```

Blaze-Actor Scheduler integration with `ScheduledExecutorService`

```xml
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-scheduler-executor</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>compile</scope>
</dependency>
```

Blaze-Actor Scheduler integration with Springs `TaskScheduler`

```xml
<dependency>
    <groupId>com.blazebit</groupId>
    <artifactId>blaze-actor-scheduler-spring</artifactId>
    <version>${blaze-actor.version}</version>
    <scope>compile</scope>
</dependency>
```

Documentation
=========

Currently there is no documentation other than the Javadoc.
 
Core quick-start
=================

To work with Blaze-Actor, a `ActorContext` with properly configured scheduler is needed.  

```java
ActorContext actorContext = ActorContext.builder()
    .withProperty(ExecutorServiceScheduler.EXECUTOR_SERVICE_PROPERTY, Executors.newSingleThreadScheduledExecutor())
    .createContext();
actorContext.getActorManager().registerActor("test", () -> {
    System.out.println("Hello from Actor");
    return ActorRunResult.rescheduleIn(1000L);
});
```

That's a simple actor that reschedules every 1000ms.

Declarative usage
=================

The declarative module allows to register actors just through their class or thanks to integrations with DI frameworks can avoid manual registration.

```java
@ActorConfig(name = "test")
class MyActor implements ScheduledActor {
  @Override
  public ActorRunResult work() {
    System.out.println("Hello from Actor");
    return ActorRunResult.rescheduleIn(1000L);
  }
}
```

Licensing
=========

This distribution, as a whole, is licensed under the terms of the Apache
License, Version 2.0 (see LICENSE.txt).

References
==========

Project Site:              https://actor.blazebit.com (coming at some point)
