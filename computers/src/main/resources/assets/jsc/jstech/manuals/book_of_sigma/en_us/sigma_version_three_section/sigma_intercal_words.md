---
section: sigma_version_three_section
order: 430
---
# comefrom, abstainfrom, reinstate, ignore and remember

These five words come from INTERCAL, a language written to be strange. You can use them in Sigma and in Sigma#, and each costs one instruction.

Table: The five words

| Written as | What it does |
| --- | --- |
| `comefrom label;` | When the statement the label marks ends, the program goes on after the comefrom instead. A label takes one comefrom. |
| `abstainfrom label;` | Switches off the code from the statement the label marks to the end of its block: from then on it is skipped. Switching off twice is the same as once. |
| `reinstate label;` | Switches that code on again. |
| `ignore x;` | Every later write to x vanishes, though it still costs its instructions. x is a local, a field or a static field. |
| `remember x;` | Writes to x count again. |

What is switched off and what is ignored belongs to the running program, and is saved with it when the world is put away. Both start clean each time the program starts.

```sg Odd.sg
namespace Tools;

class Odd
{
    static void Main()
    {
        int count = 1;
        ignore count;
        count = 99;
        remember count;
        abstainfrom loud;
        {
        loud:
            printf("never printed\n");
        }
        printf("%d\n", count);
    }
}
```

This program prints "1": the write of 99 vanished, and the block from the label loud to its end was switched off before it was reached.

## If something goes wrong

> [!PROBLEM] The compiler says the label already has a comefrom.
> Keep one comefrom for each label.

> [!PROBLEM] The compiler says only a local, a field or a static field can be ignored.
> Ignore a variable or a field, not an element of an array or a value.

> [!SEE] jsc:sigma_constants_goto
