---
section: iql_verbs
order: 372
---
# ANALYZE, VACUUM e REINDEX

A rede mantém um índice: o seu registro do que os discos guardam. ANALYZE, VACUUM e REINDEX cuidam desse índice. Cada um pode receber uma palavra depois, como o nome de um objeto, e a rede então ignora essa palavra.

> [!DEFINE] índice
> O registro da rede sobre o que os seus discos guardam. Os verbos de manutenção o mantêm atualizado.

Você roda a manutenção no Mainframe. Digitada em qualquer outro computador, ela é recusada. Ela não precisa de motor.

Table: Os três verbos de manutenção

| Verbo | O que faz, e a sua resposta |
| --- | --- |
| `ANALYZE` | Reconcilia o índice. A resposta é "ANALYZE concluído - <n> tipos reconciliados", com o número de tipos de item que ele guarda. |
| `VACUUM` | Libera as entradas fantasmas. A resposta é "VACUUM liberou <n> entradas fantasmas", com o número liberado. |
| `REINDEX` | Reconstrói o índice a partir dos discos. A resposta é "REINDEX iniciado - reconstruindo o catálogo a partir dos discos", e o novo índice substitui o antigo um pouco depois. |

## Se algo der errado

> [!PROBLEM] a manutenção só é executada no Mainframe
> Digite o comando no próprio Mainframe.

> [!PROBLEM] a rede não tem um Mainframe em execução
> Coloque um Mainframe na rede e deixe-o rodar ([](jsc:mainframes)).

> [!PROBLEM] token inesperado: <word>
> Um verbo de manutenção aceita no máximo uma palavra. Remova a palavra extra.

> [!SEE] jsc:mainframes, jsc:iql_limits
