---
section: shell_commands
order: 253
---
# Informações da máquina e do sistema

Estes comandos informam sobre o computador que você usa: o seu sistema, o seu nome, a sua energia e as suas configurações. Alguns funcionam em todas as máquinas, e os outros só nos sistemas UNIX, como cada bloco indica.

O comando config mostra e altera as configurações do computador. Sem argumento, ele as lista. Com uma chave e um valor, como em config name <name>, ele altera uma configuração.

Table: Duas configurações do config

| Configuração | O que recebe |
| --- | --- |
| `name` | O nome do computador, que o prompt mostra depois do arroba. |
| `netshare` | Um número de 0 a 1.000, em milésimos (permille). |

O comando config tem mais opções, como papel de parede e tema.

```command
id: jsc:command/linux/uname
```

```command
id: jsc:command/linux/hostname
```

```command
id: jsc:command/linux/date
```

```command
id: jsc:command/linux/status
```

```command
id: jsc:command/linux/config
```

```command
id: jsc:command/linux/reboot
```

O comando reboot pode abrir o firmware em vez do sistema: a sua opção --firmware entra no Setup do [firmware](jsc:firmware).

```command
id: jsc:command/linux/devices
```

O comando fsck conserta um disco corrompido, como o Defrag faz nos outros sistemas ([](jsc:corruption_and_the_defrag)).

```command
id: jsc:command/linux/fsck
```

```command
id: jsc:command/linux/which
```

```command
id: jsc:command/linux/whoami
```

```command
id: jsc:command/linux/echo
```

```command
id: jsc:command/linux/clear
```

```command
id: jsc:command/linux/set
```

```command
id: jsc:command/linux/export
```

```command
id: jsc:command/linux/unset
```

Você não vai encontrar um comando version nestes sistemas. Ele pertence ao MC-DOS e aos sistemas Frames, onde mostra a versão do shell.

## Se algo der errado

> [!PROBLEM] O comando config diz "configuração desconhecida", seguido do nome que você digitou.
> Execute config sem argumento para ver os nomes das configurações.

> [!PROBLEM] O comando config diz que netshare precisa de um número de 0 a 1.000.
> Informe um número inteiro de 0 a 1.000.

> [!PROBLEM] whoami não mostra nenhum nome de login.
> Digite echo "$USER", que imprime player.

> [!SEE] jsc:devices_commands, jsc:shell_prompt
