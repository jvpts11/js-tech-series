---
section: sigma_objects
order: 419
---
# Interfaces e records

Esta página descreve o Sigma#. O Sigma não tem interfaces nem records. O compilador dele recusa a palavra interface e sugere uma classe com métodos virtual no lugar, e recusa a palavra record e sugere um struct com os mesmos campos.

Uma interface lista os métodos que uma classe deve escrever. Ela guarda apenas assinaturas de métodos: um tipo de resultado, um nome e parâmetros, terminando em ponto e vírgula, sem corpo. Uma classe declara que escreve uma interface com dois-pontos, e então escreve todos os métodos dela como métodos public. Uma interface também pode ficar atrás de outra interface, depois de dois-pontos.

```sgs Shapes.sgs
namespace Shapes;

interface IShape
{
    int Area();
}

class Square : IShape
{
    public int Side;
    public int Area() { return Side * Side; }
}
```

IScript é uma interface embutida que faz um programa continuar ativo. A [página de pontos de entrada](jsc:sigma_entry_points) explica isso.

Um record é um tipo parecido com uma classe, formado por campos readonly. Você escreve os seus campos entre colchetes depois do nome. record Point(int X, int Y); declara um Point com dois campos, X e Y. Cada campo é public e readonly, e o record ganha um construtor que recebe os campos na ordem. O record também escreve ToString para você, que imprime os campos pelo nome. Dois records com os mesmos campos são iguais, porque um record é comparado pelos seus valores.

Table: Records e interfaces, em resumo

| Tipo | O que guarda | Copiado ao ser armazenado |
| --- | --- | --- |
| Interface | Apenas assinaturas de métodos, sem campos. | Não se aplica: não guarda valores. |
| Record | Campos readonly, definidos pelo construtor. | Não: é compartilhado, como todas as classes. |

## Se algo der errado

> [!PROBLEM] O compilador diz que 'Square' afirma ser um 'IShape', mas não tem 'Area'.
> Escreva todo método que a interface lista, como um método public.

> [!PROBLEM] O Sigma recusa a palavra interface e sugere uma classe com métodos virtuais.
> No Sigma, escreva uma classe cujos métodos sejam virtual e derive dela, como mostra a página do Sigma sobre herança.

> [!PROBLEM] O Sigma recusa a palavra record e sugere uma struct com os mesmos campos.
> No Sigma, escreva uma struct com os mesmos campos, ou use o Sigma# para um record.

> [!SEE] jsc:sigma_inheritance, jsc:sigma_delegates_events
