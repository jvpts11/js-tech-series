---
section: network_basics
order: 97
icon: jsc:ethernet_cable
covers_family: jsc:data_cables
---
# Cabos de dados

Os cabos de dados são a própria rede. Eles vêm em linhas conforme a função que cumprem, e cada linha tem um cabo para cada era em que existe:

```table
{"caption": "As linhas", "rows": [[{"words": "Acesso"}, {"words": "Computadores pequenos até um roteador"}], [{"words": "Backbone"}, {"words": "Roteadores, o Mainframe e os racks"}], [{"words": "Longa distância"}, {"words": "Duas redes, gateway a gateway"}], [{"words": "Alta computação"}, {"words": "Os nós de um supercomputador até a sua Interface HBW"}], [{"words": "Fabricação"}, {"words": "Um Computador de Fabricação até as peças que alimentam as suas máquinas"}]]}
```

Table: O cabo de cada linha, por era

| Era | Acesso | Backbone | Longa distância | Alta computação |
| --- | --- | --- | --- | --- |
| Vintage | Cabo Coaxial Fino | Cabo Coaxial Grosso | Linha Telefônica | nenhum |
| Legacy | Cabo Ethernet | Cabo HBW | Linha Dedicada | nenhum |
| Transition | Cabo Cat 5e | Cabo 10GBASE-CX4 | Linha T3 | Cabo InfiniBand |
| Standard | Cabo Gigabit Ethernet | Cabo de Fibra Óptica | Cabo VLDC | Cabo de Alta Computação |
| Advanced | Cabo Cat 6a | Cabo de Fibra OM5 | Cabo de Fibra Escura | Cabo OSFP |

A linha de fabricação tem um só cabo para todas as eras, o Cabo de Fabricação. É a única linha que não é dividida por era.

```table
source: jsc:line_reach
caption: Até onde um trecho de cada linha alcança, em cabos
heads: Linha | Vintage | Legacy | Transition | Standard | Advanced
```

```table
source: jsc:line_speed
caption: Quantos itens por tick cada linha move
heads: Linha | Vintage | Legacy | Transition | Standard | Advanced
```

Todo cabo tem uma velocidade e um alcance, e os dois contam. Um trecho mais longo que o seu alcance não transporta nada, então tudo o que fica apenas além dele fica fora da rede. A dica de um cabo diz até onde um trecho dele alcança e o que começa um novo trecho.

Ao montar um trecho, conte-o pelo alcance do seu cabo, e conte os repetidores e roteadores que você colocar nele. Esses são os pontos onde o alcance recomeça.

Duas regras regem duas das linhas. A partir do Standard, a fibra do backbone só corre em linha reta, então ela faz a curva em um roteador óptico ([](jsc:routers)). E a linha de longa distância liga exatamente duas pontas e mantém o seu bloco sozinho.

A fibra também só entra em um Mainframe ou em um Computador de Gerenciamento de Cluster enquanto essa máquina tem uma Placa de Rede Óptica ([](jsc:expansion_cards)), e só entra em um Rack de Servidores por uma Interface de Fibra Óptica montada nele ([](jsc:rack_equipment)).

## Se algo der errado

> [!PROBLEM] A fibra não faz a curva.
> A partir do Standard, a fibra só corre em linha reta. Faça a curva em um roteador óptico.

> [!PROBLEM] Um trecho não transporta nada além do seu alcance.
> O trecho é mais longo que o seu alcance. Adicione um repetidor ou um roteador nele, para que o cabo depois dele volte a contar o alcance ([](jsc:routers)).
