# Set changes tracking

```xml

<dependency>
    <groupId>org.open-structures</groupId>
    <artifactId>changes-tracking</artifactId>
    <version>1.0.0</version>
</dependency>
```

Imagine you have a set with slow add/remove operations (e.g. DB or an external API).
When working with that set you might want to do things differently.
For example, you might want to bulk your changes before "flushing" it, or try to avoid making slow calls to check if an
element is in the set.

`ChangesTracking` helps to determine added, removed and updated elements. Updated elements are the ones that were first deleted and re-added later.
See [ChangesTrackingTest](src/main/java/org/open_structures/changes_tracking/ChangesTracking.java) for examples. 

`ChangesTracking` is [Restorable](https://github.com/denissudak/memento):

    changesTracking.deleted(1);
    changesTracking.added(2);
    ChangesTracking.State<Integer> changesTrackingState = changesTracking.getState();
    changesTracking.added(3);
    changesTracking.restore(changesTrackingState);

At the end `changesTracking' would only have tracking data for 1 and 2.