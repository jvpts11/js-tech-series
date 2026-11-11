---
section: iql_verbs
order: 369
---
# CRAFT

CRAFT pede à rede que fabrique itens a partir das suas receitas. Você não fabrica os itens à mão. A rede usa os padrões que aprendeu ([](jsc:patterns)) e as máquinas que os executam.

```iql torches.iql
CRAFT 64 torch
```

CRAFT precisa de um motor rodando no Mainframe, porque o motor descobre como fazer a fabricação. Sem um, a rede recusa a instrução. O que é um [motor](jsc:iql_engines_overview) e como você o inicia está explicado ali.

1. Digite CRAFT, uma quantidade e o item, como CRAFT 64 torch.
2. Pressione [[Enter]] e leia a resposta.

Table: As respostas de CRAFT

| Resposta | O que significa |
| --- | --- |
| `CRAFT queued: <quantity> <item>` | A fabricação começou. |
| `Network Operations Service unavailable` | Nenhum motor está rodando no Mainframe. |
| `the network has no running Mainframe` | A rede não tem nenhum Mainframe em funcionamento. |
| `no pattern crafts <item>` | Nenhuma receita da rede fabrica o item. |
| `unknown item: <name>` | O nome não é um item. |

Sem quantidade, a resposta mostra "all" no lugar do número. PRIORITY funciona em um CRAFT como nas outras ações ([](jsc:clauses_priority_if_with)). FROM e TO são lidos, mas CRAFT não os usa. As receitas são os padrões que a rede aprendeu, como os que uma Interface de Fabricação guarda ([](jsc:crafting_interfaces)).

## Se algo der errado

> [!PROBLEM] Network Operations Service indisponível
> Inicie um motor no Mainframe ([](jsc:iql_engines_overview)).

> [!PROBLEM] nenhum padrão fabrica <item>
> Ensine uma receita à rede com um Codificador de Padrões ([](jsc:pattern_encoders)), e então fabrique de novo.

> [!PROBLEM] item desconhecido: <name>
> Confira a grafia, e use o id do item, como iron_ingot.

> [!SEE] jsc:asking_for_a_craft, jsc:crafting_interfaces
