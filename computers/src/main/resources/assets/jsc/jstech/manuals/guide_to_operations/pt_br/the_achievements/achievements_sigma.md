---
section: the_achievements
order: 173
icon: jsc:cd_rom
---
# Conquistas de Sigma

Sigma e Sigma# são as duas linguagens de programação dos computadores, e a aba Sigma recompensa você por escrevê-las, compilá-las e executá-las. O Livro do Sigma explica as duas linguagens do zero ([](jsc:sigma_two_languages)).

A maioria das conquistas de Sigma de um programa credita o operador da máquina em que ele roda. Os comandos de compilar e executar creditam o jogador que os digitou.

Table: As conquistas de Sigma

| Conquista | O que pede | Como é ganha |
| --- | --- | --- |
| Sigma (raiz) | Instale o Compilador Σ#. | O pacote sgsc está instalado. |
| Erm, Que Sigma? (tarefa) | Execute um programa Sigma. | Um programa começa em um terminal. |
| Expected ';' but Found '}' (segredo) | Faça um programa falhar na compilação. | Uma compilação falha com erros, então o compilador não produz nada. |
| No Meu Tempo (meta) | Compile um programa Σ no MC-DOS. | Um programa Sigma, e não Sigma#, compila no MC-DOS. |
| Stack Overflow (segredo) | Faça um programa parar com 1.024 chamadas de profundidade. | A cadeia de chamadas de um programa vai além de 1.024 níveis de profundidade. |
| Comportamento Indefinido (segredo) | Faça um programa parar ao dividir por zero. | Um programa para com um erro de divisão por zero ou de resto por zero. |
| Agora Você Tem Dois Problemas (tarefa) | Inicie uma segunda thread em um programa. | Um programa inicia uma thread. |
| Olha, Mãe, Sem Console (tarefa) | Abra uma janela a partir de um programa. | Um programa abre uma janela. |
| Stonks (tarefa) | Faça um programa ser acordado por um observador de estoque da rede. | Um observador dispara e acorda um programa. |
| Na Minha Máquina Funciona (tarefa) | Execute o mesmo programa em dois computadores. | Um programa começa em uma máquina diferente daquela onde você o executou pela primeira vez. |
| Ship It (meta) | Empacote um programa com o sgpack e publique-o no Mirror. | Um pacote é publicado no Mirror. |
| Automatize as Coisas Chatas (meta) | Faça um programa iniciar uma Operação na rede. | Um programa inicia uma Operação. |
| Cinco Noves (desafio) | Mantenha um programa rodando por sete dias sem parar. | Um programa roda sem parar por 168.000 ticks. |
| Depuração com Patinho de Borracha (tarefa) | Pare um programa num breakpoint. | O depurador do Virtual Studio para o seu programa num breakpoint. |

Cinco Noves conta ticks, não segundos: 24.000 ticks formam um dia no jogo, então sete dias somam 168.000 ticks. Um tick é a unidade básica de tempo do jogo, um vigésimo de segundo ([](jsc:units_and_ticks)).

Na Minha Máquina Funciona precisa que o jogo se lembre de onde você executou um programa: ele guarda até 64 nomes de programa para cada jogador.

## Se algo der errado

> [!PROBLEM] A conquista Expected ';' apareceu por um erro que você não esperava.
> Qualquer compilação com falha ganha a conquista, seja qual for o erro. Leia a linha do erro para encontrar o problema real ([](jsc:sigma_reading_an_error)).

> [!PROBLEM] Um programa para com uma mensagem sobre chamadas.
> As chamadas passaram de 1.024 níveis de profundidade: um método pode estar chamando a si mesmo sem fim. Procure um método que chame a si mesmo ([](jsc:sigma_runtime_stops)).

> [!PROBLEM] Um programa para com "dividido por zero".
> Verifique o divisor antes de dividir. Um programa que para desse jeito ganha Comportamento Indefinido.

> [!PROBLEM] Cinco Noves não aparece.
> O programa precisa rodar sem parar por 168.000 ticks. O jogo verifica a cada 1.200 ticks, então a conquista pode aparecer até um minuto depois.

> [!SEE] jsc:sigma_compile, jsc:sigma_run, jsc:sigma_limits, jsc:book_of_sigma
