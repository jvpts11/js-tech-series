---
section: sigma_functions
order: 416
---
# Methods, out and ref

A method is a named piece of code that you call by name, often with values to work on. Its header says what the method gives back, its name and its parameters. Its body holds the statements that run. A method that gives nothing back has void as its result type.

Table: The forms of a method

| Form | Meaning |
| --- | --- |
| `static int Twice(int x)` | A static method that gives back an int, from one parameter. |
| `void Show(string text)` | A method that gives back nothing. |
| `bool Split(int total, out int half)` | A method with an out parameter, which the method fills in. |

A static method belongs to the class itself. You call it through the name of the class, as Maths.Twice(4). A method without static belongs to one object, and you call it on that object, as till.Show("hi").

```sg Maths.sg
class Maths
{
    static int Twice(int x) { return x * 2; }

    static bool Split(int total, out int half)
    {
        if (total % 2 != 0)
        {
            half = 0;
            return false;
        }
        half = total / 2;
        return true;
    }
}
```

The values you pass to a method are its arguments. The method receives them in its parameters, which are the names in its header. A value type argument is copied, so the method cannot change the caller's variable. An object is passed as the same object, so the method can change what the caller sees.

Several methods can share one name when their parameters differ. These are overloads. The compiler picks the version whose parameters fit the arguments. A call that fits no version is refused, and so is a call that fits two versions equally well.

An out parameter lets a method give a second value back. The header writes out in front of the parameter, and the method must fill it in before every way out. The caller writes out in front of an existing variable of exactly the parameter's type.

```sg Caller.sg
int half;
bool even = Maths.Split(10, out half);
printf("%d", half);
```

The keyword ref is written only for strcat, which joins text onto the text that a variable holds. A method that needs to fill in a value uses out instead. A method may also call itself: each call takes room on the stack of calls, and a chain more than 1,024 calls deep stops the program, as the [runtime stops](jsc:sigma_runtime_stops) page explains.

> [!DEFINE] Static
> Written on a method or a field, it means the member belongs to the class, not to one object.

> [!DEFINE] Parameter
> A name in the header of a method, which receives the value passed to it.

> [!DEFINE] Argument
> A value you pass to a method when you call it.

> [!DEFINE] Overload
> One of several methods with the same name, told apart by their parameters.

## If something goes wrong

> [!PROBLEM] The compiler says no version of 'Twice' takes those arguments.
> Check the number of values in the call against the parameters in the header.

> [!PROBLEM] The compiler says the call to 'Twice' fits more than one version of it.
> Give the arguments exact types, so that only one overload fits.

> [!PROBLEM] The compiler says 'half' is filled in by the method, so the argument is written with out.
> Write out in front of the variable in the call.

> [!PROBLEM] The compiler says 'half' is filled in by this method, so it must be given a value before every way out.
> Set the out parameter before each return, as Split does before it returns.

> [!PROBLEM] The compiler says an out argument is exactly 'int', with no conversion on the way.
> Declare the variable with exactly the type of the parameter.

> [!PROBLEM] The compiler says 'ref' is written only for strcat.
> Write out instead, so the method fills in a value that the caller then reads.

> [!SEE] jsc:sigma_classes, jsc:sigma_delegates_events
