---
section: arch_base
order: 310
icon: jsc:cd_rom
---
# Escrevendo a tabela do sistema de arquivos (genfstab)

Um sistema precisa saber qual disco montar, e onde, toda vez que inicia. Essa lista fica em um único arquivo, /etc/fstab, a tabela do sistema de arquivos. A ferramenta genfstab escreve esse arquivo para você, a partir dos discos que você montou.

A maioria das linhas nomeia uma partição pelo seu identificador único universal (UUID), que o sistema de arquivos grava no próprio disco. O UUID permanece o mesmo quando o disco recebe outra letra, então um disco renomeado ainda inicializa. Aqui, o mesmo disco sempre gera o mesmo UUID.

1. Confirme que o sistema base está instalado.
2. Digite genfstab -U /mnt e pressione [[Enter]]. A tabela é impressa, e nada é gravado.
3. Digite genfstab -U /mnt >> /mnt/etc/fstab e pressione [[Enter]]. A tabela é gravada no arquivo.

Imprima a tabela primeiro se quiser conferi-la. A tabela impressa é o mesmo texto que o redirecionamento grava, então o que você vê é o que o arquivo recebe.

Table: As linhas da tabela

| Linha | O que diz |
| --- | --- |
| `# /dev/sda2` | Um comentário: a partição a que a linha se refere. |
| `UUID=` | O UUID da partição, depois a pasta onde ela é montada. |
| `/` | A pasta onde a partição é montada: o topo do sistema. |
| `ext4` | O tipo de sistema de arquivos da partição. |
| `rw,relatime` | As opções: o disco é lido e escrito, e os horários de acesso são registrados de forma mais leve. |

Se você montou a partição de inicialização, uma segunda linha vem depois de uma linha em branco. Ela identifica essa partição pelo seu número de série, o identificador do sistema de arquivos File Allocation Table (FAT), e a pasta onde ela é montada, como /boot.

> [!DEFINE] fstab
> O arquivo em /etc que lista quais partições montar, onde e como, quando o sistema inicia.

> [!DEFINE] UUID
> Um identificador longo que um sistema de arquivos carrega no seu disco, para que o sistema encontre a partição, qualquer que seja o nome do dispositivo.

## Se algo der errado

> [!PROBLEM] A tela mostra "uso: genfstab [opções] root".
> O comando não informa /mnt. Digite genfstab -U /mnt >> /mnt/etc/fstab.

> [!PROBLEM] A tela mostra "==> ERRO: /mnt/etc não existe".
> O pacstrap ainda não foi executado. Instale o sistema base primeiro ([](jsc:arch_base_system)).

> [!SEE] jsc:arch_chroot
