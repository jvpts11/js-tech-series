---
section: gentoo_system
order: 324
---
# O fstab feito à mão

A tabela de sistemas de arquivos, /etc/fstab, diz ao sistema quais partições montar na inicialização ([](jsc:arch_fstab)). A base começa com apenas um cabeçalho, então você mesmo escreve a linha raiz.

A linha raiz identifica a partição raiz pelo seu identificador universalmente único (UUID). O UUID pertence a uma só partição, e permanece o mesmo quando os discos são reordenados ([](jsc:filesystems_and_mkfs)).

```steps
{"steps": ["Digite blkid >> /etc/fstab e pressione [[Enter]].", "Digite nano /etc/fstab e pressione [[Enter]].", "Encontre a linha da partição raiz, e reduza-a a UUID=<uuid dela>  /  ext4  noatime  0 1.", "Salve o arquivo e feche o nano."]}
```

Table: Os campos da linha raiz

| Campo | Valor | O que significa |
| --- | --- | --- |
| Sistema de arquivos | `UUID=<its uuid>` | A partição raiz, pelo seu identificador. |
| Ponto de montagem | `/` | A pasta raiz do sistema. |
| Tipo | `ext4` | O sistema de arquivos com que a partição é formatada. |
| Opções | `noatime` | Não registra quando cada arquivo foi lido pela última vez. |
| Dump | `0` | Não incluído no backup da ferramenta dump. |
| Pass | `1` | Verificado primeiro na inicialização. |

Ao reiniciar, a máquina procura uma linha cujo ponto de montagem seja /. Ela precisa dessa linha para encontrar a raiz, então o reinício verifica a linha antes de começar.

## Se algo der errado

> [!PROBLEM] O reinício diz "nenhum sistema de arquivos raiz em /etc/fstab".
> Nenhuma linha do arquivo monta /. Escreva a linha do root como mostrado acima, e depois reinicie de novo ([](jsc:gentoo_reboot)).

> [!SEE] jsc:arch_fstab, jsc:gentoo_reboot
