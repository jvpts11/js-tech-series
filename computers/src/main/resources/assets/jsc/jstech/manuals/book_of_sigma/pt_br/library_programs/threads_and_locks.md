---
section: library_programs
order: 444
---
# Threads e bloqueio

Uma thread é uma linha de trabalho dentro de um programa. As threads de um mesmo programa compartilham a memória, e só uma delas roda por vez: a máquina dá a vez a cada uma, em turnos.

Thread existe só no Sigma#. O Sigma não tem threads, e também não tem instrução lock.

Table: Membros de Thread

| Membro | O que faz |
| --- | --- |
| `Thread.Start(action)` | Inicia uma nova thread que executa o método para o qual a ação aponta. |
| `Thread.Current` | A thread que está executando esta parte do seu programa. |
| `Thread.Sleep(ticks)` | Para a thread por essa quantidade de ticks. |
| `Thread.Yield()` | Passa a vez para a próxima thread. |
| `Thread.Id` | O número da thread. |
| `Thread.Running` | Sim enquanto a thread estiver executando. |
| `Thread.Join()` | Espera até a thread terminar. |
| `Thread.Join(ticks)` | Espera no máximo essa quantidade de ticks. Dá sim quando a thread terminou nesse tempo. |
| `Thread.Stop()` | Para a thread. |

Uma thread executa algumas instruções de cada vez, e no máximo 64, antes que outra thread do mesmo programa tenha a sua vez. Sleep e Join contam ticks, e 20 ticks formam um segundo.

## Bloqueio

Um bloqueio impede que duas threads executem os mesmos passos sobre um objeto ao mesmo tempo. A instrução lock pega o bloqueio do objeto enquanto o seu bloco roda, e o solta quando o bloco termina.

1. Escreva lock, depois o objeto entre colchetes.
2. Depois dos colchetes, escreva a instrução que o bloqueio protege.

Só um objeto pode ser bloqueado. Uma thread que encontra o bloqueio em uso por outra thread espera, e tenta de novo quando o bloqueio estiver livre. Uma thread que já tem um bloqueio pode pegá-lo de novo, e então precisa soltá-lo tantas vezes quantas o pegou.

> [!DEFINE] Thread
> Uma linha de trabalho dentro de um programa, que compartilha a memória com as outras threads do programa.

> [!DEFINE] Bloqueio
> Um bloqueio sobre um objeto, para que apenas uma thread por vez execute os passos protegidos por ele.

## Se algo der errado

> [!PROBLEM] O programa para e diz que não há nenhum bloco para executar na thread.
> Dê ao Thread.Start um método com um bloco de passos para executar.

> [!PROBLEM] O programa para e diz que não há nenhuma thread aqui.
> A thread já terminou, ou nunca foi iniciada aqui. Verifique primeiro o Thread.Running.

> [!PROBLEM] O programa para e diz que esta thread está soltando um bloqueio que não possui.
> Use a instrução lock, que pega e solta o bloqueio em sequência.

> [!PROBLEM] O compilador diz que só um objeto pode ser bloqueado.
> Bloqueie um objeto, como um valor de uma classe que você escreve, e não um número comum.

> [!PROBLEM] O compilador recusa Thread ou lock no Sigma.
> Use o Sigma#, ou escreva o programa para que os passos rodem um após o outro.

> [!SEE] jsc:sigma_delegates_events, jsc:sigma_runtime_stops, jsc:running_programs
