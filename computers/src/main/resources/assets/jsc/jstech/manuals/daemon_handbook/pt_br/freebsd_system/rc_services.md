---
section: freebsd_system
order: 335
---
# Serviços na inicialização: cron e sshd

Um serviço é um programa que roda em segundo plano, sem janela própria. O FreeBSD pergunta sobre dois deles, no instalador e em cada inicialização: o cron, que executa tarefas agendadas, e o sshd, que permite que outros computadores alcancem este.

> [!DEFINE] Serviço
> Um programa que roda em segundo plano, como o cron para tarefas agendadas ou o sshd para acesso remoto.

> [!DEFINE] rc
> Os scripts de inicialização do FreeBSD. Eles leem as configurações para decidir quais serviços iniciam.

Table: Os dois serviços

| Serviço | Configuração | O que faz |
| --- | --- | --- |
| `cron` | `cron` | Executa tarefas agendadas. Ligado por padrão. |
| `sshd` | `remote` | Permite que outros computadores alcancem este. |

O serviço sshd e a configuração remote são uma única configuração. Ligar o remote inicia o sshd na inicialização, e a caixa do sshd do instalador define a mesma configuração. A caixa não vem marcada por padrão, então um computador instalado pelo bsdinstall começa com remote desligado, a menos que você a marque.

Um computador que não foi instalado pelo bsdinstall começa com remote ligado.

## Alterando os serviços com config

Você altera as duas configurações com o comando config. Os valores são on e off, e true e false também funcionam.

1. Digite config para listar as configurações.
2. Digite config cron on para ligar o cron.
3. Digite config cron off para desligar o cron.
4. Digite config remote on para deixar outros computadores alcançarem este.
5. Digite config remote off para recusar as chamadas remotas de outros computadores.

Uma alteração bem-sucedida responde com o nome da configuração e a palavra set, como cron set. Digitar config sozinho lista as duas configurações, uma linha para cada uma.

```table
{"caption": "As linhas do config", "heads": ["Linha", "O que significa"], "rows": [[{"literal": "remote  on (other computers may run programs here)"}, {"words": "A configuração remote está ligada."}], [{"literal": "cron  on"}, {"words": "A configuração do cron, mostrada como on ou como off."}]]}
```

```text
{"text": "Sem um valor, config cron responde \"uso: config <chave> <valor>  (ou 'config' para listar)\". Um valor que não seja on ou off é recusado com a mensagem configuração desconhecida: cron. A chave é conhecida, então o valor é que está errado."}
```

## O arquivo rc.conf

Os scripts rc leem /etc/rc.conf na inicialização. No FreeBSD esse arquivo é gerado a partir das suas configurações, e não fica guardado no disco, então altere as configurações com config. Ele tem estas linhas:

```conf rc.conf
hostname="<hostname>"
ifconfig_em0="DHCP"
cron_enable="YES"
sshd_enable="NO"
dumpdev="AUTO"
```

A linha ifconfig_em0 aparece apenas quando o computador está em rede. As linhas do cron e do sshd leem YES ou NO a partir das duas configurações.

## O que uma mudança de serviço faz

Com o cron desligado, as tarefas agendadas ficam esperando em vez de rodar. Uma tarefa que você coloca em segundo plano com & continua rodando. Com o remote desligado, as chamadas remotas de outros computadores para este são recusadas.

## Se algo der errado

> [!PROBLEM] O sshd não iniciou depois da instalação.
> A caixa do sshd ficou desmarcada. Digite config remote on.

> [!PROBLEM] As tarefas agendadas não rodam.
> A configuração do cron está desligada. Digite config cron on.

> [!PROBLEM] Outros computadores não conseguem rodar programas aqui.
> A configuração de remote está desligada. Digite config remote on.

> [!PROBLEM] O computador responde configuração desconhecida: cron.
> O valor não era on nem off. Use config cron on ou config cron off.

> [!SEE] jsc:processes_and_jobs, jsc:system_commands, jsc:bsdinstall
