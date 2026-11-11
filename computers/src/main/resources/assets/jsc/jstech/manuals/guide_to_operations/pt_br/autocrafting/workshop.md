---
section: autocrafting
order: 150
icon: jsc:crafting_table_card
---
# O Workshop e as placas de uso pessoal

Um Computador Pessoal pode fazer mais do que rodar programas. O programa Workshop dele trabalha com quatro placas de uso pessoal. Você coloca uma placa no computador, e o programa a usa para um tipo de trabalho: a Placa de Bancada de Trabalho, a Placa de Fornalha, a Placa de Encantamento ou a Placa de Bigorna ([](jsc:expansion_cards)).

O Workshop tem quatro abas: Crafting, Furnace, Enchanting e Anvil. Uma aba cuja placa não está no computador fica apagada, e diz "sem placa".

## Usando uma placa

1. Coloque a placa no Computador Pessoal.
2. Abra o Workshop e escolha a aba dessa placa.

Table: As placas de uso pessoal

| Placa | No Workshop | Ação de UPDATE |
| --- | --- | --- |
| Placa de Bancada de Trabalho | Fabrica uma grade 3x3 com as receitas de fabricação do jogo. | Nenhum: uma grade preenchida pela rede é para isso que serve a fabricação. |
| Placa de Fornalha | Funde com as receitas de fundição do jogo, e sem combustível. | SMELT: funde um item guardado na rede. |
| Placa de Encantamento | Oferece as três ofertas de uma mesa de encantamento, contando até 15 estantes de livros ao redor dela. | ENCHANT: encanta um item. |
| Placa de Bigorna | Conserta, combina e renomeia com a lógica de bigorna do jogo. | REPAIR, COMBINE e NAME. |

## Como as placas funcionam

Table: A Placa de Fornalha por era

| Era | Velocidade |
| --- | --- |
| Vintage | não funde |
| Legacy | 2 |
| Transition | 3 |
| Standard | 4 |
| Advanced | 6 |

A velocidade cresce com a era do computador. Todos os números aqui são uma primeira estimativa, e podem mudar.

O trabalho das placas custa energia ao computador, e nada de lápis-lazúli: fundir, 2 kJ por item; encantar, 10 kJ por nível que a mesa de encantamento pediria; reparar, 2 kJ por ponto percentual de durabilidade devolvido; combinar, 10 kJ por nível que a bigorna pediria; e nomear, 5 kJ. O computador fica devendo essa energia e a paga à medida que a rede elétrica enche o seu estoque, e o próximo trabalho pago das placas espera até que ele pague. Um servidor pode fazer o trabalho custar níveis no lugar (metade do que o próprio bloco do jogo pede, no mínimo um), os dois, ou nada. Uma oferta mostra o nível de que precisa e o que custa, e uma oferta que não pode ser feita diz "sem oferta".

O conserto pega um material do armazenamento. Cada peça de material repara um quarto da durabilidade do item, e são usadas até 4 peças.

## UPDATE pelas placas

Um UPDATE altera um item que a rede guarda. Ele roda em três etapas. Primeiro, o item sai dos servidores de armazenamento para a gaveta da placa, como um SELECT o retira. Depois a placa faz o seu trabalho. Por fim, o resultado volta para o servidor do próprio item, e o que não couber lá fica na saída da placa.

Uma fundição de mais de uma pilha roda as três etapas de novo, uma pilha de cada vez. Uma etapa que não pode ser feita devolve o item como estava.

Um UPDATE pertence ao Computador Pessoal que o pediu. Se esse computador parar, ou perder a sua placa, o UPDATE é descartado, e o que a placa guardava fica na saída dela. A gaveta não sobrevive ao mundo ser fechado.

## Se algo der errado

> [!PROBLEM] A tela diz "só as placas do próprio Personal Computer mudam itens para a rede".
> Peça a partir de um Computador Pessoal que tenha a placa.

> [!PROBLEM] A tela diz "este computador não tem", seguido do nome da placa.
> Coloque essa placa no Computador Pessoal.

> [!PROBLEM] A tela diz "alguém precisa pedir: a placa faz o trabalho para essa pessoa, como ela faria no bloco".
> Peça por meio de um jogador: a placa encanta e usa a bigorna como esse jogador faria.

> [!PROBLEM] A tela mostra o nome do item, depois diz "não funde".
> A Placa de Fornalha não funde em um computador Vintage. Use um Computador Pessoal da era Legacy ou posterior.

> [!PROBLEM] A tela mostra o nome do item, depois diz "não aceita encantamento".
> Encante um item que aceite encantamentos.

> [!PROBLEM] A tela mostra o nome do item, depois diz "não está equipado".
> Repare apenas um item que tenha perdido durabilidade.

> [!PROBLEM] A tela diz "a rede não guarda nada que conserte", seguido do nome do item.
> Coloque um material que conserte o item no armazenamento da rede.

> [!PROBLEM] A tela diz "a placa não conseguiu fazer isso com", seguido do nome do item, e depois o motivo: nada para mudar, caro demais ou níveis insuficientes.
> Confira o item, e dê ao jogador que pede níveis suficientes.

> [!PROBLEM] A tela diz "a rede não estava guardando", seguido do nome do item.
> Coloque o item no armazenamento da rede, e peça de novo.

> [!PROBLEM] A tela diz "quem pediu não estava aqui".
> Peça de novo enquanto o jogador que pediu estiver no mundo.

> [!PROBLEM] A grade não faz nada.
> Preencha a grade com uma receita que o jogo conheça.

> [!PROBLEM] A tela diz "Níveis insuficientes para essa oferta".
> Dê ao jogador que pede mais níveis, ou escolha uma oferta mais barata.

> [!PROBLEM] A Workshop diz "O computador ainda está puxando a energia do último trabalho".
> Espere enquanto a rede elétrica enche o estoque do computador, ou leve mais energia até ele.

> [!SEE] jsc:expansion_cards, jsc:operations_catalogue, jsc:personal_computers, jsc:when_a_craft_goes_wrong
