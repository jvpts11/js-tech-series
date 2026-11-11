---
section: sigma_version_three_section
order: 429
---
# const, rótulos e goto

const declara um valor que o compilador conhece, que nunca muda. Você escreve const antes do tipo de uma variável local ou de um campo, e lhe dá o valor de uma vez: const int Size = 16;. O valor é feito de literais, outras consts, valores de enum e operadores. Um campo const é estático, e todo lugar que o lê é escrito com o próprio valor.

Um rótulo dá nome a uma instrução. Você escreve um nome e dois-pontos na frente dela, como em done:. Então goto done; salta para essa instrução. No Sigma#, goto case 3; e goto default; saltam para um braço do switch em volta deles. Um salto pode sair de blocos e laços, mas nunca entra num bloco, e nada salta para fora de um finally.

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

Este programa imprime 2 left, 1 left e 0 left. Um goto custa o que custa o salto de um laço, e uma const não custa nada: a listagem guarda o valor dela.

> [!DEFINE] Rótulo
> Um nome e dois-pontos na frente de uma instrução, para onde um goto salta.

## Se algo der errado

> [!PROBLEM] O compilador diz que o valor da const é um conhecido ao compilar.
> Dê à const um valor feito de literais, outras consts e operadores, sem chamada nem variável.

> [!PROBLEM] O compilador diz que um salto pode sair de um bloco, nunca entrar num.
> Tire o rótulo do bloco, para o bloco em que o goto está ou para um em volta dele.

> [!SEE] jsc:sigma_intercal_words, jsc:sigma_control_flow
