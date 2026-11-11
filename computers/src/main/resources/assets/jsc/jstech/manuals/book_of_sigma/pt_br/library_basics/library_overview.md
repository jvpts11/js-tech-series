---
section: library_basics
order: 437
---
# A biblioteca e as duas linguagens

Um programa alcança a máquina pela biblioteca. A biblioteca é um conjunto de tipos com nome, e cada tipo tem membros que você chama, como o terminal, os arquivos, o relógio e a rede.

Sigma e Sigma# usam ambos a biblioteca, mas não alcançam a mesma parte dela. O Sigma é a linguagem menor, e alcança apenas o namespace Standard da biblioteca. O Sigma# é a linguagem inteira, e alcança todos os namespaces System, como System.IO e System.Sound.

Todo nome em Standard é um tipo que o Sigma# também tem, sob o namespace que sempre teve. Uma linha using, explicada em [](jsc:sigma_namespaces_using), traz um namespace para dentro.

## Trazendo nomes para dentro de um programa

1. Escreva using Standard.\*; no topo do arquivo, para trazer tudo o que está em Standard.
2. Abaixo da linha using, escreva namespace, depois um nome seu e um ponto e vírgula, como namespace MyProgram;.
3. Escreva o ponto de entrada do seu tipo de programa, como mostra a tabela abaixo.

Table: Onde um programa começa

| Tipo de programa | O que você escreve |
| --- | --- |
| Um programa de console | `static void Main()` |
| Um script | Uma classe que estende Script, com OnInit, OnTick ou OnDestroy escritos dentro dela. |

Script é uma classe base em Standard. Seus três métodos não fazem nada até que a sua classe escreva os próprios.

Table: As duas linguagens

| Linguagem | Nomes que alcança | Compilador | Arquivo-fonte |
| --- | --- | --- | --- |
| Sigma | Standard | `scc` | `.sg` |
| Sigma# | Todo namespace System, como System.IO | `sgsc` | `.sgs` |

A versão é o nível da linguagem que uma compilação deve seguir. Há duas versões, 1 e 2, e a versão 2 é a mais recente. Uma compilação que não pede versão segue a mais recente.

Este livro marca cada nome que entrou na versão 2. Uma compilação da versão 1 recusa esses nomes, então um programa que os usa precisa da versão mais recente.

## Nomes que o Sigma recusa

O Sigma recusa alguns nomes que o Sigma# tem. O compilador indica o substituto que espera que você use.

Table: Nomes que o Sigma recusa

| Nome | O que usar no lugar |
| --- | --- |
| `List` | Um array, e um contador próprio ao lado dele. |
| `Map` | Dois arrays, ou um array no qual você procura pela chave. |
| `Thread` | Nada: essas máquinas executam uma coisa de cada vez. |

Um membro que o Sigma não oferece também é recusado. O compilador lista os membros que o tipo tem, em ordem alfabética, para que você possa escolher um deles.

> [!DEFINE] Namespace
> Um grupo de tipos com um nome com pontos, como System.IO. Uma linha using traz o grupo para o programa, e então os tipos podem ser escritos só pelo nome simples.

> [!DEFINE] Biblioteca
> O conjunto de tipos e de seus membros que um programa pode chamar, como Console, File e Network.

> [!DEFINE] Membro
> Um método, valor ou evento que pertence a um tipo.

> [!DEFINE] Versão
> O nível da linguagem que uma compilação deve seguir: 1 é o primeiro nível, e 2 é o mais recente.

> [!DEFINE] Subconjunto
> A linguagem menor, Sigma, que oferece menos nomes do que o Sigma#.

## Se algo der errado

> [!PROBLEM] O compilador diz que um nome está em um namespace que você não trouxe, e pede uma linha using.
> Adicione no topo do arquivo a linha using que o nome indica: using System.IO.\*; para tudo do namespace, ou using System.IO.<name>; para um só tipo.

> [!PROBLEM] O compilador diz que um nome é um namespace e recusa usá-lo sozinho.
> Escreva .\* depois do namespace para trazer tudo que há nele.

> [!PROBLEM] O compilador diz que o Sigma não tem a biblioteca System, e que tudo o que o Sigma tem está em Standard.
> Escreva using Standard.\*; e use os nomes que ele contém, ou escreva o programa em Sigma#.

> [!PROBLEM] O compilador recusa List, Map ou Thread.
> Use a substituição da tabela de nomes que o Sigma recusa, ou escreva o programa em Sigma#, que tem todos eles. Threads têm a sua própria página ([](jsc:threads_and_locks)).

> [!PROBLEM] O compilador recusa um membro que o Sigma não oferece.
> Use um dos membros que o compilador lista na mensagem.

> [!PROBLEM] Uma compilação da versão 1 recusa um nome que entrou na versão 2.
> Use a versão mais recente do compilador, ou escreva o programa sem esse nome.

> [!SEE] jsc:sigma_first_program, jsc:sigma_reading_an_error
