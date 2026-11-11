---
section: sigma_failures
order: 425
---
# Without exceptions: Try forms and halts

Many languages report a failed step with an exception, which another part of the program can catch and handle. Sigma has no exceptions, and neither has Sigma# before version 3: they refuse try, catch, throw and finally. This page is about writing without them. When you read a number from text, use a Try form first, so the program does not stop. [Exceptions](jsc:sigma_exceptions) shows the ones Sigma# 3 has.

> [!DEFINE] Exception
> A signal that a step failed, which a program can catch and handle somewhere else.

Instead, a program can ask first with a Try form, or it can let the problem stop it. A Try form is a library call that says whether it worked, and gives its result back through out. Sigma's own Convert and File offer fewer calls, and the Try forms are not among them. The [numbers page](jsc:numbers_and_time) and the [files page](jsc:files_and_file_handles) show what Sigma offers.

> [!DEFINE] Try form
> A library call that gives back true or false for success, and writes its result through out.

Table: The Try forms

| Call | Gives back |
| --- | --- |
| `Convert.TryInt(text, out int)` | A bool: whether the text is a whole number, and the number through out |
| `Convert.TryLong(text, out long)` | A bool: whether the text is a long, and the value through out |
| `Convert.TryDouble(text, out double)` | A bool: whether the text is a decimal number, and the value through out |
| `Convert.TryBool(text, out bool)` | A bool: whether the text is true or false, and the value through out |
| `File.TryRead(path, out string)` | A bool: whether the file could be read, and its text through out |
| `Map.TryGet(key, out value)` | A bool: whether the key is there, and its value through out |

A halt is different. The program stops at once, writes a message to its console, and does not go on. Without exceptions nothing catches a halt; in a Sigma# 3 program that uses them, most halts are thrown as an exception it may catch. The full list of messages is in the [runtime stops page](jsc:sigma_runtime_stops).

Table: Halts that the language itself causes

| The message begins with | Caused by |
| --- | --- |
| divided by zero | A division with a divisor of zero. |
| took the remainder of a division by zero | A remainder with a divisor of zero. |
| there is nothing here to reach into | A name that holds nothing is used as if it held an object. |
| there is no place 4 in an array of 4 | An index that is outside the array. |
| this was disposed and cannot be used | An object used again after its dispose. |
| out of memory | The program asks for more memory than it may use. |
| attempted to read or write protected memory | A pointer is read or written outside every block of raw memory. In Sigma the system says it in its own words, such as Segmentation fault. |

A fault is different again. The machine itself failed on one instruction, and only that one program stops. The message says that the runtime could not carry the step out.

> [!DEFINE] Fault
> A failure of the machine itself on one instruction, which stops only that program.

```sgs Reader.sgs
using System.*;
using System.Utils.*;
using System.IO.*;

namespace Parse;

class Reader
{
    static void Main()
    {
        string text = "42";
        int n;
        if (Convert.TryInt(text, out n))
        {
            Console.PrintLine("" + n);
        }
        else
        {
            Console.PrintLine("not a number");
        }
    }
}
```

## If something goes wrong

> [!PROBLEM] The program stops and says there is nothing here to reach into.
> Check that the name holds an object before you use it, for example by creating the object with new first.

> [!PROBLEM] The program stops and says this was disposed and cannot be used.
> Do not use an object after you dispose of it. See [Freeing objects with dispose](jsc:sigma_dispose).

> [!PROBLEM] The program stops and says divided by zero.
> Check that the divisor is not zero before you divide.

> [!PROBLEM] The compiler refuses try.
> Sigma, and Sigma# before version 3, have no try: check the value before the step, use a Try form, or hold the Sigma# project to version 3.

> [!SEE] jsc:sigma_runtime_stops, jsc:sigma_dispose, jsc:sigma_exceptions
