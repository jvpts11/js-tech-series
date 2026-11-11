---
section: openbsd_system
order: 344
---
# O boot, e os daemons que o rcctl liga e desliga

O OpenBSD inicia pelo seu prompt de boot, boot>. Ele espera cinco segundos sem contar e então inicia o sistema; aperte qualquer tecla para parar a espera, e Enter ou boot para iniciar. reboot reinicia o computador.

> [!DEFINE] Daemon
> Um programa que roda em segundo plano, sem janela própria.

O rcctl é como você liga e desliga os daemons. enable e disable dizem se um daemon inicia com o sistema; start e stop agem sobre ele agora. Um serviço que você instala vem desligado, então você o liga antes de ele rodar.

Table: Os daemons

| Daemon | O que faz |
| --- | --- |
| `cron` | Roda as tarefas agendadas. Ligado por padrão. |
| `sshd` | Deixa outros computadores alcançarem este. |
| `pf, pflogd, syslogd` | O firewall, o seu log e o log do sistema. Sempre rodam. |
| `xenodm` | Inicia a área de trabalho, quando há uma instalada. Desligado por padrão. |

1. Digite rcctl ls on para ver o que está ligado.
2. Digite rcctl enable xenodm para iniciar a área de trabalho com o computador daqui em diante.
3. Digite rcctl start xenodm para subi-la agora.

O cron e o sshd são chaves do próprio computador, então parar um deles o desliga de vez e iniciar um o liga. Um daemon desligado só inicia com rcctl -f start.

```command
id: jsc:command/openbsd/rcctl
```

## Se algo der errado

> [!PROBLEM] O rcctl diz que o serviço não existe.
> Digite rcctl ls all para ver os daemons que o computador tem. O xenodm só existe com uma área de trabalho.

> [!PROBLEM] O rcctl diz para usar -f para forçar o início.
> O daemon está desligado. Digite rcctl enable e o nome dele antes.

> [!SEE] jsc:openbsd_overview, jsc:processes_and_jobs
