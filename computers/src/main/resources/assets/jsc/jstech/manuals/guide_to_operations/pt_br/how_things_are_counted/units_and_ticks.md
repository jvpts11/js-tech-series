---
section: how_things_are_counted
order: 14
---
# Unidades e ticks

Várias unidades aparecem neste manual e nas telas. Esta página explica cada uma, para que os números façam sentido para você.

Table: As unidades deste livro

| Unidade | O que mede |
| --- | --- |
| `item` | Uma unidade de tudo que a rede guarda. O espaço do disco é contado em itens. |
| `MB, GB, TB` | O tamanho real de um disco ou de um módulo de memória, em megabytes, gigabytes e terabytes. |
| `it/t` | Itens por tick: a dica do processador mostra esse valor. |
| `W` | Watts: quanta energia uma fonte de alimentação fornece, e quanto uma peça consome. |
| `s` | Segundos. O gerenciador de inicialização conta em segundos. |

> [!DEFINE] Tick
> Uma das batidas do jogo. 20 ticks formam um segundo, então um tick é um vigésimo de segundo.

> [!DEFINE] Itens por tick (it/t)
> Quantos itens um processador trata em um tick. Um processador mais rápido trata mais itens a cada tick.

O jogo também conta o tempo em ticks. O gerenciador de inicialização espera 5 segundos, que são 100 ticks ([](jsc:boot_menu)).

A memória também é contada em itens. O buffer de cada módulo de memória guarda um item para cada 4 MB do seu tamanho, e os discos são contados pela era de cada disco ([](jsc:items_as_data)).

> [!SEE] jsc:items_as_data, jsc:boot_menu
