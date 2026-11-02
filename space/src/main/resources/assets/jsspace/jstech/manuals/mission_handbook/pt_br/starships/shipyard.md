---
section: starships
order: 1
icon: jsspace:shipyard_terminal
covers: [jsspace:shipyard_terminal, jsspace:booth_panel, jsspace:cradle_clamp, jsspace:dock_conveyor, jsspace:dock_feeder, jsspace:dock_floodlight, jsspace:dock_gantry, jsspace:dock_girder, jsspace:dock_rail, jsspace:dock_stairs, jsspace:dock_tower, jsspace:dock_walkway, jsspace:pit_rim]
---
# Estaleiro Pequeno

## O que é

A doca seca onde se constrói uma nave estelar pequena: um poço de dezesseis blocos de largura, vinte e quatro de comprimento e três de profundidade, cercado pela Borda do Poço, com Garras do Berço subindo do fundo. Torres de Treliça, Vigas da Doca, Passarelas da Doca e Trilhos do Pórtico Rolante ladeiam o poço, sob a Ponte do Pórtico Rolante e quatro Holofotes da Doca. A Escada da Doca sobe por um lado. No fundo do outro, uma cabine de Painéis da Cabine de Controle abriga o Terminal do Estaleiro, sobre uma Porta de Itens e a sua Esteira da Doca. Um Alimentador de Blocos fica em cada passarela de baixo.

## Para que serve

Construir uma nave a partir de um projeto, soltá-la para voar e trazê-la de volta para mudá-la ou consertá-la.

## Como conseguir

No menu criativo, aba J's Space. A Porta de Itens fica na mesma aba.

## Como usar

```multiblock
id: jsspace:small_shipyard
caption: O Estaleiro Pequeno, construído
height: 120
```

1. Construa a doca inteira, com o poço escavado e vazio. O Projetor de Estruturas do J's Core mostra a doca bloco por bloco, e o Construtor de Multiblocos usado nesse projetor escava o poço e constrói a doca para você, com o terminal lá em cima na cabine por último.
2. Use o terminal para abrir o Projeto da Nave. Arraste as peças dos cartões até o nó verde de uma junta; o botão direito do mouse gira a vista.
3. Respeite a checagem de voo: uma cabine, uma eclusa, um reator, um motor principal, trem de pouso, uma espinha inteira e uma nave que caiba no vão.
4. Aperte [[S]] para guardar o projeto e [[E]] para mandá-lo para a doca.
5. Construa à mão sobre os fantasmas do holograma, ou alimente a Porta de Itens com um funil ou um cano para os quatro braços robóticos; o Projeto da Nave mostra o que ainda falta trazer. Cada peça se forma no seu modelo quando o último bloco dela entra.
6. Com todas as peças formadas, use o Console de Voo com a mão vazia. A nave sai da doca, sua para [pilotar](jsspace:flying_a_ship).
7. Para mudá-la ou consertá-la, traga a nave a até 32 blocos do terminal de uma doca livre e, como piloto, use o Console de Voo dela. A doca a recebe de volta, reabastece o reator e guarda o projeto, para você mudar e mandar de novo com [[E]].

Numa rede do J's Computers, ligada por um cabo de dados ao lado do terminal, RUN jscore:assemble WITH plan = 'NOME' põe os braços para construir um projeto guardado.

## O que pode dar errado

> [!PROBLEM] O terminal abre uma tela de máquina comum.
> A doca não está inteira: a tela diz o que falta. O poço precisa estar vazio.

> [!PROBLEM] O Console de Voo não dá partida na nave.
> Uma peça ainda não se formou: a etiqueta dela no holograma conta os blocos no lugar.

> [!CAUTION]
> Quebrar o terminal desfaz todas as peças formadas, e os braços largam os blocos que seguram.
