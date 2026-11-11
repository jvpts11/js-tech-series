---
section: shell_commands
order: 258
---
# A rede pelo prompt

Estes comandos usam a rede de dados do seu computador, os cabos e o armazenamento que unem os computadores ([](jsc:what_a_network_is)). Sem uma rede, o comando net diz que o computador não está em uma rede.

A rede responde a consultas em IQL, a sua própria linguagem de consulta ([](jsc:iql)). O comando iql executa uma instrução IQL e imprime a resposta, com até 64 linhas.

```command
id: jsc:command/linux/net
```

```command
id: jsc:command/linux/iql
```

O comando interac opera os itens da rede. Sozinho, ele ocupa o terminal inteiro e mostra a rede em três abas: Rede, Servidores e Bloqueados. Com palavras, ele faz uma coisa só e devolve o prompt.

Table: Opções do interac

| Opção | O que faz |
| --- | --- |
| `--to` | Deixa o que você pegar neste computador, em vez de nas suas mãos. |
| `--sort name` | Ordena as linhas por nome em vez de por quantidade, que é o padrão. |

```command
id: jsc:command/linux/interac
```

O comando ssh abre um shell em outro computador da rede, e o comando exit fecha esse shell remoto e devolve você a este computador. Digitado sem nome, o ssh lista os computadores que ele consegue alcançar.

```command
id: jsc:command/linux/ssh
```

```command
id: jsc:command/linux/exit
```

## Se algo der errado

> [!PROBLEM] O comando net diz que o computador não está em uma rede.
> Conecte o computador a um cabo de dados ([](jsc:data_cables)).

> [!PROBLEM] O comando iql diz syntax, seguido de uma mensagem.
> A mensagem diz o que está errado na instrução. Confira-a nas páginas de IQL ([](jsc:iql)).

> [!PROBLEM] O comando ssh diz que nenhum outro computador pode ser alcançado nesta rede.
> Ligue o outro computador e confira se ele está na mesma rede.

> [!SEE] jsc:file_commands, jsc:devices_commands
