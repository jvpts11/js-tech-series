---
section: engine_choice
order: 387
icon: jsc:mainframe
---
# Dicas: mudando o plano do NextgreIQL

Uma dica muda como o NextgreIQL planeja um CRAFT. Ao adicionar uma, você escolhe de onde vêm as matérias-primas, quantos passos rodam ao mesmo tempo, e se um passo é feito em uma máquina ou em uma bancada.

Escreva as dicas depois do CRAFT, uma após a outra. O motor as lê no fim da instrução.

> [!DEFINE] Fonte
> Um servidor de armazenamento que guarda matérias-primas para uma fabricação.

Table: As dicas

| Dica | Pega | O que faz |
| --- | --- | --- |
| `PREFER SOURCE 'server'` | O nome de um servidor | Pega primeiro as matérias-primas do servidor nomeado. |
| `AVOID SOURCE 'server'` | O nome de um servidor | Não pega nada do servidor nomeado enquanto outro servidor as tiver. |
| `MAX PARALLEL n` | Um número inteiro | Executa no máximo esse número de passos da fabricação ao mesmo tempo. |
| `PREFER MACHINE` | Nada | Fabrica em uma máquina tudo o que uma máquina e uma bancada fabricam. |
| `PREFER BENCH` | Nada | Fabrica em uma bancada tudo o que uma máquina e uma bancada fabricam. |

Uma fabricação com duas dicas fica assim. O servidor chamado Storage B precisa estar na rede.

```iql hints_example.iql
CRAFT 64 iron_ingot PREFER SOURCE 'Storage B' MAX PARALLEL 2
```

Maiúsculas e minúsculas não fazem diferença.

MAX PARALLEL aceita um número inteiro acima de zero. As dicas vêm só depois de um CRAFT, porque as outras instruções as recusam.

Outros mods podem adicionar dicas próprias.

## Se algo der errado

> [!PROBLEM] A resposta é "nenhum servidor na rede se chama" seguido do nome que você digitou.
> Escreva o nome do servidor como a rede o mostra.

> [!PROBLEM] A resposta é "MAX PARALLEL pede um número inteiro acima de zero".
> Escreva um número inteiro de 1 ou mais depois de MAX PARALLEL.

> [!PROBLEM] A resposta é "as dicas mudam o plano de um CRAFT".
> Remova a dica, ou transforme a instrução em um CRAFT.

> [!SEE] jsc:nextgre_iql, jsc:engine_problems
