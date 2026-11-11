---
section: automation_jobs
order: 154
icon: jsc:standard_redstone_interface
---
# Redstone em programas

Um programa pode ler um sinal de redstone, ou criar um, por meio de uma Redstone Interface ([](jsc:redstone_interfaces)). Você coloca a interface, a liga ao seu computador com cabo e deixa que um programa a leia ou a defina.

Primeiro, dê um nome à interface na sua janela. Um programa encontra a interface por esse nome. O nome tem até 32 caracteres e precisa ser único no seu computador, sem diferenciar maiúsculas de minúsculas. Um nome vazio responde por "Redstone Interface".

Se você der a uma segunda interface um nome que a primeira já tem, a janela a recusa com "Outra interface já tem este nome".

## A partir do IQL

A instrução SET REDSTONE faz uma interface ler ou emitir. O livro do IQL a explica por completo ([](jsc:set_redstone)).

Table: Instruções SET REDSTONE

| Instrução | O que faz |
| --- | --- |
| `SET REDSTONE 'Gate' IN` | Faz a interface chamada Gate ler. |
| `SET REDSTONE 'Gate' OUT 15` | Faz a interface chamada Gate emitir uma potência de 15. |
| `SET REDSTONE 'Main Gate' IN` | O mesmo, para um nome com espaço. As aspas vão em volta do nome. |

A potência vai de 0 a 15. Uma instrução digitada em um prompt marca o ajuste como feito pelo IQL, e a janela então mostra "Definido por IQL".

## A partir do Sigma

Um programa Sigma alcança uma interface por meio de redstone("Gate"). O livro do Sigma explica a sua biblioteca ([](jsc:redstone_library)).

Table: O Sigma chama uma interface

| Chamada | O que faz |
| --- | --- |
| `redstone("Gate")` | Encontra a interface chamada Gate no computador onde o programa roda. |
| `.In()` | Faz a interface ler. |
| `.Out(15)` | Faz a interface emitir 15, de 0 a 15. |
| `.Level()` | Devolve a potência que ela lê ou emite, ou 0 enquanto não tiver energia. |

```sgs Lamp.sgs
redstone("Gate").Out(15);
```

Cada chamada marca o ajuste com o nome do programa que o fez, então a janela mostra esse programa.

## Limites

Um programa alcança apenas as interfaces do seu próprio computador. Uma interface em outro computador não é encontrada, mesmo quando os dois estão na mesma rede.

Uma instrução SET REDSTONE em uma tarefa roda a cada vez que a tarefa dispara, então uma tarefa pode abrir uma porta em um temporizador ([](jsc:automation_engine)).

## Se algo der errado

> [!PROBLEM] O IQL diz "nenhuma Interface de Redstone chamada Gate está ligada a esta máquina".
> Confira a grafia do nome na janela da interface. Confira se o cabo dela chega a este computador e se a interface não está desativada.

> [!PROBLEM] O Sigma para o programa e diz que a interface não está ligada a esta máquina.
> Confira o nome, e confira o cabo e a configuração de desativação, como acima.

> [!PROBLEM] O IQL diz "SET REDSTONE precisa de IN, ou de OUT e uma força de 0 a 15".
> Escreva IN, ou OUT seguido de um número de 0 a 15.

> [!PROBLEM] O IQL diz que o valor não é uma força de redstone, que vai de 0 a 15.
> Use um número de 0 a 15.

> [!PROBLEM] O Sigma para o programa, porque a força não está entre 0 e 15.
> Passe um número de 0 a 15 para Out.

> [!PROBLEM] A janela diz "Sem energia".
> A interface não está ativa. Confira o cabo periférico dela e se a interface não está desativada.

> [!PROBLEM] A janela recusa um nome com "Outra interface já tem este nome".
> Dê outro nome a esta interface.

> [!SEE] jsc:redstone_interfaces, jsc:set_redstone, jsc:redstone_library, jsc:peripheral_cables
