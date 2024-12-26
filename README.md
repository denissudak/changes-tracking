# Set changes tracking

```xml

<dependency>
    <groupId>org.open-structures</groupId>
    <artifactId>changes-tracking</artifactId>
    <version>1.0.0</version>
</dependency>
```

Imagine you have a set with slow add/remove operations (e.g. external API or even DB).
When working with that set you might want to bulk your changes before "flushing" it, or try to avoid making slow calls to check if an
element is in the set.

`ChangesTracking` tracks changes of the set, meaning elements that were added, removed and updated:

    Job j1 = new Job("1", "Monday job");
    Job j2 = new Job("2", "Tuesday job");
    Job j3 = new Job("3", "Wednesday job");

    ChangesTracking<Job> changesTracking = new ChangesTracking<>();
    changesTracking.added(j1);
    changesTracking.deleted(j2);
    changesTracking.deleted(j3);
    j3.setName("Thursday job");
    changesTracking.added(j3);

    changesTracking.getAdded(); // j1
    changesTracking.getDeleted(); // j2
    changesTracking.getUpdated(); // j3

See [ChangesTrackingTest](src/test/java/org/open_structures/changes_tracking/ChangesTrackingTest.java) for more examples.