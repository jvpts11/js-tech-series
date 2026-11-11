---
section: assembly
order: 17
---
# Quando a montagem é recusada

Quando uma montagem tem problemas, a tela de montagem de um servidor rack pode dizer quais são. A área de energia tem um rótulo, CHECKS. Passe o mouse sobre ele e cada problema da montagem aparece em uma linha.

Torres e o Mainframe não listam os problemas. A dica de POWER deles dá só as três linhas genéricas de [A tela de montagem](jsc:assembly_screen), então use a tabela abaixo para achar a causa.

Quando nada está errado, a lista diz "Todas as verificações passaram" em verde. Sem placa-mãe ou fonte de energia no lugar, ela diz "Insira uma placa-mãe e uma PSU para começar".

> [!DEFINE] Consumo de energia
> Os watts que as peças pedem no total. Os processadores, a memória, os discos e as placas de expansão somam nesse valor. A energia própria da placa-mãe não entra na conta.

> [!DEFINE] Slot de expansão
> Um slot na placa para uma placa de expansão. Os padrões de barramento são ISA, PCI, AGP (Accelerated Graphics Port) e PCI-e (PCI Express), do mais antigo ao mais novo.

> [!DEFINE] Família de barramento
> A família a que um barramento pertence: ISA, PCI, AGP ou PCI-e. Uma placa só encaixa em uma placa-mãe quando as famílias de barramento combinam, exceto as placas PCI do Workshop e a Placa de Fabricação PCI, que encaixam em qualquer placa-mãe que não seja ISA. ISA também é o nome do barramento mais antigo.

Table: As linhas da lista CHECKS

| A linha | O que significa | A correção |
| --- | --- | --- |
| `no CPU installed` | A montagem não tem processador. | Coloque um processador no soquete que a placa indica. |
| `no RAM installed` | A montagem não tem módulo de memória. | Coloque um módulo de uma geração que a placa aceita, da era dela. |
| `too many CPUs: <installed> installed, <sockets> sockets` | Mais processadores do que soquetes que a placa tem. | Tire processadores, ou use uma placa com mais slots de CPU. |
| `CPU socket <a> does not fit board socket <b>` | O soquete de um processador não é o soquete da placa. | Use um processador com o soquete da placa. |
| `CPU instruction set <a> does not match the <b> of the other processors` | Dois processadores entendem conjuntos de instruções diferentes. | Use processadores de um mesmo conjunto de instruções. |
| `too many PCI-e cards: <installed> installed, <slots> PCI-e slots` | Mais placas de expansão do que slots de expansão na placa. | Tire placas, ou use uma placa com mais slots PCI-e. |
| `expansion card bus family <a> is not compatible with board bus <b>` | A família de barramento de uma placa não é a família de barramento da placa-mãe. | Use uma placa da família de barramento da placa-mãe. |
| `a sound card of the <a> era does not sit on a board of the <b> era` | A placa de som é de outra era que a placa-mãe. | Use uma placa de som da era da placa-mãe. |
| `one sound card per machine: <installed> installed` | O computador tem duas placas de som. | Tire uma delas. |
| `too many RAM modules: <installed> installed, <slots> slots` | Mais módulos de memória do que slots que a placa tem. | Tire módulos, ou use uma placa com mais slots de RAM. |
| `RAM generation <a> not accepted by board` | A geração de um módulo não está na lista da placa. | Use um módulo de uma geração que a placa aceita. |
| `too many disks: <installed> installed, <slots> disk slots` | Mais discos nos slots de disco da placa do que ela tem. | Mova os discos para a Baia Hot-Swap de Disco, ou use uma placa com mais slots de disco. |
| `PSU insufficient: draw <a>W exceeds <b>W` | O consumo de energia é maior do que a fonte de energia fornece. | Use uma fonte de energia com mais watts. Em um servidor rack, o indicador DRAW mostra o total. |

Os números de cada linha mudam para acompanhar a sua montagem.

## Peças que ficam fora do seu slot

Algumas peças nunca entram em um slot. Segure uma sobre o slot, e a janela diz qual regra a deixa de fora, como a tabela abaixo as lista.

Table: Peças que ficam de fora

| Peça | Por que fica de fora |
| --- | --- |
| Uma placa-mãe de outro fator de forma ou era | O gabinete aceita só os fatores de forma dele, e placas-mãe da própria era dele. |
| Um processador de outro soquete ou era, com uma placa-mãe instalada | Ele precisa combinar com o soquete e a era da placa-mãe. |
| Um módulo de memória de outra geração ou era, com uma placa-mãe instalada | Ele precisa combinar com uma geração que a placa aceita, e com a era da placa-mãe. |
| Um disco de uma era mais nova que a do computador | Um disco cabe na era do computador ou em uma mais antiga. |
| Uma peça que não seja uma unidade em uma baia | Uma baia aceita só uma unidade. |
| Uma segunda placa de som | Um computador aceita uma placa de som. |
| Uma segunda Cluster Interface Card em um Computador de Gerenciamento de Cluster | Um Computador de Gerenciamento de Cluster aceita só uma delas. |
| Uma placa Workshop em um computador que não é um Computador Pessoal | Só um Computador Pessoal aceita placas Workshop. |
| Um coprocessador Integra Phi em um servidor rack que não é um nó de supercomputador | Só um nó de supercomputador aceita um, e só um por nó. |

Uma placa que o gabinete recusa deixa o computador sem montagem alguma. O mesmo acontece sem fonte de energia no lugar.

## Se algo der errado

> [!PROBLEM] Uma peça não entra no slot dela.
> Segure-a sobre o slot para ler a regra que ela quebra, e encontre essa regra nas tabelas acima.

> [!PROBLEM] O servidor diz "Insira uma placa-mãe e uma PSU para começar".
> Coloque uma placa-mãe que o gabinete aceite, e uma fonte de energia.
