---
section: automation_jobs
order: 152
icon: jsc:mainframe
---
# O Motor de Automação e os jobs

Algum trabalho deveria acontecer sozinho, para que você não precise vigiá-lo: repor um item quando a rede fica em falta, ou mover itens a cada hora. Um job é uma instrução salva em IQL, a linguagem de consulta da rede, com um gatilho que diz quando a instrução roda. O Motor de Automação é o serviço que mantém os jobs da rede e os dispara na hora certa, ou quando uma condição se verifica.

> [!DEFINE] Job
> Uma instrução IQL salva que a rede executa sozinha, em um horário definido ou quando uma condição se verifica.

> [!DEFINE] Motor de Automação
> Serviço do Mainframe que guarda os jobs e os dispara. Ele não tem janela própria.

Instale o Motor de Automação no Mainframe, como você instala qualquer serviço ([](jsc:mainframe_services)). Ele não tem opção de ligar e desligar própria. Ele dispara jobs enquanto estiver instalado e o Mainframe estiver rodando, com uma compilação válida e energia ligada.

Se o Mainframe estiver desligado, os jobs continuam na lista e nenhum deles dispara. Ligue o Mainframe de novo, e eles voltam a disparar.

## Gatilhos

Você dá um gatilho depois de EVERY, com um intervalo, ou depois de WHEN, com uma condição. O Motor de Automação verifica as tarefas a cada 10 ticks, o que equivale a meio segundo.

> [!DEFINE] Gatilho
> A parte de um job que diz quando ele dispara: um intervalo depois de EVERY, ou uma condição depois de WHEN.

EVERY dispara o job de novo depois de cada intervalo. A primeira verificação apenas inicia a contagem, então um job EVERY novo espera um intervalo completo antes da primeira execução.

WHEN dispara o job na borda de subida da sua condição: o momento em que a condição passa de falsa para verdadeira. Ele não dispara de novo enquanto a condição continuar verdadeira. Uma condição que já é verdadeira na primeira verificação dispara o job uma vez, nessa verificação.

> [!DEFINE] Borda de subida
> O momento em que uma condição passa de falsa para verdadeira. Um job WHEN dispara uma vez a cada um desses momentos.

Uma condição WHEN testa o estoque de um item da rede, com qty(<item>), contra um número, ou o relógio do mundo, com clock = '02:00' e weekday = 'Sunday'. Os operadores são =, ==, !=, <, >, <= e >=. Um nome de item sem dois-pontos significa minecraft:, então iron_ingot é minecraft:iron_ingot. Um item de mod precisa do nome completo, com o nome do mod antes dos dois-pontos.

WHEN EVENT espera por um evento do mundo que a rede vê: WHEN EVENT mymod:solar_storm COMING dispara quando a rede o vê a caminho pela primeira vez, o que pede uma máquina na rede que seja o instrumento do evento; UNDER WAY dispara quando ele começa onde a rede está; ENDED dispara quando ele termina. Nomeie o evento pelo id, ou pela parte depois dos dois-pontos.

```iql storm_shelter.iql
CREATE JOB shelter AS SET BUS 'Ore in' OFF WHEN EVENT mymod:solar_storm COMING
```

Table: Formas de duração

| Forma | Significado | Ticks |
| --- | --- | --- |
| `30s` | 30 segundos | 600 |
| `5m` | 5 minutos | 6.000 |
| `1h` | 1 hora | 72.000 |
| `100t` | 100 ticks | 100 |
| `30` | 30 segundos, um número sem unidade | 600 |

As letras de unidade são s para segundos, m para minutos, h para horas e t para ticks. Um número sem letra conta segundos. Há 20 ticks em um segundo.

## Escrevendo um job

1. Instale o Motor de Automação no Mainframe.
2. No prompt de um computador da rede, digite uma linha CREATE JOB, como mostra a listagem.
3. Aperte [[Enter]]. A resposta termina com "criado" e cita o nome do job.
4. Confira o job na aba Processos ou no Gerenciador de Automação.

