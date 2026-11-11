---
section: linux_differences
order: 294
---
# O que um Linux aqui não tem, e por quê

Muitos comandos que você pode conhecer de outros sistemas Linux não estão no shell daqui. A tabela lista os mais comuns e o que fazer em vez deles. O shell ainda tem reboot, que reinicia o computador.

Nada neste mundo faz login com senha ainda, então um passo que definisse uma senha não definiria nada. Todo comando roda como o usuário player, e não há outro usuário para trocar.

Table: Comandos que você pode esperar

| Comando | O que fazer aqui |
| --- | --- |
| `sudo` | Não é necessário: instale com apt install ou dnf install como player. |
| `su, passwd, useradd, usermod, adduser` | Não há outros usuários. Todo comando roda como player. |
| `chmod, chown` | O shell não mantém permissões de arquivo a alterar. |
| `systemctl` | Não há comando service. Um serviço começa quando o seu programa é instalado. |
| `journalctl` | Não está no shell. |
| `ip, ifconfig` | Não está no shell. O capítulo do shell ([](jsc:shell)) lista os comandos que existem. |
| `shutdown` | Não está no shell. Use reboot para reiniciar o computador. |

Os comandos de pacote não precisam de sudo. apt install e dnf install rodam diretamente para player ([](jsc:apt), [](jsc:dnf)).

Quando um comando falta, o shell diz "comando não encontrado" com a palavra que você digitou, e acrescenta uma linha discreta: "digite 'help' para listar os comandos".

## Se algo der errado

> [!PROBLEM] comando não encontrado: sudo
> Não é necessário aqui. Instale com apt install ou dnf install.

> [!PROBLEM] comando não encontrado: systemctl
> Não há comando service. Um serviço é instalado, e o Mirror é colocado no Mainframe com mirror install.

> [!PROBLEM] comando não encontrado: chmod
> Não há permissões de arquivo a alterar neste mundo.

> [!SEE] jsc:apt, jsc:dnf
