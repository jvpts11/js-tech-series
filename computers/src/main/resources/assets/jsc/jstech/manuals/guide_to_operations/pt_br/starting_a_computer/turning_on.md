---
section: starting_a_computer
order: 3
icon: jsc:personal_computer
---
# Ligando o computador

Ligar um computador começa uma sequência que você pode acompanhar no monitor. Ela começa com uma verificação do hardware, e depois procura um sistema para iniciar.

A verificação é o autoteste de inicialização (POST). O firmware ([](jsc:firmware)) verifica cada peça que encontra, e a lista na tela, uma linha depois da outra.

## Ligando o computador

1. Aperte o botão de energia na frente da caixa ([](jsc:case_front)).
2. Observe o monitor enquanto o autoteste roda.
3. Pressione [[DEL]] durante o autoteste para entrar no Setup, ou [[F12]] para abrir o Menu de Inicialização.

Se você desligar o computador durante o autoteste, a tela dele fica ligada até você pressionar [[Esc]].

Table: As teclas enquanto o computador inicia

| Tecla | O que faz |
| --- | --- |
| `DEL` | Entra no Setup, enquanto o autoteste roda. |
| `F12` | Abre o Menu de Inicialização, quando um disco ou mídia pode iniciar o computador. |
| Qualquer tecla | Depois de um autoteste que não encontra nada para iniciar, vai para o Setup. |

## O que a tela de inicialização mostra

Uma nova linha aparece a cada 0,15 segundo, mais ou menos. Estas são as linhas, em ordem.

```table
{"caption": "As linhas da inicialização", "heads": ["Linha", "O que ela informa"], "rows": [[{"literal": "Processor :"}, {"words": "O nome do processador, seus núcleos e a velocidade do clock, ou \"não detectado\". Computadores Legacy mostram Main Processor."}], [{"literal": "Board     :"}, {"words": "O nome da placa-mãe. Computadores Legacy não mostram esta linha."}], [{"literal": "Memory    :"}, {"words": "A memória contada até agora, que sobe enquanto é testada, e depois OK."}], [{"literal": "Video     :"}, {"words": "O nome da placa de vídeo, ou none quando não há placa. Computadores Legacy mostram Video Adapter."}], [{"literal": "Detecting drives"}, {"words": "O firmware procura discos e drives, e lista cada um que encontra."}], [{"literal": "Booting from"}, {"words": "O disco do qual o computador inicia, quando já encontrou um sistema para iniciar."}]]}
```

O autoteste leva entre 2 e 12 segundos. Quanto mais módulos de memória e dispositivos ele encontrar, mais demora, e uma era mais nova termina mais cedo.

Depois do autoteste, o sistema carrega, o que leva entre 2 e 20 segundos. Um sistema grande e uma memória curta deixam isso mais longo. Desligar leva um terço do tempo de inicialização, mas nunca menos de 1 segundo nem mais de 5 segundos.

> [!DEFINE] Autoteste de inicialização (POST)
> A verificação que o firmware faz toda vez que o computador liga. Ela lista cada peça que encontra, uma linha por vez.

> [!DEFINE] Setup
> As próprias telas de configuração do firmware. Suas páginas mostram os discos e o hardware que o firmware encontrou.

> [!DEFINE] Menu de Inicialização
> A lista que o firmware abre com F12 para iniciar um disco ou mídia, só nesta inicialização.

## Se algo der errado

> [!PROBLEM] Nada inicia quando você aperta o botão de energia.
> O computador não tem placa-mãe nem fonte de alimentação, então não tem montagem nenhuma. Adicione-as ([](jsc:first_computer)).

> [!PROBLEM] O interruptor liga, mas o computador não roda.
> As peças não formam uma montagem válida. Veja o que cada recusa significa em ([](jsc:build_refusals)).

> [!PROBLEM] DEL não faz nada.
> DEL funciona só enquanto o autoteste roda. Desligue o computador, ligue de novo e pressione DEL a tempo.

> [!PROBLEM] F12 não faz nada.
> O Menu de Inicialização só abre quando um disco ou mídia pode iniciar o computador ([](jsc:no_bootable_device)).

> [!SEE] jsc:firmware, jsc:setup_utility, jsc:boot_menu
