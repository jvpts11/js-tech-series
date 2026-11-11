---
section: iql_saved_objects
order: 380
---
# Jobs, EVERY e WHEN

> [!DEFINE] tarefa
> Uma instrução que a rede executa sozinha, sempre que o gatilho dela dispara.

> [!DEFINE] tick
> Um passo do relógio do jogo. 20 ticks formam um segundo.

Um job é uma instrução que a rede executa sozinha. Você lhe dá um gatilho: um intervalo de tempo com EVERY, ou uma condição com WHEN. Toda vez que o gatilho dispara, a instrução roda de novo.

As tarefas são guardadas pelo [Motor de Automação](jsc:automation_engine), que precisa estar instalado no Mainframe. Elas também precisam do [Midsoft IQL Server](jsc:midsoft_iql_server).

```iql jobs.iql
CREATE JOB restock AS SELECT 64 iron_ingot EVERY 30s
CREATE JOB low_gold AS SELECT 64 gold_ingot WHEN qty(gold_ingot) < 100
```

A palavra de gatilho é o último EVERY ou WHEN que fica fora de aspas e chaves. As tarefas só rodam enquanto o Motor de Automação estiver instalado e rodando. Uma tarefa pausada fica parada até você reiniciá-la pela aba Processos. Uma tarefa é creditada ao jogador que a criou.

## EVERY: um intervalo de tempo

Table: Como escrever uma duração

| Escrito como | Significa | Ticks |
| --- | --- | --- |
| `30` | 30 segundos, já que um número sem unidade conta segundos | 600 |
| `100t` | 100 ticks | 100 |
| `30s` | 30 segundos | 600 |
| `5m` | 5 minutos | 6.000 |
| `1h` | 1 hora | 72.000 |

A rede confere as tarefas a cada 10 ticks, então um intervalo é exato até cerca de 10 ticks. A primeira conferência inicia o relógio, então uma tarefa não dispara de imediato.

## WHEN: uma condição

O WHEN dispara a tarefa uma vez, quando a condição passa de falsa para verdadeira. Ela não dispara de novo enquanto a condição continuar verdadeira. Uma condição que já é verdadeira dispara na primeira conferência.

Um WHEN lê três campos: qty(item), que é o total do item na rede; clock, o relógio do mundo como 'HH:MM'; e weekday, o dia da semana pelo nome em inglês. Um WHEN com qualquer outro campo nunca dispara.

Um CREATE JOB cuja instrução é SET BUS com WHEN TIME não cria uma tarefa. Em vez disso, ele define as horas do barramento ([](jsc:set_bus)).

## WHEN no relógio, e BACKUP

WHEN clock = '02:00' dispara a tarefa às duas da manhã pelo relógio do mundo, uma vez por dia: o relógio marca esse minuto só uma vez, por isso a condição fica verdadeira uma vez. Acrescente AND weekday = 'Sunday' para dispará-la só aos domingos. Os dias vão de Monday a Sunday, e o dia do mundo, como o seu relógio, começa às seis da manhã.

BACKUP executa agora uma das tarefas do Serviço de Backup, pelo nome: BACKUP Nightly. Um backup a que você dá uma hora no Console de Backup é uma tarefa assim, que o console cria por você ([](jsc:backups)).

```iql nightly.iql
CREATE JOB backup_Nightly AS BACKUP Nightly WHEN clock = '02:00'
```

Table: As respostas para jobs

| Resposta | O que significa |
| --- | --- |
| `job <name> dropped` | O job não roda mais. |
| `no job named <name>` | Nenhuma tarefa tem esse nome. |
| `DROP JOB needs a name` | Digite o nome depois de DROP JOB. |

## Se algo der errado

> [!PROBLEM] CREATE JOB precisa de um gatilho: EVERY <duração> ou WHEN <condição>
> Acrescente EVERY e uma duração, ou WHEN e uma condição, depois da instrução.

> [!PROBLEM] CREATE JOB precisa de um corpo antes do seu gatilho
> Coloque a instrução antes de EVERY ou WHEN.

> [!PROBLEM] o gatilho do CREATE JOB precisa de um valor
> Digite a duração ou a condição depois da palavra de gatilho.

> [!PROBLEM] os jobs ficam com o Motor de Automação; instale-o primeiro no Mainframe
> Instale o Motor de Automação no Mainframe e, depois, crie o job.

> [!PROBLEM] não é uma duração: <text>
> Digite um número, com t, s, m ou h depois dele se quiser, como 30s.

> [!PROBLEM] duração vazia
> Digite uma duração depois de EVERY, como 30s.

> [!PROBLEM] a duração precisa ser >= 0: <text>
> Digite uma duração igual ou maior que 0.

> [!PROBLEM] unidade de duração desconhecida '<unit>' em: <text>
> Use t, s, m ou h como unidade, ou nenhuma unidade para segundos.

> [!PROBLEM] O job nunca dispara, porque o intervalo dele é 0.
> Use um intervalo maior que zero, como 30s.

> [!PROBLEM] O job nunca dispara, e nenhum erro aparece.
> A condição do WHEN não é lida. Confira as palavras e a grafia da condição.

> [!PROBLEM] O job não dispara de forma alguma.
> Confira se o Motor de Automação está instalado e rodando, e se o job não está pausado.

> [!PROBLEM] nenhum trabalho chamado <name>
> Confira o nome, ou crie o job primeiro.

> [!SEE] jsc:automation_engine, jsc:iql_procedures, jsc:set_bus
