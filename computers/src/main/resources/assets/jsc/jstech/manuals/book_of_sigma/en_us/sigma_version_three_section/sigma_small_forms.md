---
section: sigma_version_three_section
order: 432
---
# sealed, static classes, params, local functions and nameof

Version 3 brings five small forms of C# to Sigma#. Each saves you some writing, and none of them changes how the program runs.

Table: The small forms

| Written as | What it means |
| --- | --- |
| `sealed class Tool` | No class stands on Tool. On an override, sealed says that no class further down replaces the method again. |
| `static class Tools` | A class of static members only, which is never made with new. |
| `int Sum(params int[] values)` | The last parameter takes its values one by one, as in Sum(1, 2, 3), or an array as it is. |
| `int Twice(int n) => n * 2;` | A local function: a method written inside a method, which reads the method's locals as a lambda does. |
| `nameof(count)` | The text of the last name written, here count, known when compiling. Renaming count and forgetting the text is caught. |

```sgs Small.sgs
using System.*;

namespace Tools;

static class Small
{
    static int Sum(params int[] values)
    {
        int total = 0;
        for (int i = 0; i < values.Length; i++)
        {
            total += values[i];
        }
        return total;
    }

    static void Main()
    {
        int Twice(int n) => n * 2;
        int sum = Sum(1, 2, 3);
        Console.PrintLine(nameof(sum) + " = " + Twice(sum));
    }
}
```

This program prints sum = 12.

> [!DEFINE] Local function
> A method written inside another method, which only that method calls.

## If something goes wrong

> [!PROBLEM] The compiler says params goes on the last parameter.
> Move the params parameter to the end of the list.

> [!PROBLEM] The compiler says a member of a static class has to be static as well.
> Write static on the member, or take static off the class.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_classes
