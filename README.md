[![Build Status](https://travis-ci.org/iamironz/binaryprefs.svg?branch=master)](https://travis-ci.org/iamironz/binaryprefs)
[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14)
<a href="http://www.methodscount.com/?lib=com.github.iamironz%3Abinaryprefs%3A1.0.0-ALPHA-1"><img src="https://img.shields.io/badge/Methods count-627-e91e63.svg"/></a>
<a href="http://www.methodscount.com/?lib=com.github.iamironz%3Abinaryprefs%3A1.0.0-ALPHA-1"><img src="https://img.shields.io/badge/Size-78 KB-e91e63.svg"/></a>
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Binary%20Preferences-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/5931)


## Binary Preferences

Rapidly fast and lightweight re-implementation of SharedPreferences 
which stores each preference in files separately, performs disk operations 
via NIO with memory mapped byte buffers and works IPC (between processes). 
Written from scratch.

## Advantages

* Lightweight. Zero dependency
* Super fast (faster than any other key/value solutions)
* Small memory footprint while serialize/deserialize data
* Zero copy in-memory cache
* Persists only binary data. Not XML or JSON
* Out of box data encryption support
* Fully backward compatible with default `SharedPreferences` interface
* Store all primitives include `double`, `char`, `byte` and `short`
* Store complex data objects backward-compatible (see `Persistable` class
documentation)
* Fully optimized IPC support (preferences change listeners and in-memory
cache works between processes)
* Handle various exception events


## Usage

#### Minimal working configuration

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .build();
```

Please, use only one instance of preferences by name, it saves you from
non-reasoned allocations. You can store one instance of preferences
in application class or event better use one instance from IoC like
Dagger or some another DI framework.

All parameters are optional and chain-buildable.

#### Custom preferences name

Builder contains method which defines desirable preferences name:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .name("user_data")
                .build();
```

Default is "default" name.


#### Encryption

You can define your own key/value vice versa encryption or use default:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .keyEncryption(new XorKeyEncryptionImpl("16 bytes secret key".getBytes())))
                .valueEncryption(new AesValueEncryptionImpl("16 bytes secret key".getBytes(), "16 bytes initial vector".getBytes()))
                .build();
```

Default is no-op encryption for key/value.


#### Exception handler

You can listen exceptions which comes during disk IO, serialization,
task execution operations:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .exceptionHandler(new ExceptionHandler() {
                    @Override
                    public void handle(Exception e) {
                        //perform metrica report
                    }
                })
                .build();
```

Default is print handler which performs `e.printStacktrace()` if
exception event are comes.

#### Custom save directory

You can save preferences inside custom directory:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .customDirectory(Environment.getExternalStorageDirectory())
                .build();
```

Be careful: write into external directory required appropriate
runtime and manifest permissions.

#### IPC mode

If your app architecture is process based (services works in separate processes)
and you would like to get preferences updates with consistent cache state
you can enable this feature:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .supportInterProcess(true)
                .build();
```

Please, note that one key change delta should be less than 1 (one) megabyte
because IPC data transferring is limited by this capacity.
Details here: [Documentation](https://developer.android.com/reference/android/os/TransactionTooLargeException.html)

#### Dealing with `Persistable`

`Persistable` contract been added for fast and flexible saving and it's
restoring complex objects. It's pretty similar like standard java
`Externalizable` contract but without few methods which don't need for.
For usage you just need to implement this interface with methods in your
data-model.

All Persistable data-objects should be registered by key for understanding
de/serialization contract during cache initialization.


#### How to register `Persistable`

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .registerPersistable(TestUser.KEY, TestUser.class)
                .registerPersistable(TestOrder.KEY, TestOrder.class)
                .build();
```

Note about `deepClone` method: you should implement full object hierarchy copying 
for fast immutable in-memory data fetching.

Sample for explanation: [TestUser.java](https://github.com/iamironz/binaryprefs/blob/master/library/src/test/java/com/ironz/binaryprefs/impl/TestUser.java#L68-L121)

#### Migration from another implementations

Builder have simple api for existing preferences migration:

```java
Preferences preferences = new BinaryPreferencesBuilder(context)
                .migrateFrom(oldPreferences)
                .migrateFrom(oldPreferences2)
                .build();
```

You can append one or more preferences for migration and it's will be merged into
this one implementation.
After successful migration all data in migrated preferences will be removed. 
Please note that all existing values in this implementation will be rewritten 
to values which migrates into. 
Also type information will be rewritten and lost too without any exception. 
If this method will be called multiple times for two or more different instances 
of preferences which has keys collision then last preferences values will be applied.

## Logcat preferences dump

You can dump your preferences with adb console command right in logcat:

`adb shell am broadcast -a com.ironz.binaryprefs.ACTION_DUMP_PREFERENCE --es "pref_name" "your_pref_name" (optional: --es "pref_key" "your_pref_key")`

where:

`your_pref_name` - is your preferences name which is defined in `register` method.
`your_pref_key` - is your preference key, this is optional value.

How to register preferences by name:

```java
DumpReceiver.register(name, preferences);
```

Fully working example of all values dump:

`adb shell am broadcast -a com.ironz.binaryprefs.ACTION_DUMP_PREFERENCE --es "pref_name" "user_data"`


Example only for `user_id` key dump:

`adb shell am broadcast -a com.ironz.binaryprefs.ACTION_DUMP_PREFERENCE --es "pref_name" "user_data" --es "pref_key" "user_id"`


Please note that if you create multiple instances of one preferences
(e.g. in `Activity#onCreate`) you should unregister dump
(e.g. in `Activity#onDestroy`) like this:

```java
DumpReceiver.unregister(name);
```


## Roadmap

1. ~~Disk I/O encrypt.~~ completed
2. ~~IPC~~ completed
3. ~~Externalizable.~~ completed as `Persistable`
4. ~~Preferences tooling (key set reading).~~ completed:
`adb shell am broadcast -a com.ironz.binaryprefs.ACTION_DUMP_PREFERENCE --es "pref_name" "your_pref_name" (optional: --es "pref_key" "your_pref_key")`
5. ~~Custom serializers.~~ completed
6. ~~Synchronous commits.~~ completed
7. ~~Store all primitives (like byte, short, char, double).~~ completed
8. ~~Lock free (avoid locks).~~ completed as `LockFactory`.
9. ~~Exact background tasks for each serialization strategies.~~ completed
10. ~~Reduce events (implement events transaction).~~ completed.
11. ~~Simplify api (instance creating, exception handles).~~ completed
12. ~~File name encrypt~~ completed
13. ~~Finalize serialization and persistence contract~~ completed
14. ~~Default preferences migration mechanism~~ complete
15. Background initializer
16. `byte[]` support
17. IPC transactions without 1mb limit
18. RxJava support
19. `sun.misc.Unsafe` serialization mode for api 21+


## License
```
Copyright 2017 Alexander Efremenkov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```