---
section: sigma_objects
order: 419
---
# Interfaces and records

This page describes Sigma#. Sigma has neither interfaces nor records. Its compiler refuses the word interface and suggests a class with virtual methods instead, and it refuses the word record and suggests a struct with the same fields.

An interface lists the methods that a class must write. It holds method signatures only: a result type, a name and parameters, ending in a semicolon, with no body. A class says that it writes an interface with a colon, and then writes every method of it as a public method. An interface can stand behind another interface after a colon too.

```sgs Shapes.sgs
namespace Shapes;

interface IShape
{
    int Area();
}

class Square : IShape
{
    public int Side;
    public int Area() { return Side * Side; }
}
```

IScript is a built-in interface that makes a program stay up. The [entry points page](jsc:sigma_entry_points) explains it.

A record is a class-like type made of readonly fields. You write its fields in brackets after its name. record Point(int X, int Y); declares a Point with two fields, X and Y. Each field is public and readonly, and the record gets one constructor that takes the fields in order. The record also writes ToString for you, which prints its fields by name. Two records with the same fields are equal, because a record is compared by its values.

Table: Records and interfaces, in short

| Kind | What it holds | Copied when stored |
| --- | --- | --- |
| Interface | Method signatures only, with no fields. | Not applicable: it holds no values. |
| Record | readonly fields, set by the constructor. | No: it is shared, as every class is. |

## If something goes wrong

> [!PROBLEM] The compiler says 'Square' says it is a 'IShape' but does not have 'Area'.
> Write every method the interface lists, as a public method.

> [!PROBLEM] Sigma refuses the word interface and suggests a class with virtual methods.
> In Sigma, write a class whose methods are virtual, and derive from it, as the Sigma page on inheritance shows.

> [!PROBLEM] Sigma refuses the word record and suggests a struct with the same fields.
> In Sigma, write a struct with the same fields, or use Sigma# for a record.

> [!SEE] jsc:sigma_inheritance, jsc:sigma_delegates_events
