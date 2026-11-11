---
section: sigma_version_three_section
order: 433
---
# Tuplas

Uma tupla guarda alguns valores juntos sem um tipo próprio. Você escreve o tipo dela como os tipos dos seus lugares entre parênteses, (int count, string name), e a própria tupla como os seus valores entre parênteses, (3, "iron"). Uma tupla tem de dois a oito lugares.

Você lê um lugar pelo nome dele, ou por Item1, Item2 e assim por diante. Uma tupla é copiada como um struct é: guardar uma ou entregá-la entrega uma cópia, e ninguém a descarta.

Uma tupla pode ser desmontada em variáveis. var (n, s) = Find(); declara duas variáveis novas, e (a, b) = (b, a); troca duas que já existem: todo valor é lido antes de qualquer lugar ser escrito.

```sgs Stock.sgs
using System.*;

namespace Tools;

class Stock
{
    static (int count, string name) Most()
    {
        return (64, "iron");
    }

    static void Main()
    {
        var (count, name) = Most();
        Console.PrintLine(name + ": " + count);
        (int, int) pair = (1, 2);
        (pair.Item1, pair.Item2) = (pair.Item2, pair.Item1);
        Console.PrintLine("" + pair.Item1);
    }
}
```

Este programa imprime "iron: 64", e depois "2".

> [!DEFINE] Tupla
> Alguns valores guardados juntos, cada um num lugar próprio, sem um tipo escrito para eles.

## Se algo der errado

> [!PROBLEM] O compilador diz que uma tupla tem de dois a oito lugares.
> Mantenha entre dois e oito valores, ou escreva um struct para mais.

> [!PROBLEM] O compilador diz que um valor não pode ser desmontado em tantos lugares.
> Escreva tantos lugares quantos a tupla tem.

> [!SEE] jsc:sigma_classes
