---
section: autocrafting
order: 141
icon: jsc:crafting_card_pcie3
covers_family: jsc:crafting_cards
---
# Placas de Fabricação

A Placa de Fabricação é a peça que permite a um Computador de Fabricação fabricar. Você a coloca em um slot de expansão do computador. Sem placa, o computador não fabrica nada ([](jsc:crafting_computers)).

Uma placa define o quanto o computador pode fazer. Ela comanda certo número de Interfaces de Fabricação e guarda certo número de receitas de bancada na própria memória. As receitas ficam na placa, então vão com ela para outro computador.

> [!DEFINE] ROM
> A memória de uma Placa de Fabricação, onde ficam guardadas as receitas de bancada dela. Ela faz parte da placa, então as receitas vão junto com ela.

Table: O que cada placa dá, por era

| Era | Interfaces que ela comanda | Receitas que a ROM guarda |
| --- | --- | --- |
| Vintage | 2 | 2 |
| Legacy | 4 | 4 |
| Transition | 5 | 5 |
| Standard | 6 | 6 |
| Advanced | 8 | 8 |

Os dois números são limites separados. Hoje eles são iguais em todas as eras, mas medem coisas diferentes: as interfaces que a placa comanda, e as receitas que ela guarda.

Um computador com várias placas soma os valores: as interfaces que ele comanda e as receitas que ele guarda são os totais de todas as suas placas. Um computador desligado não comanda interface alguma.

A placa também define a velocidade. Cada placa soma a sua vazão, e a velocidade de fabricação do computador é a capacidade do processador (CPU) multiplicada pela soma dessas vazões.

Cada placa também soma as suas threads. As threads de um computador são a soma das threads das suas placas: o número de etapas que ele roda em paralelo para uma fabricação.

> [!DEFINE] etapa
> Uma etapa de um pipeline: um padrão de bancada ou um padrão de máquina.

Table: As Placas de Fabricação

| Placa | Era | Vazão | Threads |
| --- | --- | --- | --- |
| Placa de Fabricação Forge Logic ISA | Vintage | 0,02x CPU | 1 |
| Placa de Fabricação Forge Logic PCI | Legacy | 0,03x CPU | 2 |
| Placa de Fabricação Forge Logic | Transition | 0,05x CPU | 2 |
| Placa de Fabricação Forge Logic T3 | Transition | 0,10x CPU | 4 |
| Placa de Fabricação Forge Logic PCI-e 3.0 | Standard | 0,15x CPU | 6 |
| Placa de Fabricação Forge Logic T4 | Advanced | 0,20x CPU | 8 |

Uma placa vai num slot do seu próprio barramento. A placa PCI também vai numa placa-mãe AGP ou PCI-e, como as placas-mãe da sua época tinham slots PCI ao lado do slot de vídeo: as placas-mãe Legacy são AGP ou PCI-e.

Passe o mouse sobre uma placa para ler a sua dica. Cada linha da dica informa uma coisa.

```table
{"caption": "A dica de uma placa", "heads": ["Linha da dica", "O que ela informa"], "rows": [[{"words": "Acelerador de fabricação (FPGA)"}, {"words": "O tipo de placa: um acelerador de fabricação, construído sobre um arranjo de portas programáveis em campo (FPGA)."}], [{"words": "Threads  -  N threads"}, {"words": "Quantas etapas a placa roda ao mesmo tempo."}], [{"words": "Vazão  -  Nx CPU"}, {"words": "A velocidade da placa como múltiplo da capacidade do processador (CPU) do computador."}], [{"words": "Comanda N Interfaces de Fabricação"}, {"words": "Quantas interfaces a placa comanda."}], [{"words": "ROM  -  N de M receitas de bancada"}, {"words": "Quantas receitas de bancada a placa guarda, de quantas ela pode guardar."}], [{"words": "Linhas de receita, depois \"e mais N\""}, {"words": "Os nomes de até 8 receitas de bancada, depois quantas ficaram de fora."}]]}
```

Você carrega uma receita de bancada com o botão Carregar do Pattern Studio ou no Gerenciador de Fabricação ([](jsc:crafting_manager)). Ela vai para a primeira placa com espaço, contando a partir do primeiro slot, e uma placa que já guarda a mesma receita não a recebe duas vezes.

## Se algo der errado

> [!PROBLEM] O Computador de Fabricação não fabrica nada.
> Ele não tem uma Placa de Fabricação, ou ela está desligada. Coloque uma placa nele e ligue-a.

> [!PROBLEM] Uma receita de bancada não está mais no computador.
> A placa que a guardava foi para outro computador junto com a receita. Pegue a placa de volta, ou carregue a receita de novo.

> [!PROBLEM] A tela diz "Somente um Computador de Fabricação carrega receitas".
> Carregue a receita em um Computador de Fabricação.

> [!PROBLEM] A tela diz "É necessária uma Placa de Fabricação".
> Coloque uma placa no Computador de Fabricação primeiro.

> [!PROBLEM] A tela diz "Não carregado: uma placa já o guarda, ou a ROM de todas as placas está cheia".
> Uma placa já guarda a receita, então nada é necessário. Se todas as ROMs estiverem cheias, adicione uma placa, ou remova uma receita no Gerenciador de Fabricação.

> [!SEE] jsc:crafting_computers, jsc:crafting_manager, jsc:crafting_interfaces
