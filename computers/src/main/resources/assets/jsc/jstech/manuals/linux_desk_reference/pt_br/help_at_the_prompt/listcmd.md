---
section: help_at_the_prompt
order: 284
icon: jsc:guide_to_operations
---
# O comando listcmd

listcmd lista tudo o que este computador pode executar: seus comandos, de qualquer sistema a que pertençam, e os programas instalados nele. Ele imprime uma única lista, e nada fica escondido dela.

Normalmente o listcmd não existe, porque cada sistema ensina os próprios comandos do seu jeito. O dono do servidor pode ligá-lo para todos os computadores, se você preferir ler uma só lista.

## Ligando o comando

1. Abra jscomputers-server.toml na pasta serverconfig do mundo.
2. Defina prompt.list_commands como true.
3. Salve o arquivo.

No modo single-player, é o mesmo arquivo, na pasta serverconfig do seu mundo.

Table: Onde fica a opção

| Configuração | Valor |
| --- | --- |
| Arquivo | `jscomputers-server.toml` |
| Pasta | serverconfig, dentro da pasta do mundo |
| Chave | `prompt.list_commands` |
| Padrão | `false` |

Table: As duas partes da lista

| Cabeçalho | O que lista |
| --- | --- |
| `COMMANDS` | Uma linha por comando que este computador tem, com sua descrição de uma linha. |
| `PROGRAMS` | Uma linha por programa instalado, com seu nome e seu id. |

Os comandos dependem do sistema do computador, e os programas dependem do que está instalado nele. Quando a opção está ligada, listcmd também aparece na lista que o help imprime, como qualquer outro comando.

```command
id: jsc:command/linux/listcmd
```

## Se algo der errado

> [!PROBLEM] A resposta é "comando não encontrado: listcmd".
> A opção está desligada neste servidor, então o comando não existe. Peça ao dono do servidor para definir prompt.list_commands como true.

> [!PROBLEM] A lista é mais curta em um computador do que em outro.
> Cada computador lista apenas o que tem. Um programa aparece em PROGRAMS assim que estiver instalado naquele computador.

> [!SEE] jsc:help_command, jsc:programs
