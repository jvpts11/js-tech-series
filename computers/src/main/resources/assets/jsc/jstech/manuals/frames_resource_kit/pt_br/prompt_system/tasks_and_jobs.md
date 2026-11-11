---
section: prompt_system
order: 242
---
# Tarefas, execuções em segundo plano e a agenda

Uma tarefa é um programa que o computador está executando agora. Uma tarefa agendada é uma linha que o computador guarda para executar depois, ou em um horário de todos os dias. Estes comandos listam e param as tarefas, iniciam comandos em segundo plano e mantêm a agenda das tarefas agendadas.

> [!DEFINE] Tarefa
> Um programa que o computador está executando agora.

> [!DEFINE] PID
> O identificador do processo: o número que nomeia uma tarefa.

> [!DEFINE] Job
> Uma linha que o computador guarda para executar depois ou em um horário. Ela ocupa memória enquanto espera.

> [!DEFINE] Agenda
> A lista das tarefas agendadas, cada uma com o seu horário e os seus dias.

## tasklist e taskkill

O tasklist lista as tarefas que o computador está executando. Suas colunas são PID, NAME, FILE, STATE e MEM. MEM mostra a memória que cada tarefa ocupa, dentro da memória prometida a ela. Quando nada está em execução, diz "Nenhuma tarefa está em execução."

Nas edições do Frames, o tasklist funciona a partir do Frames XP. O Frames 95 não o tem.

```command
id: jsc:command/frames/tasklist
```

O taskkill para uma tarefa. Passe o número com /PID, como em taskkill /PID 12, ou escreva /PID:12. O número é o que o tasklist mostra. Assim como o tasklist, funciona no Frames XP e nos posteriores.

```command
id: jsc:command/frames/taskkill
```

## where e start

O where procura um comando pelo nome e informa onde a máquina o tem. Se não encontrar nada, diz INFO: não foi possível encontrar "NAME". Sem nome, mostra como usá-lo. Funciona no Frames XP e nos posteriores.

```command
id: jsc:command/frames/where
```

O start executa um comando e devolve o prompt na hora, enquanto o comando continua rodando. Em uma máquina que executa uma coisa por vez, ele enfileira os comandos iniciados e os executa em sequência. Cada comando iniciado ocupa 1 MB de memória enquanto roda.

```command
id: jsc:command/mc_dos/start
```

## at e a agenda

at executa um comando em uma hora do dia, todos os dias ou só nos dias que você nomear. O menor passo de tempo que uma tarefa pode usar é uma hora do relógio do mundo.

Table: Formas do at

| Forma | O que faz |
| --- | --- |
| `at` | Lista as tarefas agendadas, com os seus horários e dias. |
| `at 06:00 COMMAND` | Executa o comando todos os dias nesse horário. |
| `at 06:00 /EVERY:M,W,F COMMAND` | Executa o comando só nos dias indicados: M, T, W, Th, F, Sa e Su. |
| `at 2 /DELETE` | Tira a tarefa 2 da lista. |

Por exemplo, AT 06:00 INTERAC GET 64 COAL /LOCAL traz 64 de carvão para este computador toda manhã às seis. Só a hora conta: os minutos depois dos dois-pontos não são guardados, então escreva 06:00.

Quando o at adiciona uma tarefa, imprime "Adicionado um novo job com ID = ." e o número. Ao excluir uma tarefa, imprime "Excluída a tarefa" e o número dela. Um número de tarefa que não existe recebe "O ID do job não existe." Sem tarefas agendadas, o at diz "Não há entradas na lista."

```command
id: jsc:command/mc_dos/at
```

## Minerando em segundo plano

O Minerador Blockwright minera Teracoin no prompt ou por trás dele ([](jsc:mining)). MINER --ADDRESS e o endereço da sua carteira diz a ele quem pagar; MINER o mostra minerando até você apertar Q, MINER START minera em segundo plano sempre que o computador está ligado, e MINER STOP encerra isso.

```command
id: jsc:command/mc_dos/miner
```

## Se algo der errado

> [!PROBLEM] O tasklist diz "Nenhuma tarefa está em execução."
> Nenhum programa está em execução no computador agora. Inicie um programa e depois execute o tasklist de novo.

> [!PROBLEM] O taskkill diz que /PID precisa do número de uma tarefa.
> Escreva /PID, seguido do número que o tasklist mostra para a tarefa.

> [!PROBLEM] O start diz que não há memória suficiente para iniciar outra tarefa.
> Interrompa uma tarefa com o taskkill, ou adicione memória ([](jsc:frames_memory)).

> [!PROBLEM] O at diz que o horário não está no formato certo.
> Escreva o horário como 06:00 ou 6. Escreva os dias como M, T, W, Th, F, Sa ou Su, separados por vírgulas.

> [!PROBLEM] Uma tarefa agendada não executou no minuto que você pediu.
> Só a hora é guardada. Agende a tarefa para a hora de que você precisa.

> [!SEE] jsc:task_manager
