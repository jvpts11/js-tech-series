---
section: library_reach
order: 468
---
# Navigation: pilotar a estrutura onde o seu computador está

Um computador construído sobre uma estrutura móvel do J's Core, um navio, um dirigível ou uma carroça, pode pilotá-la. O seu programa diz ao piloto automático da estrutura para onde ir, e lê o que um sensor de voo leria onde o computador está. Você faz as duas coisas pelo Navigation, um tipo embutido do System.Navigation. O Navigation veio com o Sigma# 3.

Table: Leituras do Navigation

| Membro | O que dá |
| --- | --- |
| `Navigation.Aboard` | Sim quando o computador está numa estrutura móvel. |
| `Navigation.Piloted` | Sim enquanto um piloto automático a pilota. |
| `Navigation.X, Navigation.Z` | Onde o computador está no mundo, para leste e para sul, em metros. |
| `Navigation.Altitude` | A que altura está, em metros. |
| `Navigation.Pressure` | A pressão do ar ali, em quilopascais. |
| `Navigation.Speed` | A que velocidade vai, em metros por segundo. |
| `Navigation.Climb` | A que velocidade sobe, em metros por segundo; abaixo de zero quando desce. |
| `Navigation.Pitch, Navigation.Roll` | Quanto o nariz está para cima e quanto ela se inclina para o lado, em graus. |
| `Navigation.Heading` | Para onde aponta, em graus a partir do norte, girando pelo leste. |
| `Navigation.AboveGround, Navigation.ClearAhead` | A que distância para baixo está o chão, e a que distância à frente está o primeiro bloco sólido, em metros, até 64 metros. |

Table: Comandos do Navigation

| Membro | O que faz |
| --- | --- |
| `Navigation.GoTo(x, y, z, speed)` | Voa até esse lugar do mundo e fica parada ali. |
| `Navigation.HoldAltitude(y, speed)` | Mantém essa altura e deixa os outros controles para o leme. |
| `Navigation.FollowRoute(points, speed, loop)` | Voa até cada ponto, um de cada vez: recomeça com loop sim, senão fica parada no último. |
| `Navigation.Dock(x, y, z, speed)` | Atraca na braçadeira de outra estrutura nesse lugar do mundo. |
| `Navigation.Release()` | Desliga o piloto automático. |

Cada comando dá sim quando o computador está numa estrutura para pilotar, e não em terra, onde as leituras são as do lugar onde o computador está, parado. Uma velocidade é o mais rápido que o piloto automático vai, de 0.01 a 100000 metros por segundo; qualquer outra para o seu programa com uma ArgumentOutOfRangeException. Cada leitura custa uma olhada, e cada comando custa o que custa uma escrita.

Uma rota é uma List<double> de pontos, cada um escrito como o seu x, y e z, com até 256 pontos. O Dock dá não quando nenhuma outra estrutura tem uma braçadeira nesse lugar; o piloto automático chega devagar a partir da frente da braçadeira e solta quando as braçadeiras se prendem. Um piloto no leme continua a pilotar, faça o piloto automático o que fizer.

```sgs Patrol.sgs
if (Navigation.Aboard && !Navigation.Piloted) {
    Navigation.HoldAltitude(120, 4);
}
```

> [!DEFINE] Piloto automático
> A parte de uma estrutura móvel que trabalha os motores dela em direção a um lugar, uma altura ou uma braçadeira, para que você não precise segurar o leme.

## Se algo der errado

> [!PROBLEM] Todo comando dá não.
> O computador não está numa estrutura móvel. Construa-o sobre uma e confira o Navigation.Aboard.

> [!PROBLEM] A estrutura não se mexe.
> Ela precisa de motores que a empurrem para onde lhe mandam ir.

> [!SEE] jsc:sensors_library, jsc:signals_library
