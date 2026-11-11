---
section: iql_verbs
order: 371
---
# LOCK e UNLOCK

LOCK retém os itens onde estão. Outra Operação que peça os mesmos itens então espera, em vez de pegá-los. UNLOCK os libera de novo. Use LOCK para manter o estoque no lugar enquanto você precisa dele.

```iql hold.iql
LOCK 64 iron_ingot
UNLOCK iron_ingot
```

Sem quantidade, ou com ALL, LOCK retém cada item livre daquele tipo. UNLOCK usa apenas o item, então uma quantidade que você digitar depois de UNLOCK não tem efeito.

LOCK e UNLOCK funcionam sem motor. PRIORITY é aceito neles, mas não muda nada. O Gerenciador de Tarefas mostra o que está bloqueado ([](jsc:system_monitor_and_task_manager)).

Table: As respostas de LOCK e UNLOCK

| Resposta | O que significa |
| --- | --- |
| `LOCK held <quantity> <item> (concurrent operations will wait)` | Os itens ficam retidos, e outras Operações esperam por eles. |
| `<item> is already locked` | Você já bloqueou este item à mão. |
| `nothing to lock: the network holds no free <item>` | Todas as cópias estão em uso, ou nenhuma está na rede. |
| `UNLOCK released <quantity> <item>` | A retenção acabou. |
| `<item> is not locked` | Não havia retenção a liberar. |

## Se algo der errado

> [!PROBLEM] <item> já está bloqueado
> O item continua retido. Digite UNLOCK com o nome dele quando quiser liberá-lo.

> [!PROBLEM] nada para lock: the network holds no free <item>
> Espere até que a rede tenha estoque livre, e então bloqueie de novo.

> [!PROBLEM] <item> não está bloqueado
> Confira o nome do item. UNLOCK só libera uma retenção que você colocou à mão.

> [!SEE] jsc:operation_states, jsc:states_and_queues
