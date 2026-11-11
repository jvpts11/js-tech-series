---
section: autocrafting
order: 148
---
# O Planejador de Fabricação

O Planejador de Fabricação é um programa que mostra uma fabricação antes de você fazê-la. Você escolhe um item do catálogo de fabricação da rede. Em seguida, ele mostra o plano inteiro: cada passo, cada ingrediente e o número de etapas.

A rede calcula o catálogo e o plano, então eles mostram o que a rede tem agora. O botão Fabricar envia o mesmo pedido que o terminal e o Interator de Rede ([](jsc:asking_for_a_craft)).

## Planejando uma fabricação

1. Digite parte do nome do item na caixa de busca.
2. Escolha o item na lista.
3. Leia o plano: os passos, as etapas e os ingredientes, com o que a rede tem.
4. Pressione Fabricar para iniciar a fabricação.

Table: Os textos do Planejador de Fabricação

| O texto | O que significa |
| --- | --- |
| buscar item | A caixa onde você digita para encontrar um item no catálogo. |
| nenhum resultado | A busca não encontra nenhum item com esse nome. |
| Escolha um item para planejar. | Nada foi escolhido ainda, então não há plano. |
| planejando | O plano está sendo calculado. |
| Nenhum padrão na rede fabrica isso. | Nenhuma receita é conhecida para o item, então não há plano. |
| Fabricável | A rede pode fabricar o item com o que tem agora. |
| máx., uma quantidade e o número de etapas | A maior quantidade que a rede pode fabricar agora, e o número de etapas que ela leva. |
| Passos e Árvore | Duas visões do plano: os passos em ordem, e a árvore de fabricação. |
| INGREDIENTES | Os ingredientes brutos, cada um com a quantidade necessária e o que a rede tem. |
| tem 12 | A rede tem 12 desse ingrediente. |
| precisa de 16, tem 15, em vermelho | A rede guarda 15 das 16 unidades que o plano precisa daquele ingrediente. |
| bruto | O ingrediente é uma matéria-prima, sem receita própria. |
| máquina x3, bancada x2 | Quantas vezes uma etapa roda em uma máquina, e quantas vezes roda em uma bancada. |
| Fabricar, depois o nome do item | O botão que inicia a fabricação. |
| Em falta: 1, em vermelho | Falta à rede 1 dos ingredientes, nomeado ao lado. Fabricar fica cinza até a rede guardar todos. |
| Imprimir | Imprime o plano na impressora do computador. |

O planejador trabalha com o que a rede tem agora, então "máx." é o máximo que a rede pode fabricar neste momento. Ele só olha até certa profundidade: uma receita aninhada a mais de 16 níveis não é planejada. Uma receita que depende de si mesma é cortada, para que o plano não entre em laço.

Imprimir manda o plano para a impressora do computador como uma lista de compras: o alvo, os estágios, cada ingrediente com o que você precisa e o que a rede guarda, e o que falta, cada linha em falta numa faixa cinza, com a máquina e a hora em que foi impresso.

## Se algo der errado

> [!PROBLEM] A tela diz "Nenhum padrão na rede fabrica isso."
> Adicione um padrão para o item, como um padrão de bancada, de máquina ou de múltiplas etapas ([](jsc:patterns)).

> [!PROBLEM] Um ingrediente mostra, em vermelho, o que o plano precisa e o que a rede tem.
> Tenha mais desse ingrediente em estoque e planeje a fabricação de novo.

> [!SEE] jsc:asking_for_a_craft, jsc:patterns, jsc:when_a_craft_goes_wrong, jsc:crafting_manager
