---
section: prompt_system
order: 241
---
# A máquina: status, config, devices, reboot, date e mem

Estes verbos perguntam à máquina sobre si mesma. status mostra se ela está ligada, sua taxa e seu link. config mostra e altera suas configurações. devices lista os dispositivos ligados, reboot reinicia o computador, date informa o dia e a hora, e mem mostra a memória.

## status

status mostra se o computador está online, sua taxa de processamento, seu buffer de memória e seu link de rede. A primeira linha mostra ONLINE em verde, ou OFFLINE em vermelho. Depois vêm três linhas.

> [!DEFINE] Tick
> A batida do jogo. Um segundo tem 20 deles.

Table: As linhas de status

| Linha | O que mostra |
| --- | --- |
| `cpu` | A taxa de processamento, em it/t (itens por tick). |
| `ram` | O buffer de memória, em it, a unidade que o mod usa para itens. |
| `network` | linked, com o id da rede entre colchetes, ou -- quando não há link. |

```command
id: jsc:command/mc_dos/status
```

## config

config sem palavras lista as configurações deste computador sob o título settings, e informa como alterar uma delas. Com uma chave e um valor depois dela, config key value altera uma configuração.

Table: Configurações com regras próprias

| Chave | O que faz |
| --- | --- |
| `name` | O nome do computador, que o whoami mostra. |
| `theme` | Aplica um predefinido de cor de destaque e papel de parede. |
| `netshare` | Quanto do disco a rede pode ler, em permille (partes por mil), de 0 a 1000. |
| `share FOLDER [read or write]` | Abre uma pasta para a rede. Uma pasta compartilhada aparece como \\\\<host>\\<name>. |
| `unshare NAME` | Fecha uma pasta compartilhada. |

Outras chaves vão para o armazenamento de configurações da máquina. Uma máquina sem esse armazenamento diz que não tem um. Por exemplo, config netshare 500 define que a rede pode ler metade do disco.

```session
{"lines": [{"prompt": "C:\\> ", "text": "config netshare 500", "typed": true}, {"text": "netshare set"}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

```command
id: jsc:command/mc_dos/config
```

## devices and reboot

> [!DEFINE] Periférico
> Um dispositivo ligado a uma porta de um computador, como um monitor, uma unidade ou uma impressora.

devices lista os periféricos ligados a este computador. Apenas um computador com portas para periféricos tem este verbo.

```command
id: jsc:command/mc_dos/devices
```

reboot reinicia o computador na hora. reboot --firmware o reinicia para a configuração do firmware, o programa que roda antes do sistema ([](jsc:firmware)). O prompt diz "O sistema está sendo desligado para reiniciar AGORA!" e então reinicia.

> [!WARNING] reboot começa na hora, sem pergunta antes. Salve seu trabalho antes de digitá-lo.

```command
id: jsc:command/mc_dos/reboot
```

## date and mem

date imprime o dia e a hora pelo relógio do mundo. O primeiro dia é o dia 1. Um dia começa às 06:00 nesse relógio, então as horas de um dia vão das seis da manhã em diante.

> [!DEFINE] Relógio do mundo
> O único relógio que o mundo inteiro usa. Os trabalhos, e os horários dos arquivos, são lidos dele.

```command
id: jsc:command/mc_dos/date
```

mem mostra quanta memória o computador tem, quanta está retida agora e quanta está prometida ao que roda. Livre é o que sobra depois de tirar a memória prometida, então indica se mais um programa cabe.

> [!DEFINE] Memória comprometida
> Memória prometida aos programas em execução. Pode ser mais do que a memória em uso neste momento.

Table: As linhas de mem

| Linha | O que mostra |
| --- | --- |
| Memória total | Toda a memória que o computador tem, em MB. |
| Em uso | A memória retida neste momento. |
| Comprometida | A memória prometida aos programas em execução, em MB. |
| Livre | Memória total menos a memória comprometida, nunca abaixo de zero, em MB. |

```command
id: jsc:command/mc_dos/mem
```

## Se algo der errado

> [!PROBLEM] config informa: configuração desconhecida: NAME
> A máquina não tem nenhuma configuração com essa chave. Digite config sozinho para listar as configurações.

> [!PROBLEM] config informa que netshare precisa de um número de 0 a 1000
> Informe um número inteiro de 0 a 1000.

> [!PROBLEM] config share recusa: este computador já tem todas as pastas que pode compartilhar.
> Deixe de compartilhar uma pasta com config unshare NAME, e então compartilhe a nova.

> [!PROBLEM] config share informa que outra pasta já está compartilhada como NAME
> Escolha outro nome para o compartilhamento, ou deixe de compartilhar a pasta antiga primeiro.

> [!PROBLEM] config unshare informa que nada está compartilhado como NAME
> Confira o nome. config unshare usa o nome que o compartilhamento recebeu.

> [!PROBLEM] devices não aparece na ajuda.
> Este computador não tem portas para periféricos, então não tem o verbo devices. Use um computador com porta, ou leia [](jsc:peripheral_cables).

> [!SEE] jsc:firmware, jsc:peripheral_cables
