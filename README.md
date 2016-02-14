# Metalock library
This library provides some annotations and aspects that make it possible to do some syncronisation work in a simple way.

- @NameLock
It is possible to do locks like "Table level"
- @MetaLock
It is possible to do locks like "Row level"

## Download
- [Metalock sources](https://github.com/xantorohara/metalock/archive/master.zip)
- [Metalock jar](https://github.com/xantorohara/metalock/raw/master/target/metalock-0.1.0-SNAPSHOT.jar)

## Install

### Install to local Maven repository
#### Compile and install from sources using this command:

`mvn clean install`

#### Install jar file using this command:

`mvn install:install-file -Dfile=metalock-0.1.0-SNAPSHOT.jar -DgroupId=io.github.xantorohara
-DartifactId=metalock -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar`

