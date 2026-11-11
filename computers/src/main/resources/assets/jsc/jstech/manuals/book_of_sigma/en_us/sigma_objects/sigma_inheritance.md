---
section: sigma_objects
order: 418
---
# Inheritance: virtual, override and abstract

Inheritance lets one class build on another. A class that inherits from a base class has all of the base class's members, and it can add new ones or replace some of them. You write the base class after a colon, as in class Car : Vehicle. A class has one base class at most.

A method that a derived class may replace must be marked virtual in the base class. The derived class replaces it with override, and keeps the same parameters. Without override, a method with the same name and parameters as one above it is refused, so a method never gets hidden by accident.

```sg Garage.sg
namespace Garage;

class Vehicle
{
    public virtual string Name() { return "vehicle"; }
}

class Car : Vehicle
{
    public override string Name() { return "car"; }
}
```

Table: Words of inheritance

| Word | Sigma | Sigma# |
| --- | --- | --- |
| `virtual` | Yes | Yes |
| `override` | Yes | Yes |
| `abstract` | No | Yes |

An abstract method has no body. It says that each class deriving from this one must write one. A method marked abstract needs an abstract class, and an abstract class cannot be made with new. Sigma has virtual and override, but no abstract classes and no abstract methods. Its compiler refuses them and suggests a class with virtual methods that do nothing.

To call the version of a method that you replaced, write base.Name(args). It calls the version in the base class. A struct cannot have a base class, and a static method cannot be virtual, override or abstract.

> [!DEFINE] Inheritance
> A way for a class to take on the members of another class, and then add to them or replace some of them.

> [!DEFINE] Virtual
> Marks a method that a derived class may replace.

> [!DEFINE] Override
> Marks a method that replaces a virtual method of the base class.

> [!DEFINE] Abstract
> Marks a method with no body, or a class that cannot be made with new, which needs a derived class to finish it.

## If something goes wrong

> [!PROBLEM] The compiler says only a class or an interface can stand behind the colon.
> Name a class as the base class. Interfaces after the colon are Sigma# only.

> [!PROBLEM] The compiler says 'Name' is already on 'Vehicle', so write it with override to replace it.
> Add override in front of the method.

> [!PROBLEM] The compiler says 'Name' on 'Vehicle' is not virtual, so nothing can replace it.
> Add virtual to the method in the base class.

> [!PROBLEM] The compiler says 'Name' is written with override but nothing above it has it.
> Remove override, or check that the name and the parameters match the method above.

> [!PROBLEM] The compiler says 'Draw' has no body, so 'Shape' has to be declared abstract.
> In Sigma#, declare the class abstract, or give the method a body.

> [!PROBLEM] The compiler says 'Shape' is abstract, so it cannot be made with new.
> Make an object of a class that derives from it, and give that class the bodies it needs.

> [!SEE] jsc:sigma_entry_points, jsc:sigma_classes