```iql keep_iron.iql
CREATE JOB keep_iron AS CRAFT 64 iron_ingot EVERY 5m
```

Escreva o nome, depois AS e a instrução que o job executa, depois EVERY com uma duração ou WHEN com uma condição. Os nomes podem ter até 64 caracteres. DROP JOB seguido do nome remove um job. O livro de IQL explica as instruções de job por completo ([](jsc:iql_jobs)).

## Acompanhando os jobs

A aba Processos do Gerenciador de Tarefas ([](jsc:system_monitor_and_task_manager)) lista primeiro o motor da rede e depois cada job com o seu estado.

Table: Estados de um job

| Estado | O que significa |
| --- | --- |
| `ACTIVE` | O Motor de Automação está rodando, então o job pode disparar. |
| `IDLE` | O Motor de Automação não está rodando, então o job não dispara. |
| `PAUSED` | O job está pausado, e não dispara até ser reiniciado. |

O detalhe ao lado de um job mostra o seu gatilho, como "every 5m" ou "when qty(iron_ingot) < 64". Nessa aba, Iniciar e Parar agem sobre o motor da rede, não sobre os jobs.

## Pausando e reiniciando

Pausar um job o mantém na lista e impede que ele dispare. Reiniciar um job o retoma, e um job EVERY conta o seu intervalo de novo a partir de agora. DROP JOB remove o job e a pausa junto com ele.

## Backups com hora

Um backup a que você dá uma hora no Console de Backup também é uma tarefa aqui, chamada backup_ e o nome do backup, como backup_Nightly: ela executa BACKUP Nightly WHEN clock = '02:00'. O console a cria, a altera e a remove junto com o backup, por isso mude a hora no console e não aqui ([](jsc:backups)).

## Se algo der errado

> [!PROBLEM] Aparece "os jobs ficam com o Motor de Automação; instale-o primeiro no Mainframe".
> Instale o Motor de Automação no Mainframe e digite o job de novo.

> [!PROBLEM] Aparece "o engine da rede não guarda views, procedimentos nem jobs".
> Use um motor que mantenha objetos salvos, como o Midsoft IQL Server ([](jsc:engines)).

> [!PROBLEM] Aparece "CREATE JOB precisa de um gatilho: EVERY <duração> ou WHEN <condição>".
> Acrescente EVERY com uma duração, ou WHEN com uma condição, no fim da linha.

> [!PROBLEM] Aparece "CREATE JOB precisa de um corpo antes do seu gatilho".
> Escreva a instrução entre AS e EVERY ou WHEN.

> [!PROBLEM] Aparece "o gatilho do CREATE JOB precisa de um valor".
> Escreva a duração ou a condição depois de EVERY ou WHEN.

> [!PROBLEM] Aparece "CREATE espera VIEW, PROCEDURE ou JOB".
> Escreva VIEW, PROCEDURE ou JOB depois de CREATE.

> [!PROBLEM] Aparece que o CREATE precisa de um nome e de um corpo AS, ou que o AS deve vir depois do nome.
> Escreva CREATE JOB, o nome, depois AS e a instrução.

> [!PROBLEM] Aparece "um nome tem no máximo 64 caracteres".
> Escolha um nome mais curto, de no máximo 64 caracteres.

> [!PROBLEM] Um job feito em IQL com um intervalo ruim nunca dispara.
> O IQL não verifica a duração do EVERY quando você digita a tarefa, então uma duração inválida nunca dispara. Escreva um número com uma unidade opcional: s, m, h ou t, como 30s ou 100t.

> [!PROBLEM] Um job aparece como PAUSED.
> Reinicie-o com o controle > no Gerenciador de Automação.

> [!PROBLEM] O job dispara, mas nada se move.
> O job executa como Operações comuns. Veja as Operações que ele iniciou ([](jsc:operations)), onde cada uma mostra o seu estado.

> [!SEE] jsc:automation_manager, jsc:engines, jsc:operations
