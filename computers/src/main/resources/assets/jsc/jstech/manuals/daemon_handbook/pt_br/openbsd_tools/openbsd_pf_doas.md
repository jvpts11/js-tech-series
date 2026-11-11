---
section: openbsd_tools
order: 347
---
# O pf e o doas

O pf é o firewall do OpenBSD. Ele sempre roda, as suas regras estão em /etc/pf.conf, e pfctl -si diz há quanto tempo ele roda e o que a sua tabela de estados guarda. O que as suas regras deixam passar e o que barram vem com a segurança da rede.

```conf /etc/pf.conf
# See pf.conf(5) and /etc/examples/pf.conf

set skip on lo

block return    # block stateless traffic
pass            # establish keep-state
```

```command
id: jsc:command/openbsd/pfctl
```

O doas executa um comando como outro usuário, root a não ser que -u nomeie um. A sua única regra, permit persist :wheel em /etc/doas.conf, deixa o grupo wheel fazê-lo, e o root está no grupo wheel. Até haver contas você é root, então o doas executa o que você lhe dá.

doas -C /etc/doas.conf e um comando dizem se as regras o permitem, sem executá-lo.

```command
id: jsc:command/openbsd/doas
```

> [!SEE] jsc:openbsd_daemons
