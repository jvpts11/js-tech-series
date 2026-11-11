---
section: examples_language
order: 510
---
# Steps

A step changes a variable by one, or by an amount you give. An increment adds one, a decrement takes one, and a compound assignment does an operation on the variable and stores the result.

You write ++ or -- before or after a name. Before the name, you get the new value. After the name, you get the old value. The example shows both for each kind of place you can change.

## The program

```sgs Steps.sgs
using System.*;
using System.Collections.*;
namespace Corpus;
class Counter {
    public int Value;
    public long Big;
    public float Small;
    public double Wide;
    public static int Shared;
}
class StepsCorpus {
    static int Total;

    static void Main() {
        int local = 0;
        local++;
        local--;
        ++local;
        --local;
        int before = local++;
        int after = ++local;
        int down = local--;
        int up = --local;
        Console.PrintLine("" + before + after + down + up);

        long big = 0;
        float small = 0;
        double wide = 0;
        big++;
        small++;
        wide--;
        Console.PrintLine("" + big + small + wide);

        Total++;
        --Total;
        int shared = Total++;
        Counter.Shared++;
        int other = ++Counter.Shared;
        Console.PrintLine("" + shared + other);

        Counter counter = new Counter();
        counter.Value++;
        --counter.Value;
        int held = counter.Value++;
        int heldUp = ++counter.Value;
        counter.Big++;
        counter.Small--;
        counter.Wide++;
        Console.PrintLine("" + held + heldUp + counter.Value + counter.Big);

        int[] numbers = new int[4];
        numbers[0]++;
        --numbers[1];
        int inside = numbers[2]++;
        int insideUp = ++numbers[3];
        Console.PrintLine("" + inside + insideUp + numbers[0]);

        List<int> list = new List<int>();
        list.Add(1);
        list[0]++;
        int fromList = list[0]--;
        Console.PrintLine("" + fromList + list[0]);

        Map<string, int> map = new Map<string, int>();
        map.Put("a", 1);
        map["a"]++;
        int fromMap = ++map["a"];
        Console.PrintLine("" + fromMap);

        int kept = 0;
        Action count = () => {
            kept++;
            int was = kept++;
            int now = ++kept;
            kept--;
            Console.PrintLine("" + was + now);
        };
        count();
        Console.PrintLine("" + kept);

        int combined = 1;
        combined += 2;
        combined -= 1;
        combined *= 3;
        combined /= 2;
        combined %= 4;
        combined &= 6;
        combined |= 1;
        combined ^= 3;
        combined <<= 2;
        combined >>= 1;
        Console.PrintLine("" + combined);

        string text = "a";
        text += "b";
        text += combined;
        Console.PrintLine(text);

        counter.Value += 5;
        Counter.Shared += 2;
        numbers[0] += 3;
        list[0] += 4;
        int answered = counter.Value += 1;
        int alsoAnswered = numbers[0] += 1;
        int plain = local = 7;
        Console.PrintLine("" + answered + alsoAnswered + plain);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `local++; local--; ++local; --local;` | Change a local variable by one, up and down, with the operator before and after the name. |
| `int before = local++;` | Stores the old value in before, and then adds one to local. |
| `int after = ++local;` | Adds one to local first, and then stores the new value in after. |
| `big++; small++; wide--;` | Change a long, a float and a double by one. |
| `Total++; Counter.Shared++;` | Change a static field of the program and a static field of a class. |
| `counter.Value++;` | Changes a field of an object. |
| `numbers[0]++;` | Changes an element of an array. |
| `list[0]++;` | Changes an element of a list. |
| `map["a"]++;` | Changes the value stored under the key a in a map. |
| `kept++; kept--;` | Change a variable that the lambda keeps from the lines around it. |
| `combined += 2; ... combined >>= 1;` | Each compound assignment does its operation on combined and stores the result. |
| `text += combined;` | Adds the number to the end of the text. |
| `int answered = counter.Value += 1;` | An assignment gives back the value it stored, so answered gets the new value. |
| `int plain = local = 7;` | Sets local to 7, and stores 7 in plain as well. |

> [!DEFINE] Increment
> Adding one to a value, written ++.

> [!DEFINE] Decrement
> Taking one from a value, written --.

> [!DEFINE] Prefix
> An operator written before a name, such as ++count, which gives the new value.

> [!DEFINE] Postfix
> An operator written after a name, such as count++, which gives the old value.

> [!DEFINE] Compound assignment
> An assignment that does an operation on the variable first, such as total += 2, which means total = total + 2.

> [!DEFINE] Captured variable
> A variable around a lambda that the lambda keeps and can change.

## If something goes wrong

> [!PROBLEM] A value of one type is stored in a variable of another, and the compiler says "cannot convert" with code S3003.
> Give the variable a value of its own type, such as a whole number for an int.

> [!PROBLEM] A condition is not a bool, such as a number in an if, and the compiler refuses it with code S3009.
> Write a comparison that gives true or false, such as count > 0.

> [!PROBLEM] A line holds only a value, such as 5;, and the compiler refuses it with code S2006.
> Put an assignment, a call or an increment on the line, or store the value in a variable.

> [!PROBLEM] An assignment goes to something that is not a variable, a field, a property or an element, with code S2008.
> Assign only to a variable, a field, a property or an element.

> [!PROBLEM] A name such as List is not found, and the compiler asks for a using line, with code S3040.
> Add using System.Collections.\*; at the top of the file, as the example does.

> [!SEE] jsc:sigma_types_variables, jsc:sigma_operators
