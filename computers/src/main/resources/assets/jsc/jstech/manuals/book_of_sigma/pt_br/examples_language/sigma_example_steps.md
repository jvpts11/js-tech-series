---
section: examples_language
order: 510
---
# Passos

Um passo muda uma variável em um, ou em uma quantidade que você informa. Um incremento soma um, um decremento tira um, e uma atribuição composta faz uma operação sobre a variável e guarda o resultado.

Você escreve ++ ou -- antes ou depois de um nome. Antes do nome, você obtém o novo valor. Depois do nome, obtém o valor antigo. O exemplo mostra os dois para cada tipo de lugar que você pode mudar.

## O programa

```sgs Steps.sgs
using System.*;
using System.Collections.*;
namespace Corpus;
class Counter {
    public int Value;
    public long Big;
    public float Small;
    public double Wide;
    public static int Shared;
}
class StepsCorpus {
    static int Total;

    static void Main() {
        int local = 0;
        local++;
        local--;
        ++local;
        --local;
        int before = local++;
        int after = ++local;
        int down = local--;
        int up = --local;
        Console.PrintLine("" + before + after + down + up);

        long big = 0;
        float small = 0;
        double wide = 0;
        big++;
        small++;
        wide--;
        Console.PrintLine("" + big + small + wide);

        Total++;
        --Total;
        int shared = Total++;
        Counter.Shared++;
        int other = ++Counter.Shared;
        Console.PrintLine("" + shared + other);

        Counter counter = new Counter();
        counter.Value++;
        --counter.Value;
        int held = counter.Value++;
        int heldUp = ++counter.Value;
        counter.Big++;
        counter.Small--;
        counter.Wide++;
        Console.PrintLine("" + held + heldUp + counter.Value + counter.Big);

        int[] numbers = new int[4];
        numbers[0]++;
        --numbers[1];
        int inside = numbers[2]++;
        int insideUp = ++numbers[3];
        Console.PrintLine("" + inside + insideUp + numbers[0]);

        List<int> list = new List<int>();
        list.Add(1);
        list[0]++;
        int fromList = list[0]--;
        Console.PrintLine("" + fromList + list[0]);

        Map<string, int> map = new Map<string, int>();
        map.Put("a", 1);
        map["a"]++;
        int fromMap = ++map["a"];
        Console.PrintLine("" + fromMap);

        int kept = 0;
        Action count = () => {
            kept++;
            int was = kept++;
            int now = ++kept;
            kept--;
            Console.PrintLine("" + was + now);
        };
        count();
        Console.PrintLine("" + kept);

        int combined = 1;
        combined += 2;
        combined -= 1;
        combined *= 3;
        combined /= 2;
        combined %= 4;
        combined &= 6;
        combined |= 1;
        combined ^= 3;
        combined <<= 2;
        combined >>= 1;
        Console.PrintLine("" + combined);

        string text = "a";
        text += "b";
        text += combined;
        Console.PrintLine(text);

        counter.Value += 5;
        Counter.Shared += 2;
        numbers[0] += 3;
        list[0] += 4;
        int answered = counter.Value += 1;
        int alsoAnswered = numbers[0] += 1;
        int plain = local = 7;
        Console.PrintLine("" + answered + alsoAnswered + plain);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `local++; local--; ++local; --local;` | Muda uma variável local em um, para cima e para baixo, com o operador antes e depois do nome. |
| `int before = local++;` | Guarda o valor antigo em before, e depois soma um a local. |
| `int after = ++local;` | Soma um a local primeiro, e depois guarda o novo valor em after. |
| `big++; small++; wide--;` | Muda um long, um float e um double em um. |
| `Total++; Counter.Shared++;` | Muda um campo estático do programa e um campo estático de uma classe. |
| `counter.Value++;` | Muda um campo de um objeto. |
| `numbers[0]++;` | Muda um elemento de um array. |
| `list[0]++;` | Muda um elemento de uma lista. |
| `map["a"]++;` | Muda o valor guardado sob a chave a em um mapa. |
| `kept++; kept--;` | Muda uma variável que a lambda guarda, a partir das linhas ao redor. |
| `combined += 2; ... combined >>= 1;` | Cada atribuição composta faz a sua operação sobre combined e guarda o resultado. |
| `text += combined;` | Acrescenta o número ao fim do texto. |
| `int answered = counter.Value += 1;` | Uma atribuição devolve o valor que guardou, então answered recebe o novo valor. |
| `int plain = local = 7;` | Define local como 7, e guarda 7 em plain também. |

> [!DEFINE] Incremento
> Somar um a um valor, escrito ++.

> [!DEFINE] Decremento
> Tirar um de um valor, escrito --.

> [!DEFINE] Prefixo
> Um operador escrito antes de um nome, como ++count, que dá o novo valor.

> [!DEFINE] Sufixo
> Um operador escrito depois de um nome, como count++, que dá o valor antigo.

> [!DEFINE] Atribuição composta
> Uma atribuição que faz antes uma operação sobre a variável, como total += 2, que significa total = total + 2.

> [!DEFINE] Variável capturada
> Uma variável em volta de uma lambda que a lambda guarda e pode mudar.

## Se algo der errado

> [!PROBLEM] Um valor de um tipo é guardado em uma variável de outro, e o compilador diz "não é possível converter" com o código S3003.
> Dê à variável um valor do próprio tipo, como um número inteiro para um int.

> [!PROBLEM] Uma condição não é um bool, como um número em um if, e o compilador o recusa com o código S3009.
> Escreva uma comparação que dê true ou false, como count > 0.

> [!PROBLEM] Uma linha tem só um valor, como 5;, e o compilador o recusa com o código S2006.
> Coloque uma atribuição, uma chamada ou um incremento na linha, ou guarde o valor em uma variável.

> [!PROBLEM] Uma atribuição vai para algo que não é uma variável, um campo, uma propriedade ou um elemento, com o código S2008.
> Atribua somente a uma variável, um campo, uma propriedade ou um elemento.

> [!PROBLEM] Um nome como List não é encontrado, e o compilador pede uma linha using, com o código S3040.
> Acrescente using System.Collections.\*; no início do arquivo, como faz o exemplo.

> [!SEE] jsc:sigma_types_variables, jsc:sigma_operators
