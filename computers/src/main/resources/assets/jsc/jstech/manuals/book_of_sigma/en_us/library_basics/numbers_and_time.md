---
section: library_basics
order: 441
---
# Math, Convert, Random and Time

Programs compute with numbers, and the library gives you the common sums and conversions in Math and Convert. Random gives you numbers you cannot predict, and Time tells the program what the world's clock says. Calls to these cost nothing.

## Math

Table: Math members

| Member | What it does |
| --- | --- |
| `Math.Abs(number)` | The size of a number, without its sign. |
| `Math.Min(a, b)` | The smaller of two numbers. |
| `Math.Max(a, b)` | The larger of two numbers. |
| `Math.Floor(number)` | The whole number at or below a decimal number. |
| `Math.Ceil(number)` | The whole number at or above a decimal number. Sigma# only. |
| `Math.Round(number)` | The nearest whole number. Sigma# only. |
| `Math.Sqrt(number)` | The square root of a decimal number. |
| `Math.Pow(number, power)` | A number raised to a power. |
| `Math.Clamp(value, low, high)` | The value, kept between low and high. Sigma# only. |

Sigma offers Abs, Min, Max, Floor, Sqrt and Pow. The other members here are Sigma# only.

## Convert

Table: Convert members

| Member | What it does |
| --- | --- |
| `Convert.ToInt(text)` | The whole number in the text. Stops the program when the text is not a number. |
| `Convert.ToInt(text, fallback)` | The whole number in the text, or the fallback when the text is not a number. Version 2. |
| `Convert.ToLong(text)` | A long whole number. Sigma# only. |
| `Convert.ToFloat(text)` | A short decimal number. Sigma# only. |
| `Convert.ToDouble(text)` | The decimal number in the text. |
| `Convert.ToBool(text)` | True or false, from the text. |
| `Convert.ToString(value)` | The text of a value. |
| `Convert.ToString(number, base)` | The number as text in a base from 2 to 36. Version 2. |
| `Convert.TryInt(text, out value)` | True and the number when the text is a number, false otherwise. Sigma# only. |
| `Convert.TryLong, Convert.TryDouble, Convert.TryBool` | The same as TryInt, for the other kinds. Sigma# only. |

A base is how many different digits a number is written with. Base 10 is the ordinary way, and base 16 is hexadecimal. Base 10 keeps the minus sign. Every other base writes the 32 bits of the number, so -1 in base 16 is ffffffff.

Convert.ToBool takes true, yes, y, on or 1 for true, and false, no, n, off or 0 for false, in any case.

## Random

Table: Random members

| Member | What it does |
| --- | --- |
| `Random.Next(bound)` | A whole number from 0 up to the bound, not including the bound. |
| `Random.NextDouble()` | A decimal number from 0 up to 1, not including 1. Sigma# only. |
| `Random.Seed(number)` | Starts the sequence again from that number. |

Random is in Sigma# from version 1, and in Sigma from version 2. Each program has its own sequence, and seeding it with the same number gives the same sequence again.

## Time

Table: Time members

| Member | What it does |
| --- | --- |
| `Time.Tick` | The world's tick count, as a long whole number. |
| `Time.DayTime` | The time of day in ticks, from 0 up to 23,999. Sigma# only. |
| `Time.Day` | The day number of the world's clock. |
| `Time.Ticks(seconds)` | The number of ticks in that many seconds. Sigma# only. |

A tick is one step of the world's clock, and the game runs 20 ticks each second. Time.Ticks is the one place the library turns seconds into ticks. The credits a program gets each tick are explained in [](jsc:credits_per_tick).

> [!DEFINE] Bound
> The number a whole-number draw stays below. Random.Next(6) gives 0 to 5.

## If something goes wrong

> [!PROBLEM] The program stops and says that the text is not a number.
> Give a fallback as the second value of Convert.ToInt, or check the text first with Convert.TryInt.

> [!PROBLEM] The program stops and says there is no base in that number.
> Use a base from 2 to 36, such as 10 or 16.

> [!PROBLEM] Random.Next always gives 0.
> Give a bound of 1 or more. A bound of 6 gives numbers from 0 to 5.

> [!SEE] jsc:sigma_types_variables, jsc:sigma_runtime_stops
