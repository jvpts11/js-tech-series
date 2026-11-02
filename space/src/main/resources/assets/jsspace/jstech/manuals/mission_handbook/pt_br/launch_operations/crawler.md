---
section: launch_operations
order: 7
icon: jsspace:crawler_engine
covers: [jsspace:crawler_cab, jsspace:crawler_deck, jsspace:crawler_engine, jsspace:crawler_track, jsspace:leveling_jack]
---
# Transportador

## O que é

Um veículo lento de esteiras que leva uma [plataforma móvel](jsspace:launcher) e o seu foguete do hangar até a base. O casco tem quinze blocos de comprimento e onze de largura: o Motor do Transportador no meio e o Convés do Transportador ao redor. Embaixo de cada canto, um caminhão de quatro blocos de comprimento e dois de largura tem Esteiras do Transportador no chão e Macacos de Nivelamento sobre elas. Uma Cabine do Transportador, de dois blocos de lado e dois de altura, fica em dois cantos opostos, fora das pontas do casco.

## Para que serve

O translado. Ele levanta dos pedestais uma plataforma centrada sobre ele, vai até a ponta mais distante do seu caminho a 0,45 bloco por segundo, assenta a plataforma e volta a 0,9. Vazio, ele vai até a outra ponta e fica lá. As Portas do Hangar no caminho se abrem para ele e se fecham quando a viagem termina.

## Como conseguir

No menu criativo, aba J's Space.

## Como usar

```multiblock
id: jsspace:crawler_transporter
caption: O Transportador, construído
height: 100
```

1. Faça o caminho do transportador: uma faixa de cascalho sob as esteiras de cada lado, de até 1.024 blocos, terminando onde o transportador deve parar.
2. Construa o transportador ao longo dela, pelo fantasma de um Projetor de Estruturas, ou use um Construtor de Multiblocos no projetor. O motor o encontra inteiro e o forma.
3. Para um translado, deixe-o embaixo do deck da plataforma, centrado, com o comprimento ao longo do deck.
4. Use uma cabine. O hangar passa o plano do foguete ao [computador de voo](jsspace:flight_computer) dele, e o transportador parte.

## O que pode dar errado

> [!PROBLEM] A cabine diz que o transportador não está inteiro.
> Falta uma esteira, um macaco, uma chapa do convés ou uma cabine ao redor do motor.

> [!PROBLEM] A cabine diz que não há caminho a seguir.
> Ponha cascalho sob cada esteira, ao longo do comprimento do transportador.

> [!PROBLEM] A cabine diz que o caminho está bloqueado num lugar.
> Algo está no caminho do transportador, da plataforma ou do foguete; só as Portas do Hangar se abrem para ele.

> [!PROBLEM] A cabine diz que o foguete não está pronto, ou não tem computador de voo.
> Termine o foguete no hangar, com o seu Computador de Voo.
