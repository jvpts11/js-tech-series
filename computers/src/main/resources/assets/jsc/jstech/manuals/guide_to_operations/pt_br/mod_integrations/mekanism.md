---
section: mod_integrations
order: 160
---
# Mekanism

O Mekanism é um mod com máquinas e substâncias químicas próprias: gases e outras substâncias que as máquinas dele manipulam. Quando o Mekanism está instalado, o J's Computers lê essas substâncias químicas e as trata como dados, do mesmo jeito que trata os fluidos.

Uma substância química circula pela rede e fica nos discos, contada em milibaldes (mB), a mesma unidade de um fluido. Assim, você guarda uma substância química como guarda um fluido, e a linha de uso do disco a mostra como um número de mB de substância química.

A rede nomeia uma substância química pelo seu id de registro, com o prefixo chemical/. O oxigênio do Mekanism, por exemplo, se chama chemical/mekanism:oxygen.

Table: Nomes de substâncias químicas

| Nome | O que nomeia |
| --- | --- |
| `chemical/mekanism:oxygen` | A substância de oxigênio do Mekanism. |
| `chemical/<registry id>` | Qualquer outra substância química, pelo seu id de registro, do mod que a adiciona. |

> [!DEFINE] Químico (Mekanism)
> Um gás ou substância semelhante que o Mekanism trata como um fluido.

O Computers não adiciona nenhuma receita nem máquina própria do Mekanism. A autofabricação funciona com qualquer máquina que ofereça o acesso usual a itens, fluidos, energia e substâncias químicas, de qualquer mod ([](jsc:how_autocrafting_works)).

O buffer de um Gateway de Rede aceita somente itens, então uma substância química puxada por um Gateway não move nada para dentro do seu buffer ([](jsc:computercraft_peripheral)).

## Se algo der errado

> [!PROBLEM] A rede diz "substância química desconhecida:" seguido de um nome.
> O nome não é uma substância química que o jogo conhece. Confira se o Mekanism está instalado e se o nome é chemical/ seguido do id de registro da substância.

> [!PROBLEM] Uma substância química não se move.
> Confira se o Mekanism está instalado. Sem ele, as substâncias químicas são guardadas pelo seu id, mas toda consulta responde que a substância é desconhecida.

> [!SEE] jsc:items_as_data, jsc:how_autocrafting_works
