---
section: iql_recipe_list
order: 402
icon: jsc:mainframe
---
# Fabricar em um intervalo

Esta receita faz a rede fabricar um item de novo e de novo, com uma pausa entre cada fabricação. Você define a pausa como um intervalo, escrito como uma duração depois de EVERY, como 30s ou 1h ([](jsc:iql_jobs)).

O intervalo conta ticks do jogo, então mede o tempo de jogo, e não a hora do dia. Uma tarefa só dispara enquanto o Motor de Automação estiver instalado e o Mainframe ligado.

Uma fabricação não pode ser marcada para uma hora do dia. A única janela de horário do IQL pertence a um barramento ([](jsc:bus_conditions)).

Table: O que a receita precisa

| Necessidade | O que exige |
| --- | --- |
| Era | Legacy e mais novas, com o Motor de Automação |
| Programa | O Motor de Automação instalado no Mainframe, e o Mainframe ligado |
| Motor | O Midsoft IQL Server em execução: os outros motores não guardam tarefas |
| Padrão | Um padrão para o item na rede |

1. Instale o Motor de Automação no Mainframe ([](jsc:automation_engine)).
2. Confirme que a rede tem um padrão para o item ([](jsc:patterns)).
3. Digite a tarefa no prompt, com iql na frente, seguido do número e do intervalo.
4. Espere um intervalo. A primeira execução acontece um intervalo depois da primeira conferência da tarefa.

```table
{"caption": "Uma linha para uma tarefa", "rows": [[{"words": "Fabricar 64 tochas a cada hora"}, {"literal": "CREATE JOB torch_hourly AS CRAFT 64 torch EVERY 1h"}]]}
```

O Gerenciador de Automação tem o mesmo recurso como um formulário, chamado Fabricação em Lote. Seu campo tem o rótulo A cada (30s), e ele aceita uma duração como 30s ou 5m.

Table: Formas de uma duração

| Escrita | Ticks | Segundos |
| --- | --- | --- |
| `100t` | 100 | 5 |
| `30s` | 600 | 30 |
| `5m` | 6.000 | 300 |
| `1h` | 72.000 | 3.600 |

Um número sem letra conta segundos. Uma letra depois do número muda a unidade: s para segundos, m para minutos, h para horas e t para ticks. Há 20 ticks em um segundo.

Escreva sempre o número. Um CRAFT sem número pede à rede o máximo que ela conseguir fabricar, toda vez que a tarefa rodar.

## Se algo der errado

> [!PROBLEM] A resposta diz "nenhum padrão fabrica torch".
> A rede não tem padrão para o item. Dê um a ela primeiro ([](jsc:patterns)).

> [!PROBLEM] A resposta diz "item desconhecido: torches".
> Use o id do item, como torch, e não o nome dele.

> [!PROBLEM] A resposta diz "a rede não tem um Mainframe em execução".
> Ligue o Mainframe. A fabricação da tarefa não pode começar sem ele.

> [!PROBLEM] A resposta diz "os jobs ficam com o Motor de Automação; instale-o primeiro no Mainframe".
> Instale o Motor de Automação no Mainframe ([](jsc:automation_engine)).

> [!PROBLEM] A resposta diz "o engine da rede não guarda views, procedimentos nem jobs".
> O Gerenciador de Automação diz "Fabricação em Lote precisa de um item e um intervalo válido (ex.: 30s, 5m)."

> [!PROBLEM] Preencha o item, e um intervalo maior que zero, como 30s ou 5m.
> Use o Midsoft IQL Server para a tarefa ([](jsc:engines)).

> [!SEE] jsc:iql_jobs, jsc:automation_manager, jsc:how_autocrafting_works, jsc:asking_for_a_craft
