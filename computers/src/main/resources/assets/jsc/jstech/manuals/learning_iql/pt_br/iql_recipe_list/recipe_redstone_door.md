---
section: iql_recipe_list
order: 403
icon: jsc:standard_redstone_interface
---
# Uma porta de redstone

Uma Redstone Interface permite que um computador leia um sinal de redstone, ou envie um. Você a coloca, liga ao computador com um cabo periférico, e ela lê o redstone ao lado dela ou emite um sinal ali ([](jsc:redstone_interfaces)).

> [!DEFINE] Sinal de redstone
> O sinal de redstone, ligado ou desligado, com potência de 0 a 15. A potência 15 é a mais forte.

Uma linha SET REDSTONE faz a interface emitir uma potência. Uma porta que reage ao redstone ao lado da interface recebe esse sinal. A potência 15 é a mais forte, e 0 desliga o sinal.

Table: O que a receita precisa

| Necessidade | O que é preciso |
| --- | --- |
| Era | Todas as eras, de Vintage a Advanced, têm uma Redstone Interface |
| Programa | Nenhum: você digita a linha no prompt ([](jsc:where_to_type_iql)) |
| Link | Um cabo periférico da interface até o computador |

1. Coloque uma Redstone Interface ao lado da porta.
2. Ligue a interface ao computador com um cabo periférico ([](jsc:peripheral_cables)).
3. Dê um nome à interface na própria janela. O nome padrão é Redstone Interface, então dê a ela um nome que você consiga digitar, como Gate ([](jsc:redstone_interfaces)).
4. Digite iql SET REDSTONE 'Gate' OUT 15 no prompt, para enviar o sinal mais forte.
5. Digite iql SET REDSTONE 'Gate' OUT 0 no prompt, para desligar o sinal.

Table: Linhas de SET REDSTONE

| Linha | O que faz |
| --- | --- |
| `SET REDSTONE 'Gate' IN` | Lê o sinal ao lado da interface. |
| `SET REDSTONE 'Gate' OUT 15` | Emite o sinal mais forte, 15. |
| `SET REDSTONE 'Gate' OUT 0` | Não emite sinal. |

A resposta cita a interface e o que ela fez, como "Interface de Redstone Gate emite 15". Coloque o nome entre aspas simples quando ele tiver espaço.

## Se algo der errado

> [!PROBLEM] A resposta diz "nenhuma Interface de Redstone chamada Gate está ligada a esta máquina".
> O nome está escrito de outra forma, ou a interface não está ligada a este computador. Confira o nome na janela dela e o cabo.

> [!PROBLEM] A resposta diz "SET REDSTONE precisa de IN, ou de OUT e uma força de 0 a 15".
> Você escreveu OUT sem uma potência. Acrescente um número de 0 a 15.

> [!PROBLEM] A resposta diz "não é uma força de redstone, que vai de 0 a 15: 16".
> Use uma potência de 0 a 15.

> [!PROBLEM] A resposta diz "SET REDSTONE precisa do nome da interface, entre aspas quando tem espaços".
> Escreva o nome logo após SET REDSTONE, entre aspas simples quando ele tiver espaço.

> [!PROBLEM] A interface não lê nem emite nada.
> O cabo dela não tem computador na outra ponta. Ligue a interface a um computador.

> [!SEE] jsc:redstone_interfaces, jsc:peripheral_cables, jsc:set_redstone, jsc:where_to_type_iql
