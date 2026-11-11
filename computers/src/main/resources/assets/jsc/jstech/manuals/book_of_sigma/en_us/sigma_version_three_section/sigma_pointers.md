---
section: sigma_version_three_section
order: 436
---
# Pointers and raw memory

A pointer holds the address of a value in raw memory: bytes the program asked for, with no object around them. int\* is a pointer to an int. A pointer points at an int, a long, a byte, a struct that holds only values, or at nothing in particular with void\*. The type byte, a whole number from 0 to 255, comes with pointers.

In Sigma#, pointers are held only in unsafe code: a method written unsafe, or a block written unsafe { ... }. A Sigma file is unsafe throughout, as C was.

Table: What a pointer does

| Written as | What it means |
| --- | --- |
| `&x` | The address of x: a local, a parameter, or a place a pointer reaches. From then on, x lives in raw memory until its method returns. |
| `*p` | The value p points at, to read or to write. |
| `p->Count` | A field of the struct p points at. |
| `p[i]` | The value i places after the one p points at. |
| `p + n, p - n, p++` | The pointer moved by n values of what it points at. |
| `p - q` | How many values lie between two pointers, as a long. |
| `p == q, p < q` | Compares two addresses. null points at nothing. |
| `(byte*) p` | The same address, read as another pointer. |
| `stackalloc int[n]` | Room for n values, freed when the method returns. |
| `sizeof(Cell)` | How many bytes a value of the type takes. |

Table: The calls of raw memory

| Sigma# | Sigma | What it does |
| --- | --- | --- |
| `Memory.Alloc(bytes)` | `malloc(bytes)` | A new block of that many bytes. |
| `Memory.AllocZeroed(count, size)` | `calloc(count, size)` | A new block for count values of that size. |
| `Memory.Resize(p, bytes)` | `realloc(p, bytes)` | A block of the new size, holding what fits of the old one, which is freed. |
| `Memory.Free(p)` | `free(p)` | Gives the block back. Freeing null does nothing. |
| `Memory.Copy(dest, src, bytes)` | `memcpy(dest, src, bytes)` | Copies bytes from src to dest. |
| `Memory.Fill(p, value, bytes)` | `memset(p, value, bytes)` | Sets every byte to the value. |
| `Memory.Compare(a, b, bytes)` | `memcmp(a, b, bytes)` | Below zero, zero or above zero, by the first byte that differs. |

Every block of raw memory starts as zeros and knows its size. A read or a write outside every live block, past the end of one, in one already freed, or through null, is a violation. In Sigma# it throws an AccessViolationException. In Sigma the program dies with its system's own words, such as Segmentation fault (core dumped) on Linux and the BSDs.

The bytes count exactly in the program's memory. A read or a write through a pointer costs one instruction, Alloc and Free cost what new and dispose cost, and Copy, Fill and Compare cost one more for every 64 bytes.

```sg Cells.sg
namespace Tools;

struct Cell
{
    public int Value;
    public Cell* Next;
}

class Cells
{
    static void Main()
    {
        Cell* first = null;
        for (int i = 1; i <= 3; i++)
        {
            Cell* made = calloc(1, sizeof(Cell));
            made->Value = i;
            made->Next = first;
            first = made;
        }
        int total = 0;
        for (Cell* at = first; at != null; at = at->Next)
        {
            total += at->Value;
        }
        printf("%d\n", total);
    }
}
```

This program prints "6". A struct with a pointer field is written in Sigma, which is unsafe throughout; in Sigma#, a pointer lives in the locals and parameters of unsafe code.

> [!DEFINE] Pointer
> A value that holds the address of another value in raw memory.

> [!DEFINE] Raw memory
> Bytes a program asked for, with no object around them, read and written through pointers.

## If something goes wrong

> [!PROBLEM] The compiler says a pointer is held only in unsafe code.
> Write unsafe on the method, or put the lines in an unsafe block.

> [!PROBLEM] The program stops with Segmentation fault, or throws an AccessViolationException.
> A pointer read past the end of its block, or a block already freed. Check the index and the size you asked for, and free each block once, after its last use.

> [!SEE] jsc:sigma_exceptions, jsc:sigma_dispose
