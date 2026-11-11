---
section: sigma_data
order: 423
---
# Texto e caracteres

Texto é uma string: uma linha de caracteres entre aspas duplas. Uma string é um tipo de referência, então pode ser null. Um char é um caractere só, escrito entre aspas simples, e, dentro da máquina, é o seu número.

Você une textos com +. Quando um dos lados é texto, o resultado é texto, então "n = " + 5 dá n = 5. Dois textos são iguais com == quando guardam os mesmos caracteres. Texto não pode ser comparado com < ou >: para pôr textos em ordem, use string.Compare, que precisa da versão 2 do projeto. Ele devolve um número abaixo de zero, zero ou acima de zero.

Table: Membros de texto

| Membro | Devolve | O que faz |
| --- | --- | --- |
| `Length` | Um int | Quantos caracteres o texto tem. É uma propriedade, então não coloque parênteses. |
| `Substring(start)` | Texto | O texto desde a posição inicial até o fim. |
| `Substring(start, length)` | Texto | Um pedaço do texto, do tamanho que você informar. |
| `IndexOf(text)` | Um int | A posição da primeira ocorrência, contada a partir de 0, ou -1 quando não há nenhuma. |
| `Contains, StartsWith, EndsWith` | Um bool | Se o texto contém, começa ou termina com o pedaço que você informar. |
| `ToUpper(), ToLower(), Trim()` | Texto | Um novo texto em maiúsculas, em minúsculas, ou sem espaços nas pontas. |
| `Replace(old, new)` | Texto | Um novo texto com cada pedaço antigo trocado pelo novo. |
| `Split(char)` | Uma Lista de texto, só no Sigma# | Os pedaços entre o caractere que você informar. |
| `string.Format(format, value)` | Texto | Monta um texto a partir de um formato e dos valores para as suas lacunas. |
| `string.Compare(a, b)` | Um int | Coloca dois textos em ordem: abaixo de zero, zero ou acima de zero. |

Texto não pode ser indexado como um array: text[0] é recusado. Para pegar o primeiro caractere como texto, use Substring(0, 1).

Um char tem testes próprios: char.IsDigit(c), char.IsLetter(c), char.IsWhiteSpace(c), char.ToUpper(c) e char.ToLower(c). Eles precisam da versão 2 do projeto. Os nomes antigos, como toupper, estão na [página de nomes antigos](jsc:sigma_printf_old_names).

Um char em aritmética vira um número. 'A' + 1 é um int, não um caractere.

```sg Names.sg
string name = "Ada Lovelace";
int first = name.IndexOf(" ");
string given = name.Substring(0, first);     // "Ada"
bool long_name = name.Length > 8;
string shout = given.ToUpper();              // "ADA"
int order = string.Compare("apple", "banana"); // below zero
```

> [!DEFINE] Índice
> A posição de um item em uma linha, contada a partir de 0, então o primeiro item fica na posição 0.

## Se algo der errado

> [!PROBLEM] O compilador diz que 'string' não pode ser indexado por 'int'.
> Use Substring para pegar um pedaço do texto.

> [!SEE] jsc:sigma_list_map, jsc:sigma_printf_old_names
