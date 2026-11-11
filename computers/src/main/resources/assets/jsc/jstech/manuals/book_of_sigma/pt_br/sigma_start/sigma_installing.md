---
section: sigma_start
order: 406
---
# Instalando os compiladores e o runtime

O Sigma e o Sigma# não vêm com o computador. Você instala três pacotes: o Compilador Σ (scc), o Compilador Σ# (sgsc) e o Sigma Runtime (sigma). Um pacote é instalado pelo seu nome, do mesmo jeito que todo programa ([](jsc:programs)).

> [!DEFINE] Compilador Σ
> O pacote scc, que transforma o seu código Sigma em uma listagem.

> [!DEFINE] Compilador Σ#
> O pacote sgsc, que transforma o seu código Sigma# em uma listagem.

> [!DEFINE] Sigma Runtime
> O pacote sigma, que executa as suas listagens no computador.

O compilador Sigma é o menor dos três. É o único que um computador Vintage consegue comportar, porque o compilador Sigma# e o runtime exigem um computador Legacy ou posterior.

Table: Os três pacotes

| Pacote | Nome | Era mais antiga | Disco e memória |
| --- | --- | --- | --- |
| `scc` | Compilador Σ | Vintage | 4 MB de disco, 2 MB de memória enquanto roda |
| `sgsc` | Compilador Σ# | Legacy | 8 MB de disco, 16 MB de memória enquanto roda |
| `sigma` | Sigma Runtime | Legacy | 12 MB de disco, 24 MB de memória enquanto roda |

No Frames, a edição também importa. O Frames 95 só consegue instalar o compilador Sigma: o compilador Sigma# e o runtime exigem o Frames XP ou posterior.

Os pacotes vêm do Mirror, o repositório de pacotes da rede. Ele é um serviço de um Mainframe Standard, então o seu computador precisa estar em uma rede cujo Mainframe execute o Mirror ([](jsc:mirror)).

Table: Instalando a partir do Mirror

| Sistema | Comando a digitar |
| --- | --- |
| Frames | `pckmgr install <name>` |
| Debian e Ubuntu | `apt install <name>` |
| Fedora | `dnf install <name>` |
| Arch | `pacman -S <name>` |
| Gentoo, que compila cada pacote a partir do código-fonte | `emerge <name>` |
| FreeBSD | `pkg install <name>` |

O nome é o próprio nome do pacote, como scc ou sgsc. No MC-DOS, no MC-NET e no UNIX System V, os programas são instalados apenas a partir de mídia de instalação, não do Mirror ([](jsc:programs)).

## Instalando no Frames

1. Abra o Prompt de Comando.
2. Digite pckmgr install scc e pressione [[Enter]].
3. Digite pckmgr install sgsc e pressione [[Enter]].
4. Digite pckmgr install sigma e pressione [[Enter]].

Quando um pacote está instalado, o seu comando aparece no prompt. O prompt é a linha onde você digita um comando, e o sistema responde ali. Cada palavra de comando é um verbo: uma palavra que o sistema conhece, como install ou run.

> [!DEFINE] prompt
> A linha onde você digita um comando, e onde o sistema responde.

> [!DEFINE] verbo
> Uma palavra de comando que o sistema conhece, como install ou run.

Enquanto um pacote não estiver instalado, os seus verbos não existem no prompt. O runtime acrescenta o verbo sigma, e a ferramenta de empacotamento sgpack vem junto ([](jsc:sigma_sharing)).

## Se algo der errado

> [!PROBLEM] Aparece "sgsc precisa de hardware Legacy ou posterior".
> Instale o compilador Sigma, scc, neste computador Vintage. Ou use um computador Legacy ou posterior.

> [!PROBLEM] Aparece "não foi possível localizar o pacote sgsc".
> O Mirror não oferece o pacote a este computador. Verifique se um Mainframe executa o Mirror. No Frames, procure o nome com pckmgr search.

> [!PROBLEM] Aparece "não foi possível resolver mirror:// - conecte este computador a uma rede cujo Mainframe execute o serviço Mirror".
> Conecte este computador a uma rede cujo Mainframe execute o serviço Mirror.

> [!PROBLEM] Aparece "este sistema instala programas a partir de mídia de instalação, não de um gerenciador de pacotes".
> Use Frames, Linux ou FreeBSD para este pacote, já que eles têm um gerenciador de pacotes.

> [!PROBLEM] Aparece "requisitos não atendidos (hardware ou espaço livre em disco)".
> O computador não tem a era ou o espaço livre em disco que o pacote exige. Libere espaço em disco, ou use um computador de uma era mais nova.

> [!PROBLEM] Aparece "este verbo precisa de um nome de pacote" depois de pckmgr install.
> Digite o nome do pacote depois de install, por exemplo pckmgr install scc.

> [!SEE] jsc:programs, jsc:mirror
