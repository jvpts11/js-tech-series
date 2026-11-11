---
section: library_basics
order: 439
---
# Console: lendo e escrevendo no terminal

O console é a ligação de um programa com o terminal: a tela em que o programa é iniciado. Tudo o que você imprime aparece ali, e tudo o que você lê é uma linha que o jogador digitou ali. A maioria dos programas começa imprimindo uma pergunta e lendo a resposta.

Chamadas ao console não custam nada. Print não encerra a linha, mas PrintLine encerra. Uma leitura espera até que uma linha seja digitada, e um programa que espera é colocado em espera.

Table: Membros do console

| Membro | O que faz | Linguagem |
| --- | --- | --- |
| `Console.Print(text)` | Escreve o texto e deixa a linha aberta. | Sigma e Sigma# |
| `Console.Print(char)` | Escreve um caractere. | Sigma e Sigma#, versão 2 |
| `Console.PrintLine(text)` | Escreve o texto e encerra a linha. | Sigma e Sigma# |
| `Console.Clear()` | Limpa as linhas que o console guarda. | Sigma e Sigma# |
| `Console.ReadLine()` | Espera por uma linha e a devolve como texto. | Sigma e Sigma# |
| `Console.HasLine()` | Sim ou não: há uma linha digitada esperando? Nunca espera. | Somente Sigma# |
| `Console.Read()` | O próximo caractere como número, incluindo a quebra de linha. | Sigma e Sigma#, versão 2 |
| `Console.Scan(out value)` | Lê um valor do tipo da variável, dando 1 quando funciona e 0 quando o texto não é um valor desse tipo. | Sigma e Sigma#, versão 2 |
| `Console.ReadInt()` | Lê a linha inteira como um número inteiro. | Sigma e Sigma# |
| `Console.ReadLong()` | Lê a linha inteira como um número inteiro longo. | Somente Sigma# |
| `Console.ReadDouble()` | Lê a linha inteira como um número decimal. | Somente Sigma# |
| `Console.ReadBool()` | Lê a linha inteira como verdadeiro ou falso. | Sigma e Sigma# |

Depois de Read ou Scan, o resto da linha aguarda a próxima leitura. Quando Scan falha em uma variável de texto, a variável fica com texto vazio.

ReadBool aceita true, yes, y, on ou 1 como verdadeiro, e false, no, n, off ou 0 como falso. Ele ignora maiúsculas e espaços, então você pode digitar Yes ou OFF.

O terminal guarda no máximo 16 linhas digitadas esperando. A saída também é guardada: as últimas 200 linhas, no máximo 4.096 caracteres por linha, e 64.000 caracteres no total. As linhas mais antigas saem primeiro, e uma linha cortada termina com um marcador.

> [!DEFINE] Terminal
> A tela em que um programa é iniciado: o prompt da máquina ou uma janela.

> [!DEFINE] Em espera
> Um programa que espera por algo fora de si mesmo. Um programa em espera não gasta créditos enquanto espera.

## Se algo der errado

> [!PROBLEM] O programa para e informa que o texto não é um número.
> Digite um número inteiro. Para continuar mesmo com uma entrada inválida, leia uma linha com ReadLine e converta-a com Convert.ToInt(text, fallback).

> [!PROBLEM] O programa para e informa que o texto não é verdadeiro nem falso.
> ReadBool aceita apenas true, yes, y, on ou 1 como verdadeiro, e false, no, n, off ou 0 como falso. Digite um desses.

> [!PROBLEM] O programa espera e nada aparece.
> Ele espera por uma linha. Digite uma no terminal em que o programa está em primeiro plano.

> [!PROBLEM] As linhas de saída antigas sumiram.
> O console guarda apenas as últimas 200 linhas. Imprima menos, ou grave o que você precisa em um arquivo.

> [!PROBLEM] Uma linha de saída termina com um marcador de corte.
> Uma linha comporta 4.096 caracteres. Divida textos longos em várias linhas.

> [!SEE] jsc:sigma_runtime_stops
