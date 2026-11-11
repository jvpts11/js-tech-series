---
section: operations
order: 139
icon: jsc:mainframe
---
# Motores

O que lê o IQL e planeja o trabalho da rede é o motor. Todo Mainframe novo vem com o Midsoft IQL Server, instalado e rodando. Outros motores também podem ser instalados no Mainframe, e cada um fala a sua própria versão do IQL, com os seus próprios extras.

Table: Os motores

| Motor | O que oferece | Versões por era |
| --- | --- | --- |
| Midsoft IQL Server | IQL, com visões e procedimentos. | Vintage 4.2, Legacy 2000, Transition 2008, Standard 2012, Advanced 2022 |
| NextgreIQL | Mostra o plano que fez e como rodou, aceita dicas que mudam o plano e aceita extras de outros mods. | Legacy 7.0, Transition 8.3, Standard 9.0, Advanced 16 |
| Prophet YourIQL | Você declara o estado que a rede mantém; ela calcula as Operations para mantê-lo e responde às assinaturas. | Legacy 3.23, Transition 5.0, Standard 5.6, Advanced 8.0 |

O NextgreIQL e o Prophet YourIQL vêm a partir da era Legacy. O Prophet funciona de forma diferente dos outros: você diz qual estado a rede deve manter, e ele faz o trabalho para mantê-lo. O livro Aprendendo IQL explica cada motor ([](jsc:iql_engines_overview)).

Um programa indica o motor de que precisa. O IQL Server Management Studio precisa do Midsoft IQL Server, versão 2000 ou mais recente. O Nextgre Planner Studio precisa do NextgreIQL, e o Prophet Reactive Console precisa do Prophet YourIQL, versão 3.23 ou mais recente.

Trocar um motor leva pelo menos 2 segundos, e até 2 minutos. Uma rede com mais tipos de item leva mais tempo. Enquanto o motor é substituído, o trabalho já em andamento continua, e novos planos são recusados.

Sem um motor funcionando, só as transferências continuam, como mover itens com SELECT, INSERT, MOVE e DELETE. CRAFT, QUERY e UPDATE são recusados.

Quando um programa diz que precisa de um motor, você instala esse motor no Mainframe, ou o inicia se ele estiver parado ([](jsc:engine_services)).

## Se algo der errado

> [!PROBLEM] Um programa diz que não encontrou nenhum motor compatível nesta rede.
> O motor está ausente ou parado neste Mainframe. Instale ou inicie-o ([](jsc:engine_services)).

> [!PROBLEM] O resultado diz que o Network Operations Service está indisponível.
> Um motor está sendo substituído. Espere a substituição terminar. O trabalho já em andamento continua.

> [!PROBLEM] Um programa diz que precisa de um motor que oferece um recurso que a rede não tem.
> O motor deste Mainframe não tem esse extra. Use um motor que o tenha ([](jsc:engine_problems)).

> [!SEE] jsc:iql, jsc:automation_engine
