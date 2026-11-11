---
section: clusters
order: 63
icon: jsc:hbw_interface
covers_family: jsc:hbw_interface
---
# Interface HBW

Um supercomputador não é um bloco só. Ele é formado por todos os Racks de Supercomputador ligados entre si por um cabo de malha, e você o encontra na Interface HBW, o único ponto onde ele se encontra com o resto da rede pelo backbone.

A Standard tem uma, e a Advanced tem outra, que aceita o cabo OSFP.

A interface procura ao longo do cabo de alta computação que sai dela, por até 256 passos de cabo, e conta cada Rack de Supercomputador nesse trecho. Racks em qualquer outro trecho não são contados.

1. Conecte os racks do supercomputador à interface com cabo de alta computação.
2. Conecte a interface ao backbone da rede.

## Se algo der errado

> [!PROBLEM] O supercomputador não é visto.
> Nem todos os seus racks estão no cabo de alta computação que chega à interface. Conecte todo rack a esse mesmo trecho de alta computação.

> [!SEE] jsc:data_cables, jsc:supercomputers
