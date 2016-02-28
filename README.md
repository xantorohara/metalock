# Metalock Java library

The library contains Java annotations and aspects that make it possible to synchronize methods in a simple way.

Actually it provides some kind of "Named Locks".

Internally it is based on concurrent maps of ordered reentrant locks.

## Install

Metalock jars are available at the 
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cmetalock):

```xml
<dependency>
  <groupId>io.github.xantorohara</groupId>
  <artifactId>metalock</artifactId>
  <version>0.1.1</version>
</dependency>
```

Snapshots are available at the 
[Sonatype](https://oss.sonatype.org/content/repositories/snapshots/io/github/xantorohara/metalock/):
 
```xml
  <repositories>
        <repository>
            <id>oss.sonatype.org-snapshot</id>
            <url>http://oss.sonatype.org/content/repositories/snapshots</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    ...
    <dependencies>
        <dependency>
            <groupId>io.github.xantorohara</groupId>
            <artifactId>metalock</artifactId>
            <version>0.1.2-SNAPSHOT</version>
        </dependency>
    </dependencies>
```
    
You always free to build it from the sources via this command:

`mvn clean install`

Or just download jars directly from the [target](https://github.com/xantorohara/metalock/tree/master/target) location.

Previous releases are available on the [release page](https://github.com/xantorohara/metalock/releases).

## Method level annotations
### @NameLock

Synchronize processes by the constant value from the annotation (by "name"):

```java
@NameLock("SOME_TABLE_NAME")
public SomeEntity saveEntity() {
    //do some work
}
```

Use cases:
* Database transaction synchronisation (similar to "Table-level locking")
* Synchronize access to some global repository (e.g.: to the SVN repository)

### @MetaLock

Synchronize processes by the combination of the constant value from the annotation and 
the runtime value from the method parameter:

```java
@MetaLock(name = "Record", param = "recordKey")
public SomeRecord saveRecord(String recordKey, String recordValue) {
    //do some work
}
```

@Metalock supports multiple parameters:

```java
@MetaLock(name = "User", param = {"firstName", "lastName"})
public void addMoneyForUser(String firstName, String lastName, int amountOfMoney) {
    //do some work
}
```
   
@Metalock annotation can be repeatable:

```java
@MetaLock(name = "FileSystem", param = "filename")
@MetaLock(name = "SharedLocation", param = "filename")
public void writeData(String filename, String data) {
    //do some work
}
```

Use cases:
* Database transaction synchronisation (similar to "Row-level locking")
* Atomic writing to the file system

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

## Logging

Metalock may produce logs like this:

```txt
00:57:30.094 [Thread-2] DEBUG i.g.x.metalock.MetaLockAspect - ML1000001U DemoRegistryService.addMoneyForUser(..)
00:57:30.098 [Thread-2] TRACE i.g.x.metalock.MetaLockAspect - ML1000001U Locking User§Paul§Smith
00:57:30.100 [Thread-2] TRACE i.g.x.metalock.MetaLockAspect - ML1000001U Locked User§Paul§Smith
00:57:30.100 [Thread-2] DEBUG i.g.x.metalock.MetaLockAspect - ML1000001U Before
00:57:30.189 [Thread-3] DEBUG i.g.x.metalock.MetaLockAspect - ML1000002U DemoRegistryService.addMoneyForUser(..)
00:57:30.190 [Thread-3] TRACE i.g.x.metalock.MetaLockAspect - ML1000002U Locking User§Paul§Smith
00:57:30.301 [Thread-2] DEBUG i.g.x.metalock.MetaLockAspect - ML1000001U After
00:57:30.301 [Thread-2] TRACE i.g.x.metalock.MetaLockAspect - ML1000001U Unlocking User§Paul§Smith
00:57:30.302 [Thread-3] TRACE i.g.x.metalock.MetaLockAspect - ML1000002U Locked User§Paul§Smith
```

With "TRACE" level it logs each operation related to AOP wrapping and locking.
With "DEBUG" - only related to AOP wrapping.

As you can see, each row contains identifier like "ML1000001U".
This is search-friendly string, it is unique for each invocation of wrapped methods.
So you always can track when each operation was started and finished, or locks were obtained and released.

You can enable this logging via your logger config. For example:

* logback.xml
```xml
<logger name="io.github.xantorohara.metalock" level="DEBUG"/>
```
or
```xml
<logger name="io.github.xantorohara.metalock" level="TRACE"/>
```

* Spring Boot application.properties
```properties
logging.level.io.github.xantorohara.metadata=DEBUG
```

## Changelog

### v0.1.2-SNAPSHOT

* MetaLock supports multiple parameters. 
E.g.: ```@MetaLock(name = "User", param = {"firstName", "lastName"})``` 

### v0.1.1

* First release