---
section: engine_choice
order: 383
icon: jsc:mainframe
---
# O que é um motor

Quando você digita uma instrução IQL, alguma coisa precisa lê-la e decidir como a rede faz o trabalho. Na rede, essa coisa é um motor. Ele roda como um programa no Mainframe ([](jsc:mainframes)), lê a instrução, planeja os passos e entrega o plano à parte da rede que executa o trabalho.

> [!DEFINE] Motor
> Um programa no Mainframe que lê IQL, planeja como a rede faz o trabalho e repassa o plano. Somente um programa instalado desse tipo planeja por vez.

> [!DEFINE] Mainframe
> O único computador de uma rede que roda o software próprio da rede, como o motor que planeja o trabalho dela.

Um Mainframe novo vem com o Midsoft IQL Server instalado e rodando, na versão da sua era. Existem mais dois motores: NextgreIQL e Prophet YourIQL. Cada um fala o seu próprio dialeto da linguagem, e cada um oferece extras além do núcleo.

> [!DEFINE] Dialeto
> A variante de IQL que um motor fala. Todo dialeto aceita a linguagem central, então as mesmas instruções centrais funcionam em todos os motores.

Table: Os motores

| Motor | Dialeto | Eras | Extras |
| --- | --- | --- | --- |
| Midsoft IQL Server | IQL | Vintage a Advanced | procedimentos e visões |
| NextgreIQL | NextgreIQL | Legacy a Advanced | explain, dicas do planejador, extensões |
| Prophet YourIQL | YourIQL | Legacy a Advanced | estados declarados, assinaturas |

Você gerencia um motor na aba Services do Gerenciador de Rede ([](jsc:network_manager)), e um programa que precisa de um motor específico avisa isso quando você o abre. Todo motor responde ao mesmo núcleo: transferências, fabricações e as instruções da linguagem central.

Somente um motor planeja por vez, o motor ativo. Quando você o para ou o substitui, as Operações já em andamento continuam com os planos com que começaram. Você ainda pode navegar, depositar e retirar itens sem nenhum motor rodando.

Sem nenhum motor rodando, as transferências continuam funcionando, mas fabricações e consultas IQL são recusadas. Você inicia um motor para planejar de novo.

## Se algo der errado

> [!PROBLEM] Uma fabricação ou uma consulta diz que o Network Operations Service está indisponível.
> Nenhum motor está rodando. Inicie um na aba Services ([](jsc:engine_services)).

> [!SEE] jsc:engines, jsc:engine_services
