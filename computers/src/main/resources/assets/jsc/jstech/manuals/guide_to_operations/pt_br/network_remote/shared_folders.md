---
section: network_remote
order: 110
icon: jsc:server
---
# Pastas compartilhadas

Uma pasta do seu computador pode ser compartilhada com os outros computadores da rede. Uma pasta compartilhada é vista pelo caminho dela, que começa com duas barras invertidas, depois o nome do computador, depois o nome do compartilhamento: \\\\computer\\name.

Um compartilhamento recebe o nome da última parte do caminho da pasta, e a raiz de uma unidade, como C:, recebe o nome c. Duas pastas não podem compartilhar um nome, e um computador compartilha no máximo 16 pastas.

Table: Os comandos de compartilhamento

| Comando | O que faz |
| --- | --- |
| `config share <folder> [read\|write]` | Compartilha a pasta. Somente leitura, a menos que você diga write. |
| `config unshare <name>` | Fecha esse compartilhamento. |
| `config` | Lista os compartilhamentos, ou nenhum quando não há nenhum. |
| `config netshare <0-1000>` | Define o netshare do disco do sistema, de 0 a 1000 por mil. |

O compartilhamento de outro computador é acessado pelo caminho dele. O caminho abaixo de um compartilhamento é seguido no computador do dono, exatamente como seria no prompt daquele computador.

## Se algo der errado

> [!PROBLEM] O compartilhamento é recusado, com uma mensagem de que não há essa pasta.
> A pasta não existe. Confira o nome dela e compartilhe uma pasta que exista.

> [!PROBLEM] Outra pasta já está compartilhada com esse nome.
> Duas pastas não podem compartilhar um nome. Cancele antes o compartilhamento da outra com config unshare.

> [!PROBLEM] Este computador já compartilha 16 pastas.
> Cancele o compartilhamento de uma delas antes com config unshare.

> [!PROBLEM] Uma escrita é recusada.
> O compartilhamento foi aberto somente para leitura. Peça ao dono dele que o compartilhe com permissão de escrita.
