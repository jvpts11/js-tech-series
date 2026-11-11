---
section: sigma_version_three_section
order: 429
---
# const, labels and goto

const declares a value the compiler knows, which never changes. You write const before the type of a local or a field, and give it its value at once: const int Size = 16;. The value is made of literals, other consts, enum values and operators. A const field is static, and every place that reads it is written with the value itself.

A label names a statement. You write a name and a colon in front of it, as in done:. Then goto done; jumps to that statement. In Sigma#, goto case 3; and goto default; jump to an arm of the switch around them. A jump may leave blocks and loops, but it never goes into a block, and nothing jumps out of a finally.

```sg Retry.sg
namespace Tools;

class Retry
{
    const int Tries = 3;

    static void Main()
    {
        int left = Tries;
    again:
        left--;
        printf("%d left\n", left);
        if (left > 0)
        {
            goto again;
        }
    }
}
```

This program prints 2 left, 1 left and 0 left. A goto costs what a jump of a loop costs, and a const costs nothing at all: the listing holds its value.

> [!DEFINE] Label
> A name and a colon in front of a statement, which a goto jumps to.

## If something goes wrong

> [!PROBLEM] The compiler says the const's value is one known when compiling.
> Give the const a value made of literals, other consts and operators, with no call and no variable in it.

> [!PROBLEM] The compiler says a jump may leave a block, never enter one.
> Move the label out of the block, to the block the goto stands in or one around it.

> [!SEE] jsc:sigma_intercal_words, jsc:sigma_control_flow
