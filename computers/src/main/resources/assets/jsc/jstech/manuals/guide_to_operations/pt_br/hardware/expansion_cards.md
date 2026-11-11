---
section: hardware
order: 28
icon: jsc:furnace_card
covers_family: jsc:expansion_cards
---
# Placas de expansão

Além de gráficos e som, os slots de expansão de uma placa-mãe aceitam placas que dão a um computador um trabalho que ele não podia fazer antes. Cada placa entra em um slot da sua própria família de barramento ([família de barramento](jsc:build_refusals)).

## Placas do Workshop

A Placa de Bancada de Trabalho, a Placa de Fornalha, a Placa de Encantamento e a Placa de Bigorna dão, cada uma, um trabalho a um Computador Pessoal no programa Workshop. Só um Computador Pessoal as aceita, e cada uma consome 15 W.

O Workshop mostra "Sem combustível" quando a fundição não tem combustível, e "Sem lápis-lazúli" quando o encantamento não tem lápis-lazúli.

## Placas de Interface de Cluster

Quatro placas permitem que um Computador de Gerenciamento de Cluster alcance as máquinas dos seus racks: a Placa de Console Serial, a NIC de Gerenciamento, o Adaptador de Host Fabric e a Fabric DPU. Cada uma mais nova alcança mais. Um Computador de Gerenciamento de Cluster aceita uma delas ([Computadores de Gerenciamento de Cluster](jsc:cluster_management_computers)).

Table: As Placas de Interface de Cluster

| Placa | Era e barramento | Alcance | Pistas |
| --- | --- | --- | --- |
| Placa de Console Serial | Vintage, PCI | Servidores de rack em datacenters | 1 nó por vez |
| NIC de Gerenciamento | Legacy, PCI-e 1.0 | Datacenters e fabrics de supercomputador | 2 nós por vez |
| Adaptador de Host Fabric | Standard, PCI-e 3.0 | Tudo, todo tipo de rack | 4 nós por vez |
| Fabric DPU | Advanced, PCI-e 4.0 | Tudo, todo tipo de rack | 8 nós por vez |

> [!DEFINE] Alcance
> Os tipos de máquina que uma placa pode alcançar, desde os servidores de rack de um Datacenter até todos os tipos de rack.

> [!DEFINE] Faixa
> O número de nós que uma instalação em lote grava de uma vez. Em um slot mais antigo, as lanes caem, mas nunca abaixo de uma.

Só um Computador de Gerenciamento de Cluster usa essas placas.

## Placa de Rede Óptica

Uma placa de rede com uma porta de fibra. Um Mainframe ou um Computador de Gerenciamento de Cluster com uma dessas mostra a porta de fibra como conectada, então pode entrar no backbone de fibra da rede ([Cabos de dados](jsc:data_cables)). Um servidor nunca a aceita: um rack entra na fibra por uma Interface de Fibra Óptica ([](jsc:rack_equipment)). É uma placa Standard, em PCI-e 3.0, e consome 15 W.

## Coprocessadores Integra Phi

Quatro coprocessadores Integra Phi somam poder de computação aos nós de um supercomputador. Só um nó de supercomputador aceita um, e só um por nó. Um servidor de rack de outro tipo recusa ([Racks de servidores e servidores](jsc:server_racks)).

Table: Os coprocessadores Integra Phi

| Coprocessador | Núcleos e clock | Potência | Slots de supercomputador |
| --- | --- | --- | --- |
| Coprocessador Integra Phi 5100 | 60 núcleos a 1,05 GHz | 225 W | 1 a 2 |
| Coprocessador Integra Phi 7120 | 61 núcleos a 1,24 GHz | 250 W | 1 a 3 |
| Coprocessador Integra Phi 7290 | 72 núcleos a 1,50 GHz | 270 W | 1 a 4 |
| Coprocessador Integra Phi 9000 | 96 núcleos a 1,80 GHz | 300 W | 1 a 6 |

A última coluna é a faixa de slots de supercomputador que a dica informa, como em "Slots de supercomputador 1-2".

## Placas de Fabricação

As Placas de Fabricação dão ao Computador de Fabricação a sua fabricação. Há seis, da Vintage à Advanced, e a autofabricação as explica ([Placas de Fabricação](jsc:crafting_cards)).

```table
{"caption": "As Placas de Fabricação", "heads": ["Era", "Barramento", "Potência"], "rows": [[{"words": "Vintage"}, {"words": "ISA"}, {"amount": 10, "unit": "W"}], [{"words": "Legacy"}, {"words": "PCI"}, {"amount": 25, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 1.0"}, {"amount": 75, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 2.0"}, {"amount": 100, "unit": "W"}], [{"words": "Standard"}, {"words": "PCI-e 3.0"}, {"amount": 110, "unit": "W"}], [{"words": "Advanced"}, {"words": "PCI-e 4.0"}, {"amount": 125, "unit": "W"}]]}
```

