# Metalock Java library

The library contains Java annotations and aspects that make it possible to synchronize methods in a simple way.

Actually it provides some kind of Named Locks.

Internally it is based on concurrent maps of ordered reentrant locks.

## Install

Metalock jars are available at the Maven Central via this dependency:

```xml
<dependency>
  <groupId>io.github.xantorohara</groupId>
  <artifactId>metalock</artifactId>
  <version>0.1.1</version>
</dependency>
```

You always free to build it from the sources via this command:

`mvn clean install`

Or just download jars directly from the [target](https://github.com/xantorohara/metalock/tree/master/target) location.

Previous releases are available on the [release page](https://github.com/xantorohara/metalock/releases)
and at the [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cmetalock).

## Method level annotations
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
but with ability to lock entities by some fields,
and when the given entity is not yet persisted in the database.
```java
@MetaLock(name = "Record", param = "recordKey")
public SomeRecord saveRecord(String recordKey, String recordValue) {
    //do some work
}
```

## Aspects
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
Also it contains demo application medatata-app in the 
[examples](https://github.com/xantorohara/metalock/tree/master/examples) directory.
