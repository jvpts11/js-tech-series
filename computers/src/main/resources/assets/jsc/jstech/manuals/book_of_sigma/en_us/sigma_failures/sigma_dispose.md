---
section: sigma_failures
order: 426
---
# Freeing objects with dispose

A program makes objects with new, and each object takes some of the memory the program may use. This language has no garbage collector. Here, an object stays until your program disposes of it, or until the program ends.

> [!DEFINE] Garbage collector
> A part of a machine that finds objects nothing uses any more, and frees them. This language has none.

The dispose statement says that you have finished with an object. You write the word, then the name of the object, as in dispose scratch;. The name must hold an object: dispose of an enum value is refused.

```sg Work.sg
namespace Work;

class Scratch
{
    public int Size;
    public Scratch(int size) { Size = size; }
}

class Work
{
    static void Main()
    {
        Scratch scratch = new Scratch(4096);
        dispose scratch;
    }
}
```

After dispose, the object cannot be used again. A later use stops the program with the message "this was disposed and cannot be used." Memory is counted to the byte: each new takes bytes from the memory your program may use, and a program that asks for more stops with an out of memory message.

## If something goes wrong

> [!PROBLEM] The compiler says only an object can be disposed, not 'Level'.
> Dispose only objects made with new. A value of an enum is not an object.

> [!PROBLEM] The program stops and says this was disposed and cannot be used.
> Make a new object with new when you need one again.

> [!PROBLEM] The program stops with an out of memory message.
> Dispose of the objects you no longer need, so that the program holds less.

> [!SEE] jsc:sigma_no_exceptions, jsc:sigma_runtime_stops
