---
section: sigma_version_three_section
order: 434
---
# Extension methods, operators and indexers

An extension method adds a call to a type you did not write. It is a static method of a static class at the top of a namespace, and its first parameter is written with this, as in static int Doubled(this int n). Any program that brings in that namespace with using can then write count.Doubled() for an int count. A method the type has of its own, with the same name, wins.

Your own types can give the operators a meaning. An operator is a public static method written with the word operator and its sign: public static Money operator +(Money a, Money b). The comparisons come in pairs, so a type that declares == declares != as well, and < goes with >.

An indexer lets you read and write an object with square brackets, as an array. You write this[...] with a get and a set: public int this[int i] { get { ... } set { ... } }. An indexer takes one value in its brackets.

```sgs Money.sgs
using System.*;

namespace Shop;

struct Money
{
    public long Cents;
    public static Money operator +(Money a, Money b)
    {
        Money sum = new Money();
        sum.Cents = a.Cents + b.Cents;
        return sum;
    }
}

static class Prices
{
    public static Money Cents(this long n)
    {
        Money made = new Money();
        made.Cents = n;
        return made;
    }
}

class Till
{
    static void Main()
    {
        long price = 150;
        long tip = 25;
        Money total = price.Cents() + tip.Cents();
        Console.PrintLine("" + total.Cents);
    }
}
```

This program prints "175".

> [!DEFINE] Extension method
> A static method that reads as a call on the value of its first parameter.

> [!DEFINE] Indexer
> The methods behind square brackets on an object of your own type.

## If something goes wrong

> [!PROBLEM] The compiler says a type that declares == declares != as well.
> Write the partner of every comparison you declare, taking the same values.

> [!PROBLEM] The call of an extension method finds nothing.
> Bring in the namespace of its static class with using.

> [!SEE] jsc:sigma_classes, jsc:sigma_namespaces_using
