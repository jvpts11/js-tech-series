---
section: sigma_version_three_section
order: 430
---
# comefrom, abstainfrom, reinstate, ignore e remember

Estas cinco palavras vêm do INTERCAL, uma linguagem feita para ser estranha. Você pode usá-las no Sigma e no Sigma#, e cada uma custa uma instrução.

Table: As cinco palavras

| Escrito como | O que faz |
| --- | --- |
| `comefrom label;` | Quando a instrução que o rótulo marca termina, o programa continua depois do comefrom. Um rótulo aceita um comefrom. |
| `abstainfrom label;` | Desliga o código da instrução que o rótulo marca até o fim do seu bloco: a partir daí ele é pulado. Desligar duas vezes é o mesmo que uma. |
| `reinstate label;` | Liga esse código de novo. |
| `ignore x;` | Toda escrita posterior em x desaparece, embora ainda custe as suas instruções. x é uma variável local, um campo ou um campo estático. |
| `remember x;` | As escritas em x voltam a contar. |

O que está desligado e o que está ignorado pertence ao programa em execução, e é salvo com ele quando o mundo é guardado. Os dois começam limpos cada vez que o programa começa.

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

Este programa imprime "1": a escrita de 99 desapareceu, e o bloco do rótulo loud até o seu fim foi desligado antes de ser alcançado.

## Se algo der errado

> [!PROBLEM] O compilador diz que o rótulo já tem um comefrom.
> Fique com um comefrom para cada rótulo.

> [!PROBLEM] O compilador diz que só uma variável local, um campo ou um campo estático podem ser ignorados.
> Ignore uma variável ou um campo, não um elemento de um array nem um valor.

> [!SEE] jsc:sigma_constants_goto
