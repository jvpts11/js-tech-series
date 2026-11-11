---
section: sigma_basics
order: 414
---
# Operators and their order

An operator is a symbol that combines values: + adds two numbers, and == asks whether two values are equal. A piece of code that gives back a value, such as 2 + 3 or a name, is an expression. You join expressions with operators to make bigger expressions.

When one line has several operators, precedence decides which one is worked out first. Multiplication binds tighter than addition, so 2 + 3 \* 4 is 14, not 20. Parentheses override the order: (2 + 3) \* 4 is 20.

Table: Operator precedence

| Group | Operators, loosest group first |
| --- | --- |
| Assignment | `= += -= *= /= %= &= \|= ^= <<= >>= (right to left)` |
| Conditional | `?:` |
| Or | `\|\|` |
| And | `&&` |
| Bitwise or | `\|` |
| Bitwise exclusive or | `^` |
| Bitwise and | `&` |
| Equality | `== !=` |
| Comparison and type tests | `< <= > >= is as` |
| Shifts | `<< >>` |
| Addition | `+ -` |
| Multiplication | `* / %` |
| Unary, before a value | `! - + ~ ++ -- and casts` |
| Member, call, index and after a value | `. () [] ++ --` |

The arithmetic operators are + for adding, - for subtracting, \* for multiplying, / for dividing and % for the remainder. When both sides of / are whole numbers, the result is a whole number: the fraction is cut off, so 7 / 2 is 3, and 7 % 2 is 1. Dividing by zero, or taking a remainder with zero, stops the program with a message.

The + operator joins text. When one side is text, the result is text, so "y = " + y puts the number after the words. The comparison operators < <= > >= work on numbers only. To put two texts in order, use string.Compare, as the text page explains.

Table: What == compares

| Kind of value | What it compares |
| --- | --- |
| Numbers | Their values. |
| bool | Whether both are true, or both are false. |
| Text | The characters, one after another. |
| Struct and record | Each field, one after another. |
| Class object | Whether both names point to the same object, not what it holds. |

Table: Logical and bitwise operators

| Operators | Work on |
| --- | --- |
| `&& \|\| !` | bool values only. |
| `& \| ^` | bool values, or whole numbers. |
| `<< >>` | Whole numbers only. |
| `~` | Whole numbers only. |
| `++ --` | Numbers only, before or after the name. |

The ++ operator adds one, and -- takes one away. A line can hold only the operator, such as count++ on its own. Inside a bigger expression, the form after the name gives the old value, and the form before the name gives the new one.

The compound forms join an operator to an assignment. total += 5; does the same as total = total + 5;, and the same goes for the other compound forms.

A shift moves the bits of the number on its left by the count on its right, and gives a number of the kind on its left. It takes only the low bits of the count, as C# does: five for an int and six for a long. So 1 << 32 is 1, and 1 << 33 is 2.

```sg Operators.sg
int x = 3;
int y = x * 2 + 1;        // 7
bool ok = y > 5 && y != 9;
int shifted = 1 << 4;     // 16
string label = "y = " + y;
```

> [!DEFINE] Expression
> A piece of code that gives back a value, such as 2 + 3 or a name.

> [!DEFINE] Operator
> A symbol that combines values, such as + or ==.

> [!DEFINE] Precedence
> The rule that decides which operator is worked out first when a line has several.

## If something goes wrong

> [!PROBLEM] The compiler says '<' cannot be applied to 'string' and 'string'.
> Compare texts with string.Compare, which gives a number to put in order.

> [!PROBLEM] The compiler says "a condition is a bool, not 'int'".
> An if, a while or a for needs a true or false test. Write if (count > 0) instead of if (count).

> [!PROBLEM] The compiler says "only a call, an assignment, an increment, a decrement or a new object can be used as a statement".
> Store the value in a variable, or pass it to a method, so the line does something.

> [!PROBLEM] The compiler says "the left side of an assignment must be a variable, a field, a property or an element".
> Put a name on the left of the equals sign, such as total = 5;

> [!SEE] jsc:sigma_control_flow, jsc:sigma_strings_chars
