---
section: sigma_objects
order: 417
---
# Classes, structs and enums

A class is a blueprint, and an object is a thing made from it. You declare a class with the word class, and you make an object of it with new. A program declares its types at the top of a namespace, and each type can hold fields, constructors and methods.

Table: The kinds of type

| Kind | How its values behave | Sigma | Sigma# |
| --- | --- | --- | --- |
| class | Each value is an object, and names share it. | Yes | Yes |
| struct | Each value is copied when stored or handed over, and compared by its fields. | Yes | Yes |
| enum | A list of named numbers, such as Low and High. | Yes | Yes |
| interface | A list of methods that a class must write. | No | Yes |
| record | A class-like type made of readonly fields, compared by its values. | No | Yes |
| delegate | A named shape of method, which a value can hold. | No | Yes |

A struct is a value type. Its values are copied when you store them or hand them over, so a change to the copy does not change the original. An enum is a list of named numbers. Each name is a value of the enum type, and you write Level.Mid to use one. The first name gets the number 0, and each name after it gets the next number, unless you write its number yourself.

```sg Shop.sg
namespace Shop;

enum Level { Low, Mid = 5, High }     // Low is 0, Mid is 5, High is 6

struct Money { public int Cents; }     // copied when stored

class Till
{
    private int sales;
    public Till(int start) { sales = start; }
    public void Add(int cents) { sales += cents; }
    public int Total() { return sales; }
}
```

A constructor has the name of its class and gives nothing back. It runs when you make an object with new, and it sets the object up. Till till = new Till(100); makes a Till that starts with 100 cents. A class with no constructor gets an empty one. A constructor can hand over to another constructor of its own class with the keyword this, or to the base class with the keyword base.

You reach a member with a dot. An object's member is written with the object's name, as till.Total(). A static member is written with the type's name, as Level.Mid for an enum value. The keyword this means the current object, and base means the base class. A field marked readonly can be written only where it is declared or in a constructor. The word const is kept for a later version of the language: for now, write a static readonly field.

The words public, private, protected and internal say who may reach a member, as in C#. A public or an internal member is reached from anywhere in the program. A private one is reached only from inside its own type and the types nested in it, and a member written with no word of access is private. A protected one is reached from its own type and from the types that stand on it. Reaching a member from where its word does not let you is the error S3060.

Sigma# has interfaces, records and delegates as well, which are explained in [Interfaces and records](jsc:sigma_interfaces_records) and [Properties, events, delegates and lambdas](jsc:sigma_delegates_events).

> [!DEFINE] Struct
> A value type, declared with the word struct, which is copied whenever it is stored or handed over.

> [!DEFINE] Enum
> A type whose values are a list of names, and each name has a number.

> [!DEFINE] Field
> A variable declared inside a class or a struct, which belongs to each object.

> [!DEFINE] Constructor
> A special method that runs when you make an object with new, named after its class.

> [!DEFINE] Namespace
> A named group of types, such as Shop. A file puts its types in one namespace with a namespace line.

> [!DEFINE] Interface
> A list of method signatures that a class promises to write.

> [!DEFINE] Base class
> The class that another class inherits from, written after a colon.

> [!DEFINE] Record
> A class-like type made of readonly fields, which is compared by its values.

> [!DEFINE] Delegate
> A named shape of method. A value of a delegate type can hold any method of that shape.

## If something goes wrong

> [!PROBLEM] The compiler says 'Till' has no member called 'Count'.
> Check the spelling, and check that the class declares that member.

> [!PROBLEM] The compiler says 'Add' belongs to an object, not to the type.
> Call it on an object, such as till.Add(50).

> [!PROBLEM] You mark a field readonly, then write it in a method: the compiler says 'count' is readonly, so it can only be written where it is declared or in a constructor.
> Write it in the constructor, or leave out readonly.

> [!PROBLEM] The compiler says an enum's number has to be written as a number.
> Write the number as a plain whole number, such as Mid = 5.

> [!PROBLEM] The compiler says 'Add' is already declared here.
> Rename one of the two methods, or give the second one different parameters.

> [!SEE] jsc:sigma_inheritance, jsc:sigma_interfaces_records
