---
section: sigma_version_three_section
order: 434
---
# Métodos de extensão, operadores e indexadores

Um método de extensão acrescenta uma chamada a um tipo que você não escreveu. É um método estático de uma classe estática no topo de um namespace, e o seu primeiro parâmetro é escrito com this, como em static int Doubled(this int n). Qualquer programa que traga esse namespace com using pode então escrever count.Doubled() para um int count. Um método do próprio tipo, com o mesmo nome, vence.

Os seus próprios tipos podem dar um significado aos operadores. Um operador é um método public static escrito com a palavra operator e o seu sinal: public static Money operator +(Money a, Money b). As comparações vêm em pares, então um tipo que declara == também declara !=, e < vai com >.

Um indexador deixa você ler e escrever um objeto com colchetes, como um array. Você escreve this[...] com um get e um set: public int this[int i] { get { ... } set { ... } }. Um indexador recebe um valor entre os seus colchetes.

```sgs Money.sgs
using System.*;

namespace Shop;

struct Money
{
    public long Cents;
    public static Money operator +(Money a, Money b)
    {
        Money sum = new Money();
        sum.Cents = a.Cents + b.Cents;
        return sum;
    }
}

static class Prices
{
    public static Money Cents(this long n)
    {
        Money made = new Money();
        made.Cents = n;
        return made;
    }
}

class Till
{
    static void Main()
    {
        long price = 150;
        long tip = 25;
        Money total = price.Cents() + tip.Cents();
        Console.PrintLine("" + total.Cents);
    }
}
```

Este programa imprime "175".

> [!DEFINE] Método de extensão
> Um método estático que se lê como uma chamada sobre o valor do seu primeiro parâmetro.

> [!DEFINE] Indexador
> Os métodos por trás dos colchetes num objeto de um tipo seu.

## Se algo der errado

> [!PROBLEM] O compilador diz que um tipo que declara == também declara !=.
> Escreva a parceira de toda comparação que você declara, recebendo os mesmos valores.

> [!PROBLEM] A chamada de um método de extensão não encontra nada.
> Traga o namespace da classe estática dele com using.

> [!SEE] jsc:sigma_classes, jsc:sigma_namespaces_using
