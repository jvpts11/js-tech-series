---
section: sigma_basics
order: 413
---
# Types and variables

A variable is a named place that holds a value. Its type says what kind of value may go in it, and the compiler refuses anything else. You declare a variable by writing its type, then its name, and then, if you want, a first value.

Each declaration names exactly one variable. Write int total = 0; to declare one; a line such as int a, b; is refused.

Table: Built-in types

| Type | Holds | Example |
| --- | --- | --- |
| `int` | A whole number, in 32 bits. | `int count = 42;` |
| `long` | A whole number, in 64 bits, for bigger numbers. | `long big = 42L;` |
| `float` | A decimal number, shorter than a double. | `float f = 1.5f;` |
| `double` | A decimal number, with more precision. | `double half = 0.5;` |
| `bool` | true or false. | `bool ok = true;` |
| `char` | One character. Inside the machine, its number. | `char letter = 'A';` |
| `string` | Text, which is a reference type. | `string text = "hi";` |
| `object` | A value of any type. | `object anything = 5;` |

Types fall into two groups, and the group decides what happens when you copy a value. A value type, such as int, bool or char, is copied whenever you store it or hand it over. A reference type, such as text or an object of a class, is shared: two names can point to the same object, so a change through one name shows through the other.

A text is a reference type, so it can hold null, the empty value.

Numbers go into bigger number types without asking. An int goes into a long, a float or a double. The other way needs a cast: you write the new type in brackets in front of the value, as in (int) 2.5. The cast tells the compiler that you accept the change. Without the cast, the line is refused.

```sg Converting.sg
int total = 0;
long big = total;          // widening: fine
int back = (int) big;      // narrowing needs the cast
double avg = total / 2.0;  // int divided by double gives a double
```

The word null is the empty value of a reference type. It fits any type that holds objects, such as text or an object of a class, but it does not fit an int.

Sigma# has the word var, which reads the type from the value: var x = 5; makes x an int. Sigma refuses var, and you write the type out instead.

> [!DEFINE] Class
> A named blueprint for objects: it lists the fields and methods that each object has.

> [!DEFINE] Object
> A value made from a class with new. It holds its own fields and runs its own methods.

> [!DEFINE] Reference type
> A type whose values are objects that can be shared, so two names can point to one object.

> [!DEFINE] Value type
> A type whose values are copied whenever they are stored or handed over.

> [!DEFINE] Null
> The empty value of a reference type, which points to no object.

## If something goes wrong

> [!PROBLEM] The compiler says cannot convert 'double' to 'int'.
> Use a cast if losing the fraction is what you want, or declare the variable as a double. The error is explained in [the meaning errors](jsc:sigma_semantic_errors).

> [!PROBLEM] The compiler says 'total' does not name anything here.
> Check the spelling, and check that you declared the variable above the line that uses it.

> [!PROBLEM] The compiler refuses var.
> Sigma has no var: write the type out, as in int x = 5;

> [!SEE] jsc:sigma_operators, jsc:sigma_classes
