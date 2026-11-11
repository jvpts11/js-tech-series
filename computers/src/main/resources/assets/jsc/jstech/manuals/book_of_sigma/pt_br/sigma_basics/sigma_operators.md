---
section: sigma_basics
order: 414
---
# Operadores e a sua ordem

Um operador é um símbolo que combina valores: + soma dois números, e == pergunta se dois valores são iguais. Um trecho de código que devolve um valor, como 2 + 3 ou um nome, é uma expressão. Você junta expressões com operadores para criar expressões maiores.

Quando uma linha tem vários operadores, a precedência decide qual deles é calculado primeiro. A multiplicação tem prioridade maior que a adição, então 2 + 3 \* 4 dá 14, e não 20. Parênteses mudam a ordem: (2 + 3) \* 4 dá 20.

Table: Precedência dos operadores

| Grupo | Operadores, começando pelo grupo mais fraco |
| --- | --- |
| Atribuição | `= += -= *= /= %= &= \|= ^= <<= >>= (right to left)` |
| Condicional | `?:` |
| Ou | `\|\|` |
| E | `&&` |
| OU bit a bit | `\|` |
| OU exclusivo bit a bit | `^` |
| E bit a bit | `&` |
| Igualdade | `== !=` |
| Comparação e testes de tipo | `< <= > >= is as` |
| Deslocamentos | `<< >>` |
| Adição | `+ -` |
| Multiplicação | `* / %` |
| Unários, antes de um valor | `! - + ~ ++ -- and casts` |
| Membro, chamada, índice e depois de um valor | `. () [] ++ --` |

Os operadores aritméticos são + para somar, - para subtrair, \* para multiplicar, / para dividir e % para o resto. Quando os dois lados de / são números inteiros, o resultado é um número inteiro: a fração é cortada, então 7 / 2 dá 3, e 7 % 2 dá 1. Dividir por zero, ou calcular um resto com zero, interrompe o programa com uma mensagem.

O operador + junta textos. Quando um dos lados é texto, o resultado é texto, então "y = " + y coloca o número depois das palavras. Os operadores de comparação < <= > >= funcionam somente com números. Para colocar dois textos em ordem, use string.Compare, como explica a página de texto.

Table: O que == compara

| Tipo de valor | O que compara |
| --- | --- |
| Números | Os valores deles. |
| bool | Se os dois são verdadeiros, ou os dois são falsos. |
| Texto | Os caracteres, um após o outro. |
| Struct e record | Cada campo, um após o outro. |
| Objeto de classe | Se os dois nomes apontam para o mesmo objeto, e não para o que ele guarda. |

Table: Operadores lógicos e bit a bit

| Operadores | Atuam sobre |
| --- | --- |
| `&& \|\| !` | Somente valores bool. |
| `& \| ^` | Valores bool ou números inteiros. |
| `<< >>` | Somente números inteiros. |
| `~` | Somente números inteiros. |
| `++ --` | Somente números, antes ou depois do nome. |

O operador ++ soma um, e -- tira um. Uma linha pode ter só o operador, como count++ sozinho. Dentro de uma expressão maior, a forma depois do nome dá o valor antigo, e a forma antes do nome dá o novo.

As formas compostas juntam um operador a uma atribuição. total += 5; faz o mesmo que total = total + 5;, e o mesmo vale para as outras formas compostas.

Um deslocamento move os bits do número à sua esquerda pela contagem à sua direita, e dá um número do tipo do lado esquerdo. Ele usa só os bits baixos da contagem, como o C#: cinco para um int e seis para um long. Assim, 1 << 32 é 1, e 1 << 33 é 2.

```sg Operators.sg
int x = 3;
int y = x * 2 + 1;        // 7
bool ok = y > 5 && y != 9;
int shifted = 1 << 4;     // 16
string label = "y = " + y;
```

> [!DEFINE] Expressão
> Um trecho de código que devolve um valor, como 2 + 3 ou um nome.

> [!DEFINE] Operador
> Um símbolo que combina valores, como + ou ==.

> [!DEFINE] Precedência
> A regra que decide qual operador é calculado primeiro quando uma linha tem vários.

## Se algo der errado

> [!PROBLEM] O compilador diz que '<' não pode ser aplicado a 'string' e 'string'.
> Compare textos com string.Compare, que devolve um número para colocá-los em ordem.

> [!PROBLEM] O compilador diz "uma condição é um bool, não 'int'".
> Um if, um while ou um for precisa de um teste verdadeiro ou falso. Escreva if (count > 0) em vez de if (count).

> [!PROBLEM] O compilador diz "só uma chamada, uma atribuição, um incremento, um decremento ou um novo objeto pode ser usado como um comando".
> Guarde o valor em uma variável, ou passe-o para um método, para que a linha faça alguma coisa.

> [!PROBLEM] O compilador diz "o lado esquerdo de uma atribuição precisa ser uma variável, um campo, uma propriedade ou um elemento".
> Coloque um nome à esquerda do sinal de igual, como total = 5;

> [!SEE] jsc:sigma_control_flow, jsc:sigma_strings_chars
