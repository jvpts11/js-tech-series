---
section: isms_studio
order: 395
---
# O Monitor de Atividade

O Monitor de Atividade mostra as Operações em andamento na rede: o trabalho que a rede está fazendo agora. Abra-o pelo menu Ferramentas, e você verá a lista se atualizar sozinha enquanto ela estiver aberta.

A linha de cima mostra "Slots de fabricação <used> / <total>": os slots de fabricação em uso sobre o total. À direita mostra "<n> em execução". Quando nada está rodando, a lista diz "Nenhuma Operation em andamento."

Table: O status de cada Operação

| Palavra de status | Grupo |
| --- | --- |
| concluída, em execução | em andamento |
| parcial, aguardando, na fila | retida |
| falhou, bloqueada, descartada | parada |

Cada grupo tem uma cor própria, para você ver de relance quais Operações andam e quais estão retidas ou paradas. Os status são explicados em [os estados da Operação](jsc:operation_states).

Clique numa linha de Operação para abrir um menu com um item, Cancelar Operação. O item só funciona para uma Operação que tenha id. Cancelar Operação pede à rede que pare essa Operação.

## Se algo der errado

> [!PROBLEM] A lista diz "Nenhuma Operation em andamento."
> Inicie uma consulta ou uma fabricação. A Operação dela aparece nesta lista.

> [!PROBLEM] Cancelar Operação não pode ser escolhido numa linha.
> Essa Operação ainda não tem id. Espere até ela receber um, ou pressione [[Alt]]+[[Break]] na aba de consulta que a iniciou.

> [!SEE] jsc:operations_catalogue, jsc:cancelling_and_the_log
