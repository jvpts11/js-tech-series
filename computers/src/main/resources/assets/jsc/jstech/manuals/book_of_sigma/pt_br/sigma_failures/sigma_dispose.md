---
section: sigma_failures
order: 426
---
# Liberando objetos com dispose

Um programa cria objetos com new, e cada objeto ocupa parte da memória que o programa pode usar. Esta linguagem não tem coletor de lixo. Aqui, um objeto permanece até que o seu programa o descarte, ou até que o programa termine.

> [!DEFINE] Coletor de lixo
> Uma parte da máquina que encontra objetos que ninguém usa mais e os libera. Esta linguagem não tem nenhuma.

A instrução dispose diz que você terminou com um objeto. Você escreve a palavra, depois o nome do objeto, como em dispose scratch;. O nome precisa guardar um objeto: descartar um valor de enum é recusado.

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

Depois de dispose, o objeto não pode mais ser usado. Um uso posterior para o programa com a mensagem "isto foi descartado e não pode ser usado." A memória é contada até o byte: cada new retira bytes da memória que o programa pode usar, e um programa que pede mais do que isso para com uma mensagem de falta de memória.

## Se algo der errado

> [!PROBLEM] O compilador diz que só um objeto pode ser descartado, e não 'Level'.
> Descarte somente objetos criados com new. Um valor de enum não é um objeto.

> [!PROBLEM] O programa para e diz que isto foi descartado e não pode ser usado.
> Crie um novo objeto com new quando precisar dele de novo.

> [!PROBLEM] O programa para com uma mensagem de falta de memória.
> Descarte os objetos de que você não precisa mais, para que o programa guarde menos.

> [!SEE] jsc:sigma_no_exceptions, jsc:sigma_runtime_stops
