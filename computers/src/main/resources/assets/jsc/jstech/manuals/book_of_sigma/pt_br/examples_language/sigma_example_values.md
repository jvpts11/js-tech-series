---
section: examples_language
order: 509
---
# Valores

Um struct é um valor. Quando você o guarda, ou o entrega a um método, obtém uma cópia dele. Este exemplo mostra isso com um struct chamado Vec, e coloca o struct em um campo, em um campo estático, em um array e em uma lista.

Uma classe é um tipo por referência: dois nomes podem apontar para um mesmo objeto, e ambos veem cada mudança feita nele. A classe Held deste exemplo guarda um struct em um campo, e o seu campo estático é compartilhado por todo o programa.

## O programa

```sgs Values.sgs
using System.*;
using System.Collections.*;
namespace Corpus;
struct Vec {
    public int X;
    public int Y;
}
class Held {
    public Vec Spot;
    public static Vec Origin;
}
class ValuesCorpus {
    static Vec Kept;

    static void Main() {
        Vec one = new Vec();
        one.X = 1;
        one.Y = 2;

        Vec copy = one;
        copy.X = 9;
        Console.PrintLine("" + one.X + copy.X);

        Kept = one;
        Held held = new Held();
        held.Spot = one;
        Held.Origin = one;
        Console.PrintLine("" + Kept.X + held.Spot.Y + Held.Origin.X);

        Vec given = Same(one);
        Console.PrintLine("" + given.X + given.Y);

        Vec[] spots = new Vec[3];
        spots[0] = one;
        spots[1] = copy;
        Console.PrintLine("" + spots[0].X + spots[1].X);

        List<Vec> many = new List<Vec>();
        many.Add(one);
        many.Add(copy);
        Console.PrintLine("" + many.Count);

        int total = 0;
        foreach (Vec spot in spots) {
            total = total + spot.X;
        }
        foreach (Vec spot in many) {
            total = total + spot.Y;
        }
        Console.PrintLine("" + total);

        int[] numbers = new int[4];
        numbers[0] = 5;
        numbers[3] = 7;
        int sum = 0;
        foreach (int number in numbers) {
            sum = sum + number;
        }
        Console.PrintLine("" + sum);

        string[] words = new string[2];
        words[0] = "a";
        words[1] = "b";
        string joined = "";
        foreach (string word in words) {
            joined = joined + word;
        }
        Console.PrintLine(joined);

        List<string> names = new List<string>();
        names.Add("x");
        foreach (string name in names) {
            Console.PrintLine(name);
        }
    }

    static Vec Same(Vec spot) {
        Vec back = spot;
        return back;
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `struct Vec` | Declara Vec, um struct com dois campos, X e Y, ambos do tipo int. |
| `class Held` | Declara uma classe com um campo struct chamado Spot e um campo estático chamado Origin. |
| `Vec copy = one; copy.X = 9;` | Guarda uma cópia de um em copy. Mudar a cópia deixa um como estava. |
| `Kept = one; held.Spot = one; Held.Origin = one;` | Cada armazenamento faz a sua própria cópia, então os três lugares guardam valores separados. |
| `Same(one)` | Entrega um a um método, que devolve uma cópia dele. |
| `spots[0] = one;` | Guarda uma cópia em um elemento do array. |
| `many.Add(one);` | Acrescenta uma cópia à lista. |
| `foreach (...)` | Percorre o array e a lista, e lê cada struct por vez. |

> [!DEFINE] Tipo de valor
> Um tipo cujos valores são copiados quando são guardados ou entregues, como um struct.

> [!DEFINE] Tipo de referência
> Um tipo cujos valores são um único objeto compartilhado: cada nome para ele vê o mesmo objeto, como em uma classe.

> [!DEFINE] Estático
> Pertence ao tipo, não a um objeto: cada parte do programa vê o mesmo valor.

> [!DEFINE] Array
> Uma sequência de valores de um tipo, com tamanho fixo, criada com new e o tamanho, como new Vec[3].

Um campo ao qual você não deu valor começa como um struct vazio, então um campo struct nunca fica sem valor. Um array de structs funciona do mesmo jeito: new Vec[3] cria três vazios.

## Se algo der errado

> [!PROBLEM] Um struct declara uma classe base, e o compilador o recusa com o código S3041.
> Um struct pode implementar interfaces, mas não pode ter uma classe base. Remova a classe base do struct.

> [!SEE] jsc:sigma_classes, jsc:sigma_library
