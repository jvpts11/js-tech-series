---
section: sigma_version_three_section
order: 435
---
# Exceptions: try, catch, finally and throw

From version 3, Sigma# has exceptions. throw stops the step it is in and throws an exception, an object of the type Exception or of a class standing on it. The program then looks for a catch that takes it, first in the method that threw and then in each method that called it. Sigma has no exceptions.

A try protects a block. Each catch after it names a type of exception and takes the ones of that type, or of a type standing on it; the first that fits runs. A finally runs whichever way the block is left: at its end, by a return, a break or a jump, or by an exception. A throw written alone in a catch throws on the exception that catch took.

Table: The exceptions the language has

| Type | Thrown when |
| --- | --- |
| `NullReferenceException` | A name that holds nothing is used as if it held an object. |
| `ObjectDisposedException` | An object is used after its dispose. |
| `DivideByZeroException` | A whole number is divided by zero. |
| `IndexOutOfRangeException` | An index is outside its array. |
| `ArgumentException, ArgumentOutOfRangeException` | A call is handed a value it cannot take. |
| `InvalidOperationException` | A step the program cannot take now, such as a cast to a type the object is not. |
| `FormatException` | Text is not the number it was read as. |
| `IOException, FileNotFoundException` | A file cannot be read or written, or is not there. |
| `NetworkException` | The computer is not on a network. |
| `AccessViolationException` | A read or a write through a pointer lands outside every block of raw memory. |

Every exception has a Message, the File and the Line it was thrown at, a Trace of the calls it went through, and the Inner exception it was made with. A halt of the language, such as a division by zero, is thrown as its exception in a program that uses exceptions; running out of memory and calling too deep are thrown nothing.

An exception that nothing catches stops your program, which writes its message and its trace to the console. When a program is stopped with sigma stop, the Task Manager or kill, its finally blocks run first; kill -9 and a machine that loses power run nothing. A throw costs 50 instructions, and 5 more for each call it leaves.

```sgs Parse.sgs
using System.*;

namespace Tools;

class Parse
{
    static void Main()
    {
        try
        {
            int n = Convert.ToInt("seven");
            Console.PrintLine("" + n);
        }
        catch (FormatException failed)
        {
            Console.PrintLine("not a number: " + failed.Message);
        }
        finally
        {
            Console.PrintLine("done");
        }
    }
}
```

> [!DEFINE] Exception
> An object thrown when a step fails, which a catch somewhere along the calls can take and handle.

> [!DEFINE] Finally
> A block that runs whichever way the block it follows is left.

## If something goes wrong

> [!PROBLEM] The compiler says a catch before this one already takes every exception of that type.
> Put the catch of the narrower type first, and the one of Exception last.

> [!PROBLEM] The compiler says nothing leaves a finally but its own end.
> Take the return, break or jump out of the finally, and do it after the try.

> [!PROBLEM] The program stops and says an exception was thrown and nothing caught it.
> Catch it where the program can go on, or check the value before the step.

> [!SEE] jsc:sigma_no_exceptions, jsc:sigma_runtime_stops
