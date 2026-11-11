---
section: examples_language
order: 509
---
# Values

A struct is a value. When you store one, or hand it to a method, you get a copy of it. This example shows that with a struct called Vec, and it puts the struct into a field, a static field, an array and a list.

A class is a reference type: two names can point at one object, and both see every change made to it. The class Held in this example holds a struct in a field, and its static field is shared by the whole program.

## The program

```sgs Values.sgs
using System.*;
using System.Collections.*;
namespace Corpus;
struct Vec {
    public int X;
    public int Y;
}
class Held {
    public Vec Spot;
    public static Vec Origin;
}
class ValuesCorpus {
    static Vec Kept;

    static void Main() {
        Vec one = new Vec();
        one.X = 1;
        one.Y = 2;

        Vec copy = one;
        copy.X = 9;
        Console.PrintLine("" + one.X + copy.X);

        Kept = one;
        Held held = new Held();
        held.Spot = one;
        Held.Origin = one;
        Console.PrintLine("" + Kept.X + held.Spot.Y + Held.Origin.X);

        Vec given = Same(one);
        Console.PrintLine("" + given.X + given.Y);

        Vec[] spots = new Vec[3];
        spots[0] = one;
        spots[1] = copy;
        Console.PrintLine("" + spots[0].X + spots[1].X);

        List<Vec> many = new List<Vec>();
        many.Add(one);
        many.Add(copy);
        Console.PrintLine("" + many.Count);

        int total = 0;
        foreach (Vec spot in spots) {
            total = total + spot.X;
        }
        foreach (Vec spot in many) {
            total = total + spot.Y;
        }
        Console.PrintLine("" + total);

        int[] numbers = new int[4];
        numbers[0] = 5;
        numbers[3] = 7;
        int sum = 0;
        foreach (int number in numbers) {
            sum = sum + number;
        }
        Console.PrintLine("" + sum);

        string[] words = new string[2];
        words[0] = "a";
        words[1] = "b";
        string joined = "";
        foreach (string word in words) {
            joined = joined + word;
        }
        Console.PrintLine(joined);

        List<string> names = new List<string>();
        names.Add("x");
        foreach (string name in names) {
            Console.PrintLine(name);
        }
    }

    static Vec Same(Vec spot) {
        Vec back = spot;
        return back;
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `struct Vec` | Declares Vec, a struct with two fields, X and Y, both of type int. |
| `class Held` | Declares a class with a struct field called Spot and a static field called Origin. |
| `Vec copy = one; copy.X = 9;` | Stores a copy of one in copy. Changing the copy leaves one as it was. |
| `Kept = one; held.Spot = one; Held.Origin = one;` | Each store makes its own copy, so the three places hold separate values. |
| `Same(one)` | Hands one to a method, which gives back a copy of it. |
| `spots[0] = one;` | Stores a copy in an element of the array. |
| `many.Add(one);` | Adds a copy to the list. |
| `foreach (...)` | Loops over the array and the list, and reads each struct in turn. |

> [!DEFINE] Value type
> A type whose values are copied when they are stored or handed over, such as a struct.

> [!DEFINE] Reference type
> A type whose values are one shared object: every name for it sees the same object, as with a class.

> [!DEFINE] Static
> Belongs to the type, not to one object: every part of the program sees the same value.

> [!DEFINE] Array
> A row of values of one type with a fixed length, made with new and the length, such as new Vec[3].

A field you did not give a value starts as an empty struct, so a struct field is never nothing. An array of structs works the same way: new Vec[3] makes three empty ones.

## If something goes wrong

> [!PROBLEM] A struct declares a base class, and the compiler refuses it with code S3041.
> A struct can implement interfaces, but it cannot have a base class. Remove the base class from the struct.

> [!SEE] jsc:sigma_classes, jsc:sigma_library
