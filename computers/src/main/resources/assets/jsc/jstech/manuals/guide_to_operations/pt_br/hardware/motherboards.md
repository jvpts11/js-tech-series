---
section: hardware
order: 18
icon: jsc:motherboard_atx_standard_lga1150
covers_family: jsc:motherboards
---
# Placas-mãe

A placa-mãe é a placa sobre a qual todas as outras peças se encaixam, como o esqueleto de um computador real. Ela decide quais processadores, memórias e placas o computador pode receber, e qual gabinete o aceita.

Passe o mouse sobre uma placa e a dica da ferramenta conta o resto. Ela informa o fator de forma da placa, o soquete, os slots de memória e os tipos de memória que aceita, os slots de expansão e o barramento deles, se ela tem som próprio, se liga uma tela sozinha e a era dela.

> [!DEFINE] Fator de forma
> O fator de forma de uma placa, ou seja, o tamanho e o padrão de fixação dela. Ele decide quais gabinetes aceitam a placa.

```table
{"caption": "As linhas da dica da ferramenta", "heads": ["Linha", "O que ela informa"], "rows": [[{"literal": "<form factor> form factor"}, {"words": "O tamanho da placa, que decide qual gabinete a aceita."}], [{"literal": "<n>x <socket>  |  <n> RAM (<kinds>)  |  <n>x <bus>"}, {"words": "Os slots de processador e o soquete deles. Os slots de memória e os tipos de memória que ela aceita. Os slots de expansão e o barramento deles."}], [{"literal": "On-board audio"}, {"words": "Placas a partir da Transition já têm som próprio."}], [{"literal": "Seats parts of its own era only"}, {"words": "A placa aceita apenas peças da própria era."}], [{"literal": "Has a screen output of its own, for its console"}, {"words": "Uma placa de servidor liga um monitor sozinha, então um servidor montado nela não precisa de placa de vídeo e deixa os slots de expansão para outras placas. Todas as outras placas dizem que não têm: um monitor nelas precisa de uma placa de vídeo, ou de um processador com gráficos."}], [{"literal": "Firmware: <kind>"}, {"words": "O firmware na placa, o da era dela: uma BIOS em texto, uma BIOS azul ou uma UEFI ([](jsc:firmware))."}], [{"literal": "No firmware on this board"}, {"words": "Uma placa ainda sem nada gravado. Um computador montado nela gira as ventoinhas e nada mais, até uma Pré-Instaladora gravar o firmware dela ([](jsc:computer_making_machines))."}]]}
```

Cada computador aceita apenas algumas placas. A tabela mostra quais, e a era da placa também deve coincidir com a era do computador.

Table: As placas que cada computador aceita

| Computador | Era | Placas que aceita |
| --- | --- | --- |
| Computador Pessoal | Vintage | Baby-AT e AT |
| Computador Pessoal | Legacy | Somente ATX |
| Computador Pessoal | Transition e posteriores | ATX e EATX |
| Computador de Fabricação | Vintage | Baby-AT e AT |
| Computador de Fabricação | Legacy e posteriores | Somente ATX |
| Computador de Gerenciamento de Cluster | Vintage | Baby-AT e AT |
| Computador de Gerenciamento de Cluster | Legacy e posteriores | ATX e EATX |

O Mainframe aceita apenas placas MTX. Uma placa de outra era é recusada no slot, sem aviso. Uma placa de outro fator de forma também é recusada da mesma maneira.

Uma placa mais barata pode ter menos slots de memória ou de expansão do que o gabinete tem espaço para.

Os slots de expansão funcionam em um barramento. Os barramentos, do mais antigo para o mais novo, são ISA, PCI, AGP 2x, AGP 4x, AGP 8x, e PCI-e 1.0, 2.0, 3.0, 4.0, 5.0 e 6.0. Uma placa de expansão precisa de um barramento da família da placa-mãe ([a família de barramentos](jsc:build_refusals)).

Algumas placas de servidor também aceitam as placas de um barramento mais antigo: a placa de servidor Vintage aceita placas ISA ao lado das PCI, e as placas de servidor Legacy aceitam placas PCI ao lado das PCI-e. A dica da placa diz qual é o barramento mais antigo.

## Se algo der errado

> [!PROBLEM] Ela não cabe no gabinete.
> O gabinete aceita apenas alguns fatores de forma, e apenas placas da própria era. Confira a tabela acima.

> [!PROBLEM] Uma peça fica na janela, mas não faz nada.
> A placa não oferece esse slot. Use uma placa com mais slots desse tipo.

> [!PROBLEM] O computador liga, as ventoinhas giram, e nada mais acontece.
> A placa dele não tem firmware: a janela diz SEM FIRMWARE. Uma Pré-Instaladora o grava ([](jsc:computer_making_machines)).

> [!PROBLEM] Uma placa não entra num servidor.
> A janela mostra só os slots de expansão que a placa e o gabinete do servidor ligam, e um servidor pequeno tem só um ou dois, dos quais uma placa de vídeo ocupa um. Monte o servidor numa placa de servidor, que tem saída de vídeo própria, e os slots ficam livres para outras placas.

> [!SEE] jsc:eras, jsc:processors, jsc:memory
