---
section: sigma_objects
order: 420
---
# Properties, events, delegates and lambdas

Sigma has none of the features on this page. Sigma# has all of them. Sigma refuses lambdas as well, so this page is for Sigma#.

A property is a named value of an object that runs code when it is read or written. You write it with get and set accessors, as in int Size { get; set; }. A property with only get can be read but not written. Sigma refuses properties: write a field, or a method that gives the value back.

A delegate is a named shape of method. A value of a delegate type can hold any method of that shape, and you call the value as if it were a method. The line delegate int Pick(int a, int b); declares a shape that takes two ints and gives back an int. The System namespace has three ready-made delegates: Action takes nothing and gives nothing back, Action<T> takes one value and gives nothing back, and Func<T, R> takes one value and gives one back.

```sgs Pick.sgs
using System.*;
using System.IO.*;

namespace Shop;

delegate int Pick(int a, int b);

class Chooser
{
    static void Main()
    {
        Pick biggest = (int a, int b) => a > b ? a : b;
        Console.PrintLine("" + biggest(3, 9));
    }
}
```

A class declares an event with the word event and a delegate type, as in event Action Changed;. Other code subscribes with += and unsubscribes with -=. Only the class that declares the event can raise it, by calling it.

A lambda is a short method written in place. x => x \* 2 takes one value, and () => { statements } takes none, with a block of statements for its body. The shape comes from the delegate that the lambda is given to, so its parameters and its result must match that delegate. A lambda can use the local variables of the method it sits in, and it can use this.

```sgs Count.sgs
using System.*;
using System.IO.*;

namespace Shop;

class Till
{
    static void Main()
    {
        int kept = 0;
        Action count = () => {
            kept++;
            Console.PrintLine("" + kept);
        };
        count();
    }
}
```

> [!DEFINE] Event
> A way for an object to announce that something happened. Other code subscribes to it with += and unsubscribes with -=.

> [!DEFINE] Lambda
> A short method written in place, which takes its shape from the delegate it is given to.

## If something goes wrong

> [!PROBLEM] The compiler says an event's type must be a delegate.
> Give the event a delegate type, such as Action.

> [!PROBLEM] The compiler says an event can only be raised inside the type that declares it.
> Call a method of the class that declares the event, and let that method raise it.

> [!PROBLEM] The compiler says this lambda does not have the shape 'Pick' takes.
> Give the lambda the same parameters and the same result as the delegate.

> [!PROBLEM] The compiler says a lambda that keeps a variable a loop declares is not built yet.
> Declare the variable before the loop starts, so the lambda keeps a variable of its own.

> [!PROBLEM] The compiler says 'Ring' is a method: call it, or hand it over where a delegate of its shape is wanted.
> Write Ring() with its brackets, or hand the method to a delegate of its shape.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_namespaces_using
