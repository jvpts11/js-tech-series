---
section: library_toolkit
order: 465
---
# Queue, Stack, Set, and Find and Sort with a function

Sigma# 3 adds three collections to List and Map, in System.Collections, and two calls on a list that take a function you write with =>.

Table: The new collections

| Member | What it does |
| --- | --- |
| `Queue<T>.Enqueue(value), Dequeue(), Peek()` | A queue gives back first what it was given first. Dequeue takes it out; Peek only looks. |
| `Stack<T>.Push(value), Pop(), Peek()` | A stack gives back first what it was given last. |
| `Set<T>.Add(value), Remove(value), Items()` | A set holds each value once. Add gives no when it already held the value; Items lists them in the order they came. |
| `Count, Contains(value), Clear()` | On all three, as on a list. |

Table: A list with a function

| Member | What it does |
| --- | --- |
| `List<T>.Find(test)` | The first element the test says yes to, or the type's empty value: 0, false or null. |
| `List<T>.Sort(compare)` | Sorts the list by a comparison of two elements, which gives below 0 when the first comes first. Elements it finds equal keep their order. |

Taking from an empty queue or stack stops the program. Find and Sort call your function once for each element they look at, and each call costs what its instructions cost.

```sgs Sorting.sgs
List<int> counts = new List<int>();
counts.Add(64); counts.Add(8); counts.Add(32);
counts.Sort((a, b) => a - b);
int big = counts.Find(n => n > 16);
```

> [!DEFINE] Queue
> A collection that gives back first what came in first, as a line of people does.

> [!DEFINE] Stack
> A collection that gives back first what came in last, as a pile of plates does.

## If something goes wrong

> [!PROBLEM] The program stops and says the queue is empty.
> Check Count before Dequeue or Peek.

> [!SEE] jsc:sigma_list_map
