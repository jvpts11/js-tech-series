---
section: iql_settings
order: 377
---
# SET REDSTONE

Uma [Redstone Interface](jsc:redstone_interfaces) liga a rede aos sinais de redstone do mundo. SET REDSTONE diz a ela se deve ler um sinal ou emitir um, e com que força.

Table: As duas formas de SET REDSTONE

| Instrução | O que faz |
| --- | --- |
| `SET REDSTONE 'x' IN` | A interface lê um sinal. |
| `SET REDSTONE 'x' OUT 15` | A interface emite um sinal com a força que você digitar, de 0 a 15. |

A força é um número inteiro de 0 a 15. O nome que você digitar precisa ser uma das interfaces às quais este computador está ligado.

Table: As respostas de SET REDSTONE

| Resposta | O que significa |
| --- | --- |
| `Redstone Interface <name> reads` | A interface agora lê um sinal. |
| `Redstone Interface <name> emits <strength>` | A interface agora emite essa força. |
| `no Redstone Interface named <name> is linked to this machine` | Este computador não tem nenhuma interface com esse nome. |

## Se algo der errado

> [!PROBLEM] SET REDSTONE precisa do nome da interface, entre aspas quando ela tem espaços
> Digite o nome da interface depois de REDSTONE.

> [!PROBLEM] SET REDSTONE precisa de IN, ou de OUT e uma força de 0 a 15
> Adicione IN, ou OUT seguido de um número.

> [!PROBLEM] não é uma força de redstone, que vai de 0 a 15: <text>
> Digite um número inteiro de 0 a 15.

> [!PROBLEM] nenhuma Interface de Redstone chamada <name> está ligada a esta máquina
> Use o nome de uma interface à qual este computador esteja ligado.

> [!SEE] jsc:redstone_interfaces, jsc:set_bus
