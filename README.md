# udp-proxy

Very simple udp proxy with custom protocol realization.

## Start app

Java 12.0.1 is required.

```./gradlew build```

After it build app jar file was created at build/libs/

Application start command
```java -jar udp-proxy-1.0-SNAPSHOT.jar```

The ```app.properties``` need to be near the application file.

## Change log configuration

use ```-D-Dlog4j.configurionFile=configuration-file-name```

Like this:
```java -Dlog4j.configurionFile=log4j2.xml -jar udp-proxy-1.0-SNAPSHOT.jar```

## GraalVM

The graalvm plugin were installed to have AOT compile

On my machine I have to manually additionally install native-image. For this you need go to graal bin folder and execute next command ```gu install native-image```
See additional [info](https://www.graalvm.org/docs/getting-started/#native-images)

```./gradlew nativeimage``` - execute to have binary file in ```build/graal/``` location

## Known issues

1. It is no instantly blocking of income requests when proxying server no response in time.
    It is because cache no clean just in time, it have some delay.

TODO's
1. Есть смысл сделать возможность выбора типа используемых IO операций для netty (OIO, NIO, EPOLL). Сейчас используется EPOLL
