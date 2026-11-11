---
section: prompt_programs_and_network
order: 245
---
# Verbos de rede: net, iql, interac e ssh

Uma rede é o conjunto de computadores ligados por cabos de dados, com um Mainframe que os coordena ([](jsc:what_a_network_is)). net a descreve, iql faz perguntas a ela, interac trabalha com os seus itens, e ssh abre um shell em outro computador dela.

> [!DEFINE] Mainframe
> O computador que coordena uma rede. Cada rede tem um.

Table: Verbos da rede

| Verbo | Palavras depois dele, e o que faz |
| --- | --- |
| `net (network)` | Sem palavras. Resume a rede: o seu id, se há um Mainframe, e quantos servidores, computadores e subframes ela tem. |
| `iql (operation, op, sql)` | Uma instrução. Executa uma instrução IQL na rede e imprime a resposta. |
| `interac` | Palavras opcionais. Trabalha com os itens da rede: encontra, busca, devolve e fabrica. |
| `ssh` | O nome de um computador. Abre um shell em outro computador da rede. |

## net

net imprime as linhas network, mainframe, servers, computers, subframes e indexed types. A linha mainframe mostra present, ou MISSING quando não há Mainframe.

```command
id: jsc:command/mc_dos/net
```

## iql

iql executa uma instrução IQL na rede. Digite iql, um espaço e a instrução, como iql SELECT 64 Cobblestone. Um SELECT imprime cada item que encontra como uma linha com a sua quantidade, e mostra no máximo 64 linhas. Quando nada corresponde, responde nenhuma linha. Uma alteração, como INSERT, imprime as palavras do motor em verde.

Uma instrução que define uma visão, um procedimento ou uma tarefa é respondida como no IQL Server Management Studio ([](jsc:studio_overview)). A gramática do IQL é ensinada em [](jsc:iql), então esta página não a repete.

```command
id: jsc:command/mc_dos/iql
```

## interac

interac trabalha com os itens da rede. Ele encontra o que a rede guarda, traz um item para as suas mãos, devolve itens e os fabrica. As suas palavras incluem get, put, craft, where, info, list e ops. Sozinho, o interac ocupa a tela inteira, e [[F10]] devolve o terminal a você.

Table: Opções do interac

| Opção | O que faz |
| --- | --- |
| `/LOCAL` | Deixa o que o interac pega neste computador, não nas suas mãos. |
| `/S:COUNT` | Ordena uma listagem pela quantidade. --sort name ordena por nome. |

Por exemplo, interac get 42 cobblestone pega 42 cobblestone para as suas mãos, por meio deste computador. Um nome de item pode ser curto, como cobblestone, ou completo, como minecraft:cobblestone. Quando um nome combina com mais de uma coisa, o interac as lista e pede que você diga qual.

Esta página cobre os verbos mais comuns. As Operações por trás do trabalho do interac são descritas em [](jsc:operations_catalogue).

```command
id: jsc:command/mc_dos/interac
```

## ssh

ssh, o shell seguro, abre um shell em outro computador desta rede, para que você possa digitar comandos ali. Digite ssh e o nome do computador, como a lista o mostra. O prompt lista os computadores alcançáveis, cada um com o seu número de nó, ou (offline) quando está desligado. Digite exit para voltar ([](jsc:shell_verbs)).

```command
id: jsc:command/mc_dos/ssh
```

## Se algo der errado

> [!PROBLEM] não está em uma rede
> O computador precisa estar em uma rede de dados: um cabo de dados até um roteador, ou até o Mainframe ([](jsc:data_cables)).

> [!PROBLEM] iql responde syntax: e depois as palavras do analisador.
> A mensagem indica a palavra onde o analisador parou. Corrija essa palavra. A gramática está em [](jsc:iql).

> [!PROBLEM] iql SELECT responde nenhuma linha.
> Nada corresponde ao nome. Use interac list com parte do nome para conferir a grafia.

> [!PROBLEM] nenhum outro computador alcançável nesta rede
> Nenhum outro computador desta rede está ligado e alcançável. Tente de novo quando algum estiver.

> [!SEE] jsc:what_a_network_is, jsc:first_network, jsc:iql
