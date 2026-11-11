---
section: desktop_apps
order: 85
icon: jsc:monitor
---
# Campo Minado, Paciência e Snake

Três jogos rodam nos sistemas com área de trabalho. Cada um é instalado como qualquer outro programa ([](jsc:programs)), e precisa de 1 megabyte (MB) de memória.

## Campo Minado

Revele uma célula para ver quantas minas a tocam. Um número mostra essa contagem, na sua própria cor. Uma célula vazia abre as células ao redor dela. Você marca com uma bandeira uma célula que acha que tem uma mina.

> [!DEFINE] Bandeira
> Uma bandeira que você coloca em uma célula que acha que tem uma mina.

Table: Níveis do Campo Minado

| Nível | Botão | Grade | Minas |
| --- | --- | --- | --- |
| Iniciante | `Beg` | 9 por 9 | 10 |
| Intermediário | `Int` | 16 por 16 | 40 |
| Especialista | `Exp` | 16 por 30 | 99 |

O jogo começa no nível Iniciante.

## Paciência

Paciência é Klondike. Você tira uma carta por vez do Estoque, a pilha virada para baixo, para o descarte, que guarda as cartas que você tirou. As sete colunas de cartas são as Colunas, e as quatro pilhas, uma para cada naipe, são as fundações, construídas do ás até o rei.

> [!DEFINE] Coluna
> As sete colunas de cartas da Paciência, onde as cartas ficam dispostas em fila.

> [!DEFINE] Fundação
> Uma das quatro fundações da Paciência, pilhas onde um naipe é montado do ás ao rei.

Table: Pontos na Paciência

| Movimento | Pontos |
| --- | --- |
| Descarte para Coluna | 5 pontos |
| Virar uma carta | 5 pontos |
| Para uma Fundação | 10 pontos |
| De uma Fundação de volta para a Coluna | menos 15 pontos |
| Descarte de volta para o estoque | menos 100 pontos |

A janela mostra a pontuação e os movimentos, e diz "Você venceu" quando o jogo termina. O botão Novo inicia outro jogo.

## Snake

O Snake roda em uma grade de 20 por 12 células. Você come a comida para crescer, e cada comida vale 10 pontos. A cobra começa com tamanho 4. Pressione uma tecla de seta para começar.

A velocidade define quantos ticks passam entre dois passos. Um tick é a pulsação do jogo, e 20 ticks formam um segundo ([](jsc:units_and_ticks)).

Table: Velocidades do Snake

| Velocidade | Um passo a cada |
| --- | --- |
| Lenta | 6 ticks |
| Normal | 4 ticks |
| Rápida | 2 ticks |

Com as paredes ligadas, bater em uma parede encerra o jogo. Com as paredes desligadas, a grade dá a volta, e só bater em si mesma encerra o jogo. A janela mostra a pontuação, a melhor pontuação e o tamanho, e diz "Fim de jogo" quando você perde.

> [!SEE] jsc:programs, jsc:units_and_ticks
