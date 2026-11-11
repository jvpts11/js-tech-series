---
section: sigma_basics
order: 415
---
# Statements: if, loops, switch, break and return

Some statements hold other statements inside them. They decide whether the inside runs, or how many times it runs. Most of them test a condition: an expression that is true or false.

A block can stand wherever one statement can. A variable declared inside a block exists only until the closing brace.

Table: Control statements

| Statement | Form | Language |
| --- | --- | --- |
| `if` | `if (condition) statement else statement` | Sigma and Sigma# |
| `while` | `while (condition) statement` | Sigma and Sigma# |
| `do` | `do statement while (condition);` | Sigma and Sigma# |
| `for` | `for (start; condition; step) statement` | Sigma and Sigma# |
| `foreach` | `foreach (Type name in array) statement` | Sigma# only |
| `switch` | `switch (value) with case and default labels` | Sigma and Sigma# |
| `lock` | `lock (object) statement` | Sigma# only |

The if statement runs its statement when the condition is true. The optional else runs its statement when the condition is false. A loop repeats a statement: while checks the condition before each run, and do checks it after each run. The for loop keeps a counter. For example, for (int i = 0; i < 3; i++) runs three times, with i at 0, 1 and 2.

The foreach statement runs its statement once for each item of an array or a list, in order. It gives each item to a variable of your choice. Sigma refuses foreach and asks you to write a for over the array's Length instead.

The switch statement compares a value with each case label, and runs the section whose label matches. A section does not stop by itself. Without a break or a return at its end, the program goes on into the next section. Put break at the end of each section unless you want that.

```sg Counting.sg
for (int i = 0; i < 3; i++)
{
    switch (i)
    {
        case 0:
            printf("zero");
            break;
        default:
            printf("%d", i);
            break;
    }
}
```

The break statement leaves the nearest loop or switch. The continue statement skips to the next turn of the nearest loop. The return statement ends a method. A method that gives back a value must return one on every way out, so the code cannot reach the end of the method without a return.

The lock statement, in Sigma# only, takes an object, and a number cannot be locked. How locks are used with threads is explained in [Threads and locks](jsc:threads_and_locks).

> [!DEFINE] Block
> A group of statements between a pair of braces, which counts as one statement.

> [!DEFINE] Loop
> A statement that repeats another statement, such as while, do or for.

> [!DEFINE] Condition
> An expression that must give a true or false value, such as count > 0.

> [!DEFINE] Array
> A fixed row of values of one type, numbered from 0. You write its type with brackets, such as int[].

> [!DEFINE] List
> A growable row of values of one type, from the List type of Sigma#. Its items are numbered from 0 as well.

## If something goes wrong

> [!PROBLEM] The compiler says 'break' only means something inside a loop or a switch.
> Move the break inside a loop or a switch, or remove it.

> [!PROBLEM] The compiler says 'continue' only means something inside a loop.
> Move the continue inside a loop, or remove it.

> [!PROBLEM] The compiler says a method that gives back 'int' must return a value.
> End the method with a return that gives a value, such as return total;

> [!PROBLEM] The compiler says a method that gives back nothing cannot return a value.
> Remove the value after return, or change the method so that it gives back a value of some type, such as int.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_dispose
