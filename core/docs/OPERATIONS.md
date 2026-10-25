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

Your mod can add a kind of Operation. Declare it once, while the game loads, by listening for
`RegisterOperationTypesEvent` (`dev.jstech.core.jstechseries.operation`) on your mod's event bus:

```java
public record CoolArgs(int kelvin) implements IOperationArgs {
    // How a request written as text is read: RUN myaddon:cool WITH kelvin = 300
    public static final MapCodec<CoolArgs> TEXT = RecordCodecBuilder.mapCodec(cool -> cool.group(
            Codec.intRange(0, 10_000).fieldOf("kelvin").forGetter(CoolArgs::kelvin)).apply(cool, CoolArgs::new));
}

public static final TextKey COOL_NAME = TextKey.of("myaddon.operation.cool", "Cool down");

// In your mod's constructor:
modBus.addListener(RegisterOperationTypesEvent.class, event -> event.operations().register(new OperationType<>(
        "myaddon:cool",                 // saved in worlds: never change it
        COOL_NAME,                      // what the log, the list of what runs and the statistics call it
        CoolArgs.class,                 // what the request carries
        OperationCategory.INDUSTRIAL,   // the family it belongs to
        IndustrialTier.T1,              // the lowest tier that can run it
        EnumSet.of(NetworkCategory.C),  // who may run it: the members given work
        CoolArgs.TEXT,                  // how a request written as text is read; leave it out for code only
        new CoolHandler())));           // the code that does the work
```

After the game has loaded, the list closes and `register` returns `null` instead of adding. That is on purpose: a
world saved yesterday has to mean the same thing today. An id another mod already registered is refused with an
exception, which stops the load while somebody is there to read why.

A kind that leaves the text reader out (the constructor without it) can only be asked for from code, such as a button
on the machine's own screen; one with it can be asked for in J's Computers' network language, by a player at a
prompt, by a job, or by a program, with its fields by name (`RUN myaddon:cool ON 'Reactor 1' WITH kelvin = 300`). A
field the reader does not have, or a value it cannot read, is refused with the field named.

Reach the kinds of every mod with `JsCore.operations()`: `get("jsc:select")`, `contains(id)`, `all()`.

## Doing the work: the request

*Added 2026-10-25.*

The handler is handed each request as an `IOperationRequest`: what it carries (`args()`), the machine of the network
it was given to (`machine()`, the member that carries the kind out), and the way that machine says how the work goes.
It answers the state the request is in once it returns: a finished one for work done on the spot, or `PROCESSING` for
work that goes on over the next ticks. A machine whose work takes time keeps the request and reports on it from its
own tick:

```java
public final class CoolHandler implements IOperationHandler<CoolArgs> {

    @Override
    public OperationStatus execute(final IOperationRequest<CoolArgs> request) {
        if (!(request.machine().orElse(null) instanceof ReactorBlockEntity reactor)) {
            return OperationStatus.FAILED;
        }
        return reactor.coolTo(request);   // keeps the request, answers PROCESSING
    }
}

// in the reactor's tick
if (cooling != null && temperature <= cooling.args().kelvin()) {
    cooling.report(OperationStatus.COMPLETED);
    cooling = null;
}
```

| The machine says | With |
| --- | --- |
| it runs, or waits on something | `report(PROCESSING)`, `report(WAITING)` |
| it is over | `report(COMPLETED)`, `report(COMPLETED_PARTIAL)`, `report(RESOURCE_LOCKED)` |
| it cannot go on | `fail(OperationFailure.of(MY_REASON, what))`, a reason each player reads in their language |

Whoever asked can call a request off at any time (J's Computers' list of what runs has a button for it, and a
Mainframe that switches off calls off everything it gave out): it becomes `DISCARDED`, the machine hears it through
`whenCancelled(stop)`, and nothing it reports after that counts. Once a request is over, what is said changes nothing.

Code that asks for one itself builds an `OperationRequest` and starts it: `new OperationRequest<>(COOL, new
CoolArgs(300), reactor).start()`, then reads `status()` and `failure()` as the ticks go. A handler that throws fails
its request, with the reason naming the kind and the exception in the log, rather than taking the server's tick down.

In J's Computers, the Mainframe gives these requests out: it queues each by its priority as it does its own, hands it
to the machine when a queue is free and lets the queue go at once, and keeps it in the log as running until the
machine says it is over. A test gives one to a member with the test kit's `NetworkScenes.request(...)`.

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
- **`register` gives back null.** It was called after loading. Register from `RegisterOperationTypesEvent` only.
- **"requiredCategories must contain at least one".** Say who may run it; the computers are `NetworkCategory.C`.
- **The id is refused.** It must be `namespace:path` in lower case letters, digits, `_` and `/`.
- **A request fails at once, "the code of ... broke on the request".** The handler threw; the exception is in the
  log, under the kind's id.
- **"... is asked for from its machine's own screen".** The kind was declared without a text reader, so the network's
  language cannot ask for it; give it one (`MapCodec` of its arguments) if players and programs should.
