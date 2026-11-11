---
section: library_toolkit
order: 462
---
# StringBuilder e Regex: montar e casar texto

Juntar textos com + faz um texto novo toda vez. Quando você monta um texto longo aos pedaços, use um StringBuilder: cada pedaço entra pelo mesmo custo, por maior que o texto já seja. Os dois tipos ficam no System.Text e vieram com o Sigma# 3.

Table: Membros do StringBuilder

| Membro | O que faz |
| --- | --- |
| `new StringBuilder(), StringBuilder.new()` | Um montador vazio. |
| `StringBuilder.Append(text)` | Acrescenta o texto no fim e devolve o montador, para as chamadas virem uma atrás da outra. |
| `StringBuilder.AppendLine(text)` | Acrescenta o texto e uma quebra de linha. |
| `StringBuilder.Clear()` | Esvazia o montador. |
| `StringBuilder.ToString()` | Tudo o que foi acrescentado, como um texto só. |
| `StringBuilder.Length` | Quantos caracteres ele tem. |

Um padrão acha texto pela forma: \\d+ é um ou mais dígitos. O Regex casa um padrão escrito como Java e C# o escrevem.

Table: Membros do Regex

| Membro | O que dá |
| --- | --- |
| `Regex.IsMatch(text, pattern)` | Sim quando o padrão aparece em qualquer lugar do texto. |
| `Regex.Match(text, pattern)` | O primeiro casamento e os seus grupos, o casamento inteiro primeiro, ou uma lista vazia quando não há nenhum. |
| `Regex.Matches(text, pattern)` | Todos os casamentos, em ordem. |
| `Regex.Replace(text, pattern, with)` | O texto com cada casamento trocado. $1 na troca representa o primeiro grupo. |
| `Regex.Split(text, pattern)` | Os pedaços entre os casamentos. |

Um padrão custa um crédito a cada 4096 caracteres que lê. Alguns padrões leem o mesmo texto muitas vezes: um padrão que o leria mais de 256 vezes para o programa no lugar.

```sgs Builder.sgs
StringBuilder report = new StringBuilder();
foreach (string line in File.Read("stock.txt").Split('\n')) {
    if (Regex.IsMatch(line, "^iron")) {
        report.AppendLine(line);
    }
}
Console.PrintLine(report.ToString());
```

> [!DEFINE] Padrão
> Um texto que descreve a forma do texto a achar, como \\d+ para uma sequência de dígitos.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o texto não é um padrão.
> Confira os parênteses e as barras invertidas do padrão. Num texto, escreva a barra invertida duas vezes: "\\\\d".

> [!PROBLEM] O programa para e diz que o padrão leu o texto vezes demais.
> Escreva um padrão mais simples: um grupo repetido dentro de um grupo repetido é o que lê sem fim.

> [!SEE] jsc:numbers_and_time, jsc:data_library
