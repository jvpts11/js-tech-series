---
section: sigma_version_three_section
order: 432
---
# sealed, classes estáticas, params, funções locais e nameof

A versão 3 traz ao Sigma# cinco formas pequenas do C#. Cada uma poupa um pouco de escrita, e nenhuma muda como o programa roda.

Table: As formas pequenas

| Escrito como | O que significa |
| --- | --- |
| `sealed class Tool` | Nenhuma classe se apoia em Tool. Num override, sealed diz que nenhuma classe mais abaixo substitui o método de novo. |
| `static class Tools` | Uma classe só de membros estáticos, que nunca é criada com new. |
| `int Sum(params int[] values)` | O último parâmetro recebe os seus valores um a um, como em Sum(1, 2, 3), ou um array como ele é. |
| `int Twice(int n) => n * 2;` | Uma função local: um método escrito dentro de um método, que lê as variáveis locais do método como um lambda lê. |
| `nameof(count)` | O texto do último nome escrito, aqui count, conhecido ao compilar. Renomear count e esquecer o texto é apanhado. |

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

Este programa imprime sum = 12.

> [!DEFINE] Função local
> Um método escrito dentro de outro método, que só esse método chama.

## Se algo der errado

> [!PROBLEM] O compilador diz que params vai no último parâmetro.
> Mova o parâmetro params para o fim da lista.

> [!PROBLEM] O compilador diz que um membro de uma classe estática também tem de ser estático.
> Escreva static no membro, ou tire o static da classe.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_classes
