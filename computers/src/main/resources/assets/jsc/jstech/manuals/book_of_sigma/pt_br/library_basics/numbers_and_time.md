---
section: library_basics
order: 441
---
# Math, Convert, Random e Time

Os programas calculam com números, e a biblioteca oferece as somas e conversões comuns em Math e Convert. Random dá números que você não consegue prever, e Time diz ao programa o que o relógio do mundo marca. Chamadas a estes não custam nada.

## Math

Table: Membros de Math

| Membro | O que faz |
| --- | --- |
| `Math.Abs(number)` | O tamanho de um número, sem o sinal. |
| `Math.Min(a, b)` | O menor de dois números. |
| `Math.Max(a, b)` | O maior de dois números. |
| `Math.Floor(number)` | O número inteiro igual ou abaixo de um número decimal. |
| `Math.Ceil(number)` | O número inteiro igual ou acima de um número decimal. Só no Sigma#. |
| `Math.Round(number)` | O número inteiro mais próximo. Só no Sigma#. |
| `Math.Sqrt(number)` | A raiz quadrada de um número decimal. |
| `Math.Pow(number, power)` | Um número elevado a uma potência. |
| `Math.Clamp(value, low, high)` | O valor, mantido entre low e high. Só no Sigma#. |

O Sigma oferece Abs, Min, Max, Floor, Sqrt e Pow. Os outros membros aqui são só do Sigma#.

## Convert

Table: Membros de Convert

| Membro | O que faz |
| --- | --- |
| `Convert.ToInt(text)` | O número inteiro no texto. Para o programa quando o texto não é um número. |
| `Convert.ToInt(text, fallback)` | O número inteiro no texto, ou o valor alternativo quando o texto não é um número. Versão 2. |
| `Convert.ToLong(text)` | Um número inteiro longo. Só no Sigma#. |
| `Convert.ToFloat(text)` | Um número decimal curto. Só no Sigma#. |
| `Convert.ToDouble(text)` | O número decimal do texto. |
| `Convert.ToBool(text)` | Verdadeiro ou falso, a partir do texto. |
| `Convert.ToString(value)` | O texto de um valor. |
| `Convert.ToString(number, base)` | O número como texto em uma base de 2 a 36. Versão 2. |
| `Convert.TryInt(text, out value)` | Verdadeiro e o número quando o texto é um número, falso caso contrário. Só no Sigma#. |
| `Convert.TryLong, Convert.TryDouble, Convert.TryBool` | O mesmo que TryInt, para os outros tipos. Só no Sigma#. |

A base é quantos dígitos diferentes um número usa para ser escrito. A base 10 é o jeito comum, e a base 16 é hexadecimal. A base 10 mantém o sinal de menos. Todas as outras bases escrevem os 32 bits do número, então -1 na base 16 é ffffffff.

Convert.ToBool aceita true, yes, y, on ou 1 como verdadeiro, e false, no, n, off ou 0 como falso, em qualquer combinação de maiúsculas e minúsculas.

## Random

Table: Membros de Random

| Membro | O que faz |
| --- | --- |
| `Random.Next(bound)` | Um número inteiro de 0 até o limite, sem incluir o limite. |
| `Random.NextDouble()` | Um número decimal de 0 até 1, sem incluir o 1. Só no Sigma#. |
| `Random.Seed(number)` | Recomeça a sequência a partir desse número. |

Random está no Sigma# a partir da versão 1, e no Sigma a partir da versão 2. Cada programa tem a sua própria sequência, e semear com o mesmo número dá a mesma sequência de novo.

## Time

Table: Membros de Time

| Membro | O que faz |
| --- | --- |
| `Time.Tick` | A contagem de ticks do mundo, como número inteiro longo. |
| `Time.DayTime` | A hora do dia em ticks, de 0 até 23.999. Só no Sigma#. |
| `Time.Day` | O número do dia do relógio do mundo. |
| `Time.Ticks(seconds)` | O número de ticks nessa quantidade de segundos. Só no Sigma#. |

Um tick é um passo do relógio do mundo, e o jogo roda 20 ticks por segundo. Time.Ticks é o único ponto em que a biblioteca converte segundos em ticks. Os créditos que um programa recebe a cada tick são explicados em [](jsc:credits_per_tick).

> [!DEFINE] Limite superior
> O número abaixo do qual um sorteio de número inteiro fica. Random.Next(6) dá de 0 a 5.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o texto não é um número.
> Dê um valor alternativo como segundo valor de Convert.ToInt, ou verifique o texto antes com Convert.TryInt.

> [!PROBLEM] O programa para e diz que não há base nesse número.
> Use uma base de 2 a 36, como 10 ou 16.

> [!PROBLEM] Random.Next sempre dá 0.
> Dê um limite de 1 ou mais. Um limite de 6 dá números de 0 a 5.

> [!SEE] jsc:sigma_types_variables, jsc:sigma_runtime_stops
