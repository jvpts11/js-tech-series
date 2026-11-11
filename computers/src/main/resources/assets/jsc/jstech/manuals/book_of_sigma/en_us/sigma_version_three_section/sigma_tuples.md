---
section: sigma_version_three_section
order: 433
---
# Tuples

A tuple holds a few values together without a type of its own. You write its type as the types of its places in brackets, (int count, string name), and a tuple itself as its values in brackets, (3, "iron"). A tuple has from two to eight places.

You read a place by its name, or by Item1, Item2 and so on. A tuple is copied as a struct is: storing one or handing it over hands over a copy, and nobody disposes of one.

A tuple can be taken apart into variables. var (n, s) = Find(); declares two new variables, and (a, b) = (b, a); swaps two that already exist: every value is read before any place is written.

```sgs Stock.sgs
using System.*;

namespace Tools;

class Stock
{
    static (int count, string name) Most()
    {
        return (64, "iron");
    }

    static void Main()
    {
        var (count, name) = Most();
        Console.PrintLine(name + ": " + count);
        (int, int) pair = (1, 2);
        (pair.Item1, pair.Item2) = (pair.Item2, pair.Item1);
        Console.PrintLine("" + pair.Item1);
    }
}
```

This program prints "iron: 64", and then "2".

> [!DEFINE] Tuple
> A few values held together, each in a place of its own, with no type written for them.

## If something goes wrong

> [!PROBLEM] The compiler says a tuple has from two to eight places.
> Keep between two and eight values, or write a struct for more.

> [!PROBLEM] The compiler says a value cannot be taken apart into that many places.
> Write as many places as the tuple has.

> [!SEE] jsc:sigma_classes
