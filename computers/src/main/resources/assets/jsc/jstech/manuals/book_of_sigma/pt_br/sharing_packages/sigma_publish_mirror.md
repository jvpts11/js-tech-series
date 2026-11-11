---
section: sharing_packages
order: 495
---
# Publicando um pacote no Mirror

Publicar coloca um pacote compilado no [o Mirror](jsc:mirror), o serviço de pacotes que fica no Mainframe da rede ([](jsc:mainframes)). Depois que ele está lá, qualquer computador da rede pode instalá-lo pelo nome.

O Mirror só serve pacotes quando está instalado no Mainframe e o Mainframe está rodando. Um Mainframe desligado mantém o Mirror instalado, mas não serve nada até ser ligado de novo.

Table: Os verbos do sgpack

| Verbo | O que faz |
| --- | --- |
| `init [name]` | Escreve um package.cpk para preencher. |
| `build` | Compila o pacote que o manifesto descreve. |
| `publish [file]` | Coloca no Mirror da rede. |
| `unpublish <name>` | Retira do Mirror. |

## Publicando um pacote compilado

1. Compile o pacote, para que o seu arquivo .cpk fique na pasta.
2. Digite sgpack publish e pressione [[Enter]]. O comando publica o pacote que o manifesto descreve.
3. Para publicar outro arquivo, digite sgpack publish e o nome dele, como mypkg-1.0.0.cpk.

Uma publicação bem-sucedida responde publicado mypkg 1.0.0 no Mirror, com o nome e a versão. Publicar um nome que já existe responde substituído mypkg 1.0.0 no Mirror, e a nova cópia toma o lugar da antiga em um único passo.

O Mirror guarda até 64 pacotes por rede. Um nome novo é recusado nessa contagem. Substituir um nome que você já publicou não conta para o limite.

O Mirror guarda o texto inteiro de cada pacote, e não registra quem o publicou. Uma publicação bem-sucedida também concede a conquista Ship It ([](jsc:achievements_sigma)), que pede para você empacotar um programa com sgpack e publicá-lo no Mirror.

Publish confere o pacote antes de enviá-lo. Ele informa apenas o primeiro problema que encontra, então corrija essa linha e publique de novo.

## Se algo der errado

> [!PROBLEM] Publish diz não foi possível resolver mirror:// - conecte este computador a uma rede cujo Mainframe rode o serviço Mirror.
> Conecte este computador a uma rede cujo Mainframe rode o Mirror, e ligue esse Mainframe ([](jsc:mirror)).

> [!PROBLEM] Publish diz mypkg-1.0.0.cpk: isto não é um pacote (compile um com 'sgpack build').
> Execute sgpack build primeiro, e depois publique o arquivo .cpk que ele cria.

> [!PROBLEM] Um nome novo é recusado: o Mirror está cheio (64 pacotes).
> Retire do Mirror um pacote de que você não precisa mais ([](jsc:sigma_unpublish)).

> [!PROBLEM] Publish diz informe o pacote a publicar, ou execute isto ao lado de um package.cpk.
> Informe o nome do arquivo, como sgpack publish mypkg-1.0.0.cpk, ou execute o comando na pasta que guarda o manifesto.

> [!SEE] jsc:sigma_unpublish, jsc:sigma_sharing_errors, jsc:mainframe_services