## Placas de armazenamento

A Vaultis faz uma controladora RAID e um cache de leitura para o slot de cada era. Num servidor, eles juntam os discos dele num único volume e respondem às leituras mais rápido; em outro computador, ficam no slot e não fazem nada ([modos RAID](jsc:raid_arrays)).

## Placas de Ancoragem de Rede

Uma Placa de Ancoragem de Rede mantém carregados os chunks ao redor do seu computador enquanto ele roda, para que as máquinas ali continuem trabalhando quando você está longe. Qualquer computador aceita uma, e um servidor num rack também: o rack então mantém os chunks ao redor dele pela placa de maior alcance dos seus servidores ligados.

```table
{"caption": "As Placas de Ancoragem de Rede", "heads": ["Era", "Slot", "Mantém carregado", "Consumo"], "rows": [[{"words": "Vintage"}, {"words": "ISA"}, {"words": "O chunk do computador"}, {"amount": 5, "unit": "W"}], [{"words": "Legacy"}, {"words": "PCI"}, {"words": "O chunk do computador"}, {"amount": 8, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 2.0"}, {"words": "3 por 3 chunks"}, {"amount": 10, "unit": "W"}], [{"words": "Standard"}, {"words": "PCI-e 3.0"}, {"words": "3 por 3 chunks"}, {"amount": 12, "unit": "W"}], [{"words": "Advanced"}, {"words": "PCI-e 4.0"}, {"words": "5 por 5 chunks"}, {"amount": 15, "unit": "W"}]]}
```

Os chunks contam na cota de quem opera o computador: o último jogador a colocá-lo ou usá-lo. O servidor define essa cota, 25 chunks se não disser outra coisa, e a dica da placa mostra quanto da sua você já usa. Quando a cota está cheia a placa mantém o que cabe, do mais perto para o mais longe, e o log da rede avisa.

## Placas de Interface WAN

Uma Placa de Interface WAN opera uma porta de um Computador Gateway WAN, e só um gateway aceita uma ([Computadores Gateway WAN](jsc:wan_gateways)). Há uma para a linha de longa distância de cada era, no slot dessa era. Cada uma aceita a linha da sua própria era e as mais antigas, e um gateway aceita quatro, uma para cada uma das suas portas.

```table
{"caption": "As Placas de Interface WAN", "heads": ["Placa", "Era e barramento", "Linha", "Velocidade"], "rows": [[{"words": "Placa de Modem"}, {"words": "Vintage, ISA"}, {"words": "Linha Telefônica"}, {"amount": 1, "unit": "it/t"}], [{"words": "Placa T1"}, {"words": "Legacy, PCI"}, {"words": "Linha Dedicada"}, {"amount": 8, "unit": "it/t"}], [{"words": "Placa T3"}, {"words": "Transition, PCI-e 2.0"}, {"words": "Linha T3"}, {"amount": 24, "unit": "it/t"}], [{"words": "Placa VLDC"}, {"words": "Standard, PCI-e 3.0"}, {"words": "VLDC"}, {"amount": 64, "unit": "it/t"}], [{"words": "Placa de Transporte Óptico"}, {"words": "Advanced, PCI-e 4.0"}, {"words": "Fibra Escura"}, {"amount": 256, "unit": "it/t"}]]}
```

## Se algo der errado

> [!PROBLEM] O Workshop mostra "Sem combustível" ou "Sem lápis-lazúli".
> Dê combustível à fundição, ou dê lápis-lazúli ao encantamento.

> [!PROBLEM] Uma placa do Workshop não entra em um computador.
> Só um Computador Pessoal aceita placas do Workshop. Coloque-as em um.

> [!PROBLEM] Uma segunda Placa de Interface de Cluster não entra.
> Um Computador de Gerenciamento de Cluster aceita só uma. Retire a outra.

> [!PROBLEM] Uma Placa de Interface de Cluster não faz nada.
> Só um Computador de Gerenciamento de Cluster usa isto. Coloque-o em um.

> [!PROBLEM] A compilação diz que há placas PCI-e demais.
> Retire placas, ou use uma placa-mãe com mais slots PCI-e.

> [!PROBLEM] As máquinas ao redor de um computador com uma Placa de Ancoragem de Rede param quando você vai embora.
> A placa só funciona enquanto o computador roda e alguém o opera. Se o log da rede diz que a cota está cheia, ela mantém só o que cabe: libere chunks que você mantém em outro lugar.

> [!PROBLEM] Uma Placa de Interface WAN não entra num computador.
> Só um Computador Gateway WAN aceita uma, quatro no máximo.

> [!SEE] jsc:graphics_cards, jsc:sound_cards, jsc:build_refusals
