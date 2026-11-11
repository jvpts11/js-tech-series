---
section: hardware
order: 20
icon: jsc:ram_simm_4
covers_family: jsc:memory
---
# Memória

A memória, ou RAM, é onde o computador guarda o que usa neste momento: o sistema, os programas abertos, o arquivo que você está editando. A RAM é rápida e esquece tudo quando o computador desliga, por isso o disco também existe.

O Gerenciador de Tarefas mostra quanta memória está em uso, na aba Desempenho.

> [!DEFINE] Geração de memória
> O padrão que um módulo de memória segue, como SIMM, DDR3 ou DDR4.

A memória vem em gerações: SIMM, EDO e SDRAM na Vintage; SDRAM, DDR e DDR2 na Legacy; depois DDR2, DDR3, DDR4 e DDR5 nas eras posteriores.

Uma placa aceita apenas algumas gerações, e a dica que aparece ao passar o mouse as lista. Um módulo também precisa combinar com a era da placa. Um módulo de outra geração ou era é recusado no slot.

Os módulos Vintage e Legacy começam com as palavras Stratix Layer, como em Stratix Layer SIMM-4. A tabela dá o resto de cada nome.

```table
{"caption": "Os módulos de memória", "heads": ["Módulo", "Era e geração", "Buffer de RAM", "Energia"], "rows": [[{"words": "SIMM-4"}, {"words": "Vintage, SIMM"}, {"words": "1 item"}, {"amount": 1, "unit": "W"}], [{"words": "SIMM-16"}, {"words": "Vintage, SIMM"}, {"words": "4 itens"}, {"amount": 2, "unit": "W"}], [{"words": "EDO-16"}, {"words": "Vintage, EDO"}, {"words": "4 itens"}, {"amount": 2, "unit": "W"}], [{"words": "EDO-32"}, {"words": "Vintage, EDO"}, {"words": "8 itens"}, {"amount": 3, "unit": "W"}], [{"words": "EDO-64"}, {"words": "Vintage, EDO"}, {"words": "16 itens"}, {"amount": 4, "unit": "W"}], [{"words": "SDRAM-32"}, {"words": "Vintage, SDRAM"}, {"words": "8 itens"}, {"amount": 3, "unit": "W"}], [{"words": "SDRAM-64"}, {"words": "Vintage, SDRAM"}, {"words": "16 itens"}, {"amount": 4, "unit": "W"}], [{"words": "SDRAM-128"}, {"words": "Legacy, SDRAM"}, {"words": "32 itens"}, {"amount": 5, "unit": "W"}], [{"words": "SDRAM-256"}, {"words": "Legacy, SDRAM"}, {"words": "64 itens"}, {"amount": 6, "unit": "W"}], [{"words": "DDR-256"}, {"words": "Legacy, DDR"}, {"words": "64 itens"}, {"amount": 7, "unit": "W"}], [{"words": "DDR-512"}, {"words": "Legacy, DDR"}, {"words": "128 itens"}, {"amount": 10, "unit": "W"}], [{"words": "DDR-1024"}, {"words": "Legacy, DDR"}, {"words": "256 itens"}, {"amount": 12, "unit": "W"}], [{"words": "DDR2-512"}, {"words": "Legacy, DDR2"}, {"words": "128 itens"}, {"amount": 9, "unit": "W"}], [{"words": "DDR2-1024"}, {"words": "Transition, DDR2"}, {"words": "256 itens"}, {"amount": 10, "unit": "W"}], [{"words": "DDR2-2048"}, {"words": "Transition, DDR2"}, {"words": "512 itens"}, {"amount": 12, "unit": "W"}], [{"words": "DDR2 4096 RDIMM"}, {"words": "Transition, DDR2"}, {"words": "1.024 itens"}, {"amount": 15, "unit": "W"}], [{"words": "DDR3-1024"}, {"words": "Transition, DDR3"}, {"words": "256 itens"}, {"amount": 8, "unit": "W"}], [{"words": "DDR3-2048"}, {"words": "Transition, DDR3"}, {"words": "512 itens"}, {"amount": 10, "unit": "W"}], [{"words": "DDR3-4096"}, {"words": "Transition, DDR3"}, {"words": "1.024 itens"}, {"amount": 12, "unit": "W"}], [{"words": "DDR3-8192"}, {"words": "Standard, DDR3"}, {"words": "2.048 itens"}, {"amount": 15, "unit": "W"}], [{"words": "DDR3L-4096"}, {"words": "Standard, DDR3"}, {"words": "1.024 itens"}, {"amount": 10, "unit": "W"}], [{"words": "DDR3 16384 RDIMM"}, {"words": "Standard, DDR3"}, {"words": "4.096 itens"}, {"amount": 18, "unit": "W"}], [{"words": "DDR4 8192"}, {"words": "Advanced, DDR4"}, {"words": "2.048 itens"}, {"amount": 12, "unit": "W"}], [{"words": "DDR4 16384"}, {"words": "Advanced, DDR4"}, {"words": "4.096 itens"}, {"amount": 13, "unit": "W"}], [{"words": "DDR4 32768"}, {"words": "Advanced, DDR4"}, {"words": "8.192 itens"}, {"amount": 15, "unit": "W"}], [{"words": "DDR4 65536 RDIMM"}, {"words": "Advanced, DDR4"}, {"words": "16.384 itens"}, {"amount": 18, "unit": "W"}], [{"words": "DDR5 16384"}, {"words": "Advanced, DDR5"}, {"words": "4.096 itens"}, {"amount": 12, "unit": "W"}], [{"words": "DDR5 32768"}, {"words": "Advanced, DDR5"}, {"words": "8.192 itens"}, {"amount": 14, "unit": "W"}], [{"words": "DDR5 65536 RDIMM"}, {"words": "Advanced, DDR5"}, {"words": "16.384 itens"}, {"amount": 18, "unit": "W"}], [{"words": "DDR5 131072 RDIMM"}, {"words": "Advanced, DDR5"}, {"words": "32.768 itens"}, {"amount": 22, "unit": "W"}]]}
```

Em um Mainframe, o buffer de RAM tem mais uma função: os itens esperam nele a caminho de entrar ou sair da rede ([Mainframes](jsc:mainframes)).

## Se algo der errado

> [!PROBLEM] O módulo é recusado.
> É de uma geração que a placa-mãe não aceita, ou de outra era. Use um módulo que a placa liste, da sua própria era.

> [!PROBLEM] A montagem diz que a geração de RAM não é aceita pela placa-mãe.
> Use um módulo de uma geração que a placa liste.

> [!PROBLEM] A montagem diz que não há RAM.
> Coloque um módulo de um tipo que a placa aceite.

> [!SEE] jsc:processors, jsc:motherboards, jsc:mainframes, jsc:swap
