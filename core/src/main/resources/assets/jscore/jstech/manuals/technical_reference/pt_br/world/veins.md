---
section: world
order: 20
icon: minecraft:raw_iron
---
# Veios

## O que é

Os minérios não ficam espalhados pela rocha: ficam em veios. Um veio é um corpo de minério denso no meio e que vai rareando até sumir na borda, então um lugar não tem nada e o seguinte tem centenas de blocos de minério.

Entre os veios um pouco de cada minério fica solto, em pequenos nós de um punhado de blocos, como os minérios do próprio jogo ficam espalhados: cavar em qualquer lugar encontra um pouco de cada tipo nas suas alturas, o bastante para começar, nunca o bastante para viver disso.

Sobre um veio, alguns pedaços do minério dele ficam no chão, como pedras soltas: de dois a quatro pedacinhos do próprio minério do veio juntos, umas oito dessas manchas sobre cada veio, em chão firme e nunca na água. Quebrados com a mão, dão um do minério, ferro bruto no caso do ferro.

> [!DEFINE] Zona
> Um quadrado do mundo de três chunks de lado. Uma zona tem no máximo um veio, e a maioria não tem nenhum.

## Para que serve

Saber onde cavar. Um veio de ferro perto da superfície alimenta uma fornalha por muito tempo; um veio de ouro lá embaixo vale um túnel comprido.

## Como conseguir

Todo mundo tem veios, e eles ficam onde a semente do mundo diz, iguais em todo servidor com essa semente. Estes são os veios da série; um modpack ou um mod pode mudá-los e acrescentar outros.

Table: Os veios da série

| Veio | O meio fica | Onde se encontra | De largura, de altura |
| --- | --- | --- | --- |
| Ferro | de y -48 a 96 | no Overworld, cerca de uma zona em seis | 20 blocos, 6 |
| Cobre | de y -16 a 112 | no Overworld, cerca de uma zona em oito | 22 blocos, 7 |
| Ouro | de y -64 a 32 | no Overworld, cerca de uma zona em vinte | 16 blocos, 5 |
| Ouro do Nether | de y 10 a 117 | no Nether, cerca de uma zona em cinco | 18 blocos, 6 |
| Estanho | de y -16 a 80 | no Overworld, cerca de uma zona em doze, quando um mod usa estanho | 18 blocos, 6 |

## Como usar

1. Andando pelo terreno, procure pedaços de minério no chão: um veio desse minério está embaixo deles, em algum ponto mais fundo da coluna e ao redor, e onde há vários o meio dele está perto.
2. Olhe onde o minério aparece na parede de uma caverna ou num penhasco: um veio é bem maior do que o que aparece, então cave em volta.
3. Siga-o para o lado em que o minério fica mais grosso: para lá está o meio.
4. Siga-o de uma rocha a outra: o mesmo veio põe minério de ferro na pedra e minério de ferro de ardósia profunda na ardósia profunda.
5. Com o instrumento de prospecção de um mod, saiba para que lado ficam os veios mais próximos e com que força cada um aparece; de pé dentro de um, o que ele tem e mais ou menos quanto.
6. Quando um veio se esgota, procure alguns chunks adiante: o próximo está a pelo menos uma zona de distância.

## O que pode dar errado

> [!PROBLEM] Nenhum minério em lugar nenhum, por chunks a fio.
> A maioria das zonas não tem veio, e entre elas só ficam pequenos nós. Siga alguns chunks, ou procure mais fundo ou mais alto: cada tipo fica entre as suas alturas. Um servidor escolhe quanto minério fica solto em `jstech-balance.toml` (`veins.loose`, 0 para nenhum).

> [!PROBLEM] O ferro do jogo sumiu de onde costumava ficar.
> Enquanto os minérios ficam em veios, o ferro, o cobre e o ouro espalhados do jogo não são postos. Um servidor desliga os veios em `jstech-balance.toml` (`veins.enabled`); só os chunks feitos depois mudam.

> [!PROBLEM] Um veio tem o seu minério numa rocha e não em outra.
> Um minério só fica numa rocha em que algum mod o tem: um bloco de um veio cujo material não tem minério nessa rocha fica como estava.
