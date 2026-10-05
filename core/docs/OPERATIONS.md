# Operations

*Added 2026-04-29.*

## Operations: asking the network to do something

Once players have a network, they stop moving items and running machines by hand: they ask the network, and it
does the work as an **Operation**. An Operation is a request with a kind (search the storage, craft this, move
that), the arguments that kind needs, and a status that goes from `PENDING` to `COMPLETED`, or to one of the
other outcomes when it cannot finish.

Who does the work depends on what is on the network ([Networks](NETWORKS.md)):

- the **Mainframe**, one per network, receives every Operation and decides where it runs;
- the **computers** (category C, see `NetworkCategory`) run them;
- everything else can be read by the network but never runs anything.

This part of the Core is in the API: what it promises is on [docs/API.md](../../docs/API.md).

## Adding a kind of Operation

*Added 2026-09-17.*

Your mod can add a kind of Operation. Declare it once, while the game loads, by listening for `CoreRegisterEvent`
on your mod's event bus:

```java
public record CountItemsArgs(String item) implements IOperationArgs {
}

@SubscribeEvent
static void register(CoreRegisterEvent event) {
    event.operations().register(new OperationType<>(
            "myaddon:count_items",         // saved in worlds: never change it
            CountItemsArgs.class,           // what the request carries
            OperationCategory.STORAGE,      // the family it belongs to
            IndustrialTier.T1,              // the lowest tier that can run it
            EnumSet.of(NetworkCategory.C),  // who may run it: the computers
            new CountItemsHandler()));      // the code that does the work
}
```

After the game has loaded, the list closes and `register` returns `null` instead of adding. That is on purpose: a
world saved yesterday has to mean the same thing today. An id another mod already registered is refused with an
exception, which stops the load while somebody is there to read why.

The handler is handed each request's arguments and answers the state the request is in: a finished one for work
done on the spot, or `PENDING` for work that goes on over the next ticks.

```java
public final class CountItemsHandler implements IOperationHandler<CountItemsArgs> {

    @Override
    public OperationStatus execute(final CountItemsArgs args) {
        // look the item up, report the count somewhere
        return OperationStatus.COMPLETED;
    }
}
```

Reach the kinds of every mod with `JsCore.operations()`: `get("jsc:select")`, `contains(id)`, `all()`.

## The eight states

| State | Means |
| --- | --- |
| `PENDING` | Queued, not yet running. |
| `PROCESSING` | Running. |
| `WAITING` | Waiting on something locked or busy; gives up after a while (1200 ticks unless the server says otherwise). |
| `COMPLETED` | Delivered everything. |
| `COMPLETED_PARTIAL` | Finished with less than was asked. |
| `FAILED` | Could not be done; the reason is a translation key with its values, so each player reads it in their language. |
| `RESOURCE_LOCKED` | Refused, because what it needed is held. |
| `DISCARDED` | Never ran, or was cancelled. |

The first three are active, the other five final. Each has a number of its own (1 to 8) because states are saved and
sent; zero means "nothing known".

## Priority and queues

An Operation has a **priority**: `LOW`, `MEDIUM_LOW`, `MEDIUM` (the default), `MEDIUM_HIGH`, `HIGH`. The Mainframe
works several **queues** at once (one, plus one for each of its graphics cards); when there are more Operations
ready than queues free, the highest priority goes first, and equal ones keep the order they came in. An Operation
that waits climbs a level every 600 ticks (the server can change it, or turn it off), so nothing waits forever.

## Watching Operations

*Added 2026-06-04.*

Every Operation's life is posted on the Core's event bus: created when accepted, started on its first tick, then
exactly one of completed, failed or discarded.

```java
final Consumer<IOperationLifecycleEvent> listener = event -> {
    if (event instanceof IOperationLifecycleEvent.Completed done) {
        log(done.typeId() + " took " + done.durationTicks() + " ticks");
    }
};
JsCore.events().subscribe(IOperationLifecycleEvent.class, listener);
```

Each event carries the network, the Operation's id and its kind's id. Listeners run on the server thread, in the
order they subscribed; unsubscribe with the same listener when you are done.

## What can go wrong

- **"Operation type already registered".** Another mod, or your own code twice, registered that id.
- **`register` gives back null.** It was called after loading. Register from `CoreRegisterEvent` only.
- **"requiredCategories must contain at least one".** Say who may run it; the computers are `NetworkCategory.C`.
- **The id is refused.** It must be `namespace:path` in lower case letters, digits, `_` and `/`.
