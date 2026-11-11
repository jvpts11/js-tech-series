---
section: autocrafting
order: 149
icon: jsc:crafting_computer
---
# Pedindo uma fabricação

Você pede uma fabricação como pede qualquer outra coisa: no Interator de Rede, ou em IQL, a linguagem de consulta da rede, com CRAFT 512 piston ([](jsc:iql)).

A rede planeja a fabricação de baixo para cima: o que ela tem, o que precisa fabricar primeiro, em que ordem e em quais máquinas. Depois executa os passos. Os passos de bancada rodam no Computador de Fabricação, com quantas etapas ao mesmo tempo as suas threads permitirem. Os passos de máquina rodam nas Interfaces de Fabricação, cada um na sua própria máquina ([](jsc:crafting_computers)).

Antes de pedir, o Planejador de Fabricação mostra o mesmo plano, com o que ele precisa e o que está faltando ([](jsc:craft_planner)). Quando você abre uma fabricação no catálogo do Interator de Rede, o pop-up pede o plano de imediato.

## No Interator de Rede

Table: As teclas do pop-up de fabricação

| Tecla | O que faz |
| --- | --- |
| `Enter` | Envia a fabricação completa. |
| `Backspace` | Divide a quantidade por 10. |
| `Escape` | Fecha o pop-up. |
| `Left` | Escolhe outra receita, quando o plano tem mais de uma. |
| `Right` | Escolhe outra receita, quando o plano tem mais de uma. |

A quantidade, o número de itens a fabricar, começa em 1 item e pode chegar a 99.999 itens.

Table: Os textos do pop-up de fabricação

| O texto | O que ele informa |
| --- | --- |
| Fabricar, depois o nome do item | O botão de fabricar, na lista. |
| Requisição, depois um número de itens | O botão que requisita essa quantidade de itens do armazenamento. |
| RECIPE | Quantos padrões fazem o item, quando há mais de um. |
| PLAN - with, depois uma receita | O plano, com a receita que você escolheu. |
| PLAN - ingredientes brutos | O plano, quando o item não tem receita para escolher. |
| planejando | O plano está sendo calculado. |
| EST (tempo estimado), depois segundos | O tempo estimado da fabricação, em segundos. |
| EST seguido de dois hífens | Ainda não há estimativa. |
| Prio, depois um nível | A prioridade da fabricação, alterada com o botão de prioridade: LOW, MED-, MED, MED+ ou HIGH, com MED como padrão. |

O computador lembra a receita escolhida para cada item, então você escolhe uma vez só.

## No terminal

O pop-up de fabricação do terminal tem os botões CRAFT e PARTIAL. CRAFT faz a fabricação inteira. PARTIAL faz o que a rede consegue fazer agora, e o pop-up mostra essa maior quantidade como "máximo agora". CLOSE fecha o pop-up.

A aba Craft do terminal lista o que está RUNNING e o que está RECÉM-FEITO. Ela mostra "nada sendo feito" quando a rede está ociosa. Um padrão precisa estar carregado antes de o terminal poder fabricar: sem um, o terminal mostra "nenhum padrão carregado" e pede que você carregue um de uma mídia em Padrões.

Table: As palavras de uma fabricação concluída

| Palavra | O que significa |
| --- | --- |
| concluído | A fabricação fez tudo o que pediu. |
| parcial | A fabricação fez parte do que pediu. |
| bloqueado | Outra operação segurou o que a fabricação precisava, então a fabricação expirou esperando. |
| falhou | A fabricação não pôde ser feita. |

## Cancelando uma fabricação

Você cancela uma fabricação no Gerenciador de Rede. Abra os detalhes da fabricação ali e aperte Cancelar. Uma fabricação cancelada enquanto o seu plano ainda está sendo calculado é descartada ([](jsc:network_manager)).

## Se algo der errado

> [!PROBLEM] Uma fabricação de muitos passos falha em uma etapa.
> Essa etapa esgotou o tempo: a máquina dela está sem energia, está cheia, ou nunca entregou a saída ao Barramento de Recebimento. A fabricação indica a etapa ([](jsc:when_a_craft_goes_wrong)).

> [!PROBLEM] A fabricação mostra "bloqueado".
> Outra operação segurou o que ela precisava. Peça de novo quando essa operação tiver terminado.

> [!SEE] jsc:iql, jsc:craft_planner, jsc:crafting_manager, jsc:network_manager, jsc:when_a_craft_goes_wrong, jsc:priorities, jsc:operation_states
