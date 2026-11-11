---
section: library_programs
order: 444
---
# Threads and lock

A thread is a line of work inside one program. Threads of one program share its memory, and only one of them runs at a time: the machine gives each a turn in turn.

Thread exists only in Sigma#. Sigma has no threads, and it has no lock statement either.

Table: Thread members

| Member | What it does |
| --- | --- |
| `Thread.Start(action)` | Starts a new thread that runs the method the action points at. |
| `Thread.Current` | The thread that is running this part of your program. |
| `Thread.Sleep(ticks)` | Stops the thread for that many ticks. |
| `Thread.Yield()` | Gives the turn to the next thread. |
| `Thread.Id` | The number of the thread. |
| `Thread.Running` | Yes while the thread runs. |
| `Thread.Join()` | Waits until the thread ends. |
| `Thread.Join(ticks)` | Waits at most that many ticks. Gives yes when the thread ended in that time. |
| `Thread.Stop()` | Stops the thread. |

A thread runs a few instructions at a time, and 64 at most before another thread of the same program has its turn. Sleep and Join count ticks, and 20 ticks make one second.

## Lock

A lock stops two threads from running the same steps on one object at once. The lock statement takes the object's lock while its body runs, and lets go when the body ends.

1. Write lock, then the object in brackets.
2. Write the statement the lock covers after the brackets.

Only an object can be locked. A thread that finds the lock held by another thread waits, and asks again when the lock is free. A thread that already holds a lock can take it again, and then it must let go as many times as it took it.

> [!DEFINE] Thread
> A line of work inside one program, sharing its memory with the program's other threads.

> [!DEFINE] Lock
> A claim on an object, so that only one thread at a time runs the steps that hold it.

## If something goes wrong

> [!PROBLEM] The program stops and says there is no body to run on the thread.
> Give Thread.Start a method with a body of steps to run.

> [!PROBLEM] The program stops and says there is no thread here.
> The thread already ended, or it was never started here. Check Thread.Running first.

> [!PROBLEM] The program stops and says that this thread is letting go of a lock it does not hold.
> Use the lock statement, which takes and lets go of the lock in turn.

> [!PROBLEM] The compiler says only an object can be locked.
> Lock an object, such as a value of a class you write, not a plain number.

> [!PROBLEM] The compiler refuses Thread or lock in Sigma.
> Use Sigma#, or write the program so that its steps run one after another.

> [!SEE] jsc:sigma_delegates_events, jsc:sigma_runtime_stops, jsc:running_programs
