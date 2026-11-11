---
section: sigma_data
order: 424
---
# Lists and maps

An array holds a fixed number of values of one type, numbered from 0. You make one with new and its length: new int[4] makes an array of four ints, with the indexes 0 to 3. Length tells how many places the array has. An index outside the array stops the program with a message, as the [runtime stops page](jsc:sigma_runtime_stops) explains. Arrays are in Sigma and in Sigma#.

A List is a growable row of values of one type, written List<int> for a list of ints. You make one with new List<int>(), and each Add puts one more item at the end. Sigma# only.

Table: Members of List

| Member | Gives back | What it does |
| --- | --- | --- |
| `Count` | An int | How many items the list holds. |
| `Add(item)` | Nothing | Puts the item at the end. |
| `Insert(index, item)` | Nothing | Puts the item in the place index, moving the later items along. |
| `RemoveAt(index)` | Nothing | Takes away the item at the place index. |
| `Remove(item)` | A bool | Takes away an item that matches, and tells whether it found one. |
| `Clear()` | Nothing | Takes away every item. |
| `Contains(item)` | A bool | Whether the item is in the list. |
| `IndexOf(item)` | An int | The place of the item, or -1 when it is not there. |
| `Get(index), Set(index, item)` | An item, or nothing | Reads or changes the item at the place index. |
| `Sort()` | Nothing | Puts the items in order. |

A Map holds pairs: a key, and the value that belongs to it. Map<string, int> makes a map from text keys to int values. Put adds a pair, and the key finds the value again. A map has no Clear member. Sigma# only.

> [!DEFINE] Key
> The part of a pair that finds its value in a map, such as the name of a fruit.

> [!DEFINE] Map
> A collection of pairs: each key leads to one value. Written Map<K, V> in Sigma#.

Table: Members of Map

| Member | Gives back | What it does |
| --- | --- | --- |
| `Count` | An int | How many pairs the map holds. |
| `Put(key, value)` | Nothing | Adds the pair, or changes the value of a key that is already there. |
| `Get(key)` | The value | The value of the key. A missing key gives back null and does not stop the program. |
| `ContainsKey(key)` | A bool | Whether the key is in the map. |
| `TryGet(key, out value)` | A bool | Whether the key is there, and its value through out. |
| `Remove(key)` | A bool | Takes the pair away, and tells whether the key was there. |
| `Keys(), Values()` | A List | Every key, or every value, in a List. |

To find out whether a key is there before you read it, use ContainsKey, or use TryGet, which tells you and gives the value back through out in one call. A map is indexed with the key in brackets, as in count["apple"].

```sgs Stock.sgs
using System.*;
using System.Collections.*;
using System.IO.*;

namespace Inventory;

class Stock
{
    static void Main()
    {
        Map<string, int> count = new Map<string, int>();
        count.Put("apple", 3);
        int got;
        if (count.TryGet("apple", out got))
        {
            Console.PrintLine("" + got);
        }
        List<string> names = count.Keys();
        foreach (string name in names)
        {
            Console.PrintLine(name);
        }
    }
}
```

A foreach statement walks a List or an array, one item after another. A map cannot be walked with foreach: walk its Keys() or its Values() instead, which are Lists.

Sigma refuses the List and Map types and any type written with angle brackets. In Sigma, use an array with a count of your own beside it, or use two arrays for pairs.

## If something goes wrong

> [!PROBLEM] The program stops and says "there is no place 3 in a list of 3".
> The index must run from 0 up to one less than Count. Check it before you read the item.

> [!PROBLEM] The program stops and says "there is no place 4 in an array of 4".
> Use the indexes from 0 up to the length minus one.

> [!PROBLEM] The compiler says foreach walks an array or a list, not a Map.
> Walk Keys() or Values() instead, as in foreach (string name in count.Keys()).

> [!PROBLEM] The compiler refuses List in Sigma, and says to use an array, and a count of your own beside it.
> Use an array, with a count of your own beside it, as Sigma allows.

> [!SEE] jsc:sigma_control_flow, jsc:sigma_no_exceptions
