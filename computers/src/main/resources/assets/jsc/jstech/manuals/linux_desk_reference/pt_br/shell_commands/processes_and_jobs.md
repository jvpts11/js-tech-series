---
section: shell_commands
order: 254
---
# Processos, tarefas e agendamento

Um processo é um programa que está rodando agora. Cada processo tem um número, o seu ID de processo (PID), e o comando ps lista os processos com ele. Ele lista primeiro os processos do próprio sistema. O número 1 é o primeiro processo, que inicia todos os outros: systemd na maioria das distribuições Linux, e init no Gentoo, no FreeBSD e no UNIX. Em seguida vem cada serviço que o sistema roda, e os programas que você inicia são numerados a partir de 1000.

> [!DEFINE] Processo
> Um programa em execução. Cada programa em execução tem um número, que o ps lista como seu PID.

O comando kill para um processo pelo número. O processo para onde está, e o trabalho que ele não tinha terminado fica por fazer. Um sinal pode vir antes do número, como -9 ou -KILL, e kill -l lista os sinais. Um serviço que você para continua parado até o computador ligar de novo. Parar o processo 1 derruba o sistema inteiro: o kernel entra em pânico, mostra o motivo na tela e o computador recomeça sozinho.

Uma tarefa é um comando em segundo plano que você iniciou com um e comercial (&). Você lista suas tarefas com jobs, e para uma delas com kill %1, onde 1 é o número dela nessa lista.

```command
id: jsc:command/linux/ps
```

```command
id: jsc:command/linux/kill
```

Quando o computador fica sem memória, o kernel, o núcleo do sistema, encerra o processo que mais ocupa, depois o seguinte, até que tudo o que sobra caiba. O comando dmesg mostra o que o kernel registrou desde que o computador ligou: o que encontrou ao iniciar e uma linha para cada processo que encerrou. Num desktop, uma janela que o kernel encerra fecha com um balão, "Out of memory", que diz o nome dela. Um programa encerrado na frente do terminal deixa a palavra Killed. O kernel só entra em pânico quando não sobra nada para encerrar, e então o computador recomeça sozinho. Um servidor num rack faz tudo isto como um computador, cada servidor por si, e um serviço que o kernel encerra nele, ou que você para, deixa de servir a rede até esse servidor recomeçar.

```command
id: jsc:command/linux/dmesg
```

```command
id: jsc:command/linux/jobs
```

O comando crontab define uma linha que a máquina executa sozinha, em uma hora que você escolhe, todos os dias ou em dias determinados. Essa linha é um agendamento.

> [!DEFINE] Agendamento
> Uma linha que a máquina executa sozinha, em um horário definido, todos os dias ou em dias determinados.

Table: Argumentos de crontab

| Argumento | O que faz |
| --- | --- |
| `-l` | Lista as linhas que a máquina executa, e quando. |
| `-r <id>` | Remove a linha com esse número. |
| `06:00` | A hora em que a linha é executada. A forma 6 também funciona. |
| `M, T, W, Th, F, Sa, Su` | Os dias, separados por vírgulas. Omita-os para executar todos os dias. |

```command
id: jsc:command/linux/crontab
```

## Minerando em segundo plano

O Minerador Blockwright minera Teracoin no prompt ou por trás dele ([](jsc:mining)). miner --address e o endereço da sua carteira diz a ele quem pagar; miner o mostra minerando até você apertar Q, miner start minera em segundo plano sempre que a máquina está ligada, e miner stop encerra isso.

```command
id: jsc:command/linux/miner
```

## Se algo der errado

> [!PROBLEM] O comando kill diz: kill: não é um ID de processo.
> Informe o número do processo da lista do ps, e não o nome dele.

> [!PROBLEM] O comando kill diz que não existe tal tarefa.
> O comando jobs mostra os números das tarefas atuais. Use um deles depois do sinal de porcentagem.

> [!PROBLEM] O comando crontab diz que a palavra informada não é uma hora do dia.
> Escreva a hora como "06:00" ou como "6".

> [!SEE] jsc:pipes_and_variables, jsc:system_commands
