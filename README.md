# Metalock Java library

The library contains Java annotations and aspects 
that make it possible to synchronize methods in a simple way.

Actually it provides some kind of Named Locks.

Internally it is based on concurrent maps of reentrant locks.


## Method leval annotations
### @NameLock
It is possible to do locks similar to "Database Table level locking"

```java
@NameLock("SOME_TABLE_NAME")
public SomeEntity saveEntity() {
    //do some work
}
```

### @MetaLock
It is possible to do locks similar to "Database Row level locking",
but with ability to lock entities by some values,
and when the given entity is not yet persisted in the database.
```java
@MetaLock(name = "Record", param = "recordKey")
public SomeRecord saveRecord(String recordKey, String recordValue) {
    //do some work
}
```

### Aspects
Metalock itself is a plain Java 8 library, but it uses Spring Framework for unit testing.
Demo application also based on Spring. Spring is perfect.

Aspects for the annotations above can be enabled via Spring's Java Based Configuration:
```java
@Configuration
@EnableAspectJAutoProxy
public class ApplicationConfiguration {
    @Bean
    public MetaLockAspect getMetaLockAspect() {
        return new MetaLockAspect();
    }

    @Bean
    public NameLockAspect getNamedLockAspect() {
        return new NameLockAspect();
    }
}
```

or via application xml:

```xml
<aop:aspectj-autoproxy/>
<bean id="metaLockAspect" class="io.github.xantorohara.metalock.MetaLockAspect"/>
<bean id="nameLockAspect" class="io.github.xantorohara.metalock.NameLockAspect"/>
```

## Examples

This library has several unit-tests that demonstrates some cases.
Also it contains demo application medatata-app in the "examples" directory.


## Download
- [Metalock sources](https://github.com/xantorohara/metalock/archive/master.zip)
- [Metalock jar](https://github.com/xantorohara/metalock/raw/master/target/metalock-0.1.0-SNAPSHOT.jar)
- [Metalock pom](https://github.com/xantorohara/metalock/raw/master/target/pom.xml)

## Install

### Install to local Maven repository
- Compile and install from sources using this command:

`mvn clean install`

- Install jar using this command:

`mvn install:install-file -Dfile=metalock-0.1.0-SNAPSHOT.jar -DgroupId=io.github.xantorohara
-DartifactId=metalock -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar`

- or this:

`mvn install:install-file -Dfile=metalock-0.1.0-SNAPSHOT.jar -DpomFile=pom.xml`


