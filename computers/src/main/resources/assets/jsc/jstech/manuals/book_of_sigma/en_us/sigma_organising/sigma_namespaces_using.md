---
section: sigma_organising
order: 421
---
# Namespaces and using

Every type of a program belongs to a namespace, which is a name that groups types. A file declares its namespace with one line before its types, such as namespace Shop;. Every type in the file then belongs to that namespace, so two programs can each have a type called Till without a clash. When you write a program, the first lines tell the compiler where the types it uses come from.

The library is grouped into namespaces as well. A program that wants a library type writes a using line at the top of the file, before the namespace line. using System.IO.\*; brings in everything in System.IO, and using System.IO.Console; brings in one type only.

Table: Namespaces you will meet

| Namespace | Holds |
| --- | --- |
| `Standard` | The library of Sigma, which Sigma# also has: Console, File, Math, Script and more. |
| `System` | Action, Func and IScript. |
| `System.IO` | Console and File. |
| `System.Utils` | Math and Random. |
| `System.Collections` | List and Map. |
| `System.Machine` | Other parts of the library. The [library chapter](jsc:sigma_library) describes them. |

A using line for a whole namespace is written with a star, such as using System.IO.\*;. A line that names one type has no star. A program written in Sigma writes using Standard.\*; to reach the library of Sigma.

A type that the program declares itself wins over a library type with the same name. A full name, such as System.IO.Console.PrintLine("hi"), works in Sigma# without a using line, and Sigma refuses full names.

Table: The start of a new project

| Project | Header lines |
| --- | --- |
| Sigma# console program | `using System.IO.*; then namespace, then the class` |
| Sigma# script | `using System.*; and using System.IO.*; then namespace, then the class` |
| Sigma console program | `using Standard.*; then namespace` |
| Sigma script | `using Standard.*; then namespace` |

```sgs Ping.sgs
using System.IO.*;

namespace Garage.Tools;

class Ping
{
    static void Main() { Console.PrintLine("up"); }
}
```

## If something goes wrong

> [!PROBLEM] The compiler says 'Console' is in System.IO; add 'using System.IO.\*;' or 'using System.IO.Console;' at the top of the file.
> Add that using line at the top of the file.

> [!PROBLEM] The compiler says 'System.IO' names a namespace.
> Write 'using System.IO.\*;' to bring in everything in it, or name one of its types.

> [!PROBLEM] The compiler says a using has to come before the namespace and the types.
> Move the using line above the namespace line.

> [!PROBLEM] The compiler refuses using System in Sigma.
> Write using Standard.\*; instead, which holds everything that Sigma has.

> [!SEE] jsc:sigma_entry_points, jsc:sigma_library
