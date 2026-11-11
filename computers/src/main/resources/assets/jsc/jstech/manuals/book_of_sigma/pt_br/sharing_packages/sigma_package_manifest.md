---
section: sharing_packages
order: 493
---
# O que um pacote diz sobre si mesmo

Um pacote é um programa empacotado para outra pessoa. Ele guarda os arquivos do programa e uma breve descrição deles, chamada manifesto. Você pode escrever um manifesto à mão, e o jogo o lê da mesma forma, quem quer que o tenha escrito.

O manifesto é um arquivo de texto simples chamado package.cpk. Ele tem uma coisa em cada linha: uma chave, dois-pontos e depois o valor, como name: shop. O jogo lê as chaves sem diferenciar maiúsculas de minúsculas. Ele ignora uma linha sem dois-pontos e qualquer linha que comece com sinal de cerquilha (#).

Uma chave que o jogo não conhece é ignorada, não recusada. Assim, um manifesto escrito para uma versão posterior ainda é lido aqui, sem as ideias mais novas. Quando você publica um pacote, ele vai para [o Mirror](jsc:mirror), o serviço de pacotes da rede, e o jogo confere o manifesto antes. A linha ram só é registrada: o jogo não recusa um pacote num computador pequeno.

Table: Chaves de um manifesto

| Chave | O que guarda |
| --- | --- |
| `name` | O nome do pacote: letras minúsculas, dígitos e traços, com no máximo 32 caracteres. |
| `version` | Três números separados por pontos, como 1.0.0. Cada número tem de 1 a 4 dígitos. |
| `house` | A software house que o Mirror lista ao lado do pacote. |
| `entry` | A listagem que o pacote executa. Termina em .asm e também precisa ser informada em uma linha de arquivo. |
| `icon` | Um dos oito ícones abaixo. Sem esta linha, o ícone é gear. |
| `ram` | A memória que o pacote pede, em megabytes: um número inteiro de pelo menos 1. |
| `about` | Uma linha opcional sobre o pacote. O Mirror a mostra no lugar do nome e da versão. |
| `file` | Uma linha para cada arquivo que o pacote carrega, com o seu caminho dentro do pacote. |

O ícone é uma de oito imagens que o jogo já tem: gear, chart, box, bell, wrench, flask, eye e clock. Um pacote não pode trazer a sua própria arte.

## Começando um manifesto com sgpack init

Na pasta em que você trabalha, digite sgpack init e um nome. O jogo deixa o nome em minúsculas. Se você não digitar nome, o pacote se chama program. O comando escreve package.cpk nessa pasta.

1. Digite sgpack init shop e pressione [[Enter]].
2. Edite package.cpk e altere as linhas de que você precisar.

Table: O que sgpack init escreve

| Chave | Valor escrito |
| --- | --- |
| `name` | O nome que você digitou, em minúsculas, ou program. |
| `version` | `1.0.0` |
| `house` | O nome do computador, ou unsigned quando ele não tem nome. |
| `entry` | O nome com .asm acrescentado, como shop.asm. |
| `icon` | `gear` |
| `ram` | `1` |
| `file` | O arquivo de entrada, como o único arquivo. |

O comando se recusa a escrever sobre um manifesto que já existe. Ele responde "package.cpk já existe aqui; edite-o, ou apague-o para começar de novo".

> [!DEFINE] Pacote
> Um programa empacotado em um único arquivo de texto, com o seu manifesto e todos os arquivos que ele carrega.

> [!DEFINE] Manifesto
> O arquivo de texto package.cpk que descreve um pacote: seu nome, versão, entrada e arquivos.

> [!DEFINE] Listagem
> O arquivo .asm compilado que o compilador escreve a partir de um arquivo-fonte.

## Se algo der errado

> [!PROBLEM] A compilação diz package.cpk: name: deve conter apenas letras minúsculas, dígitos ou traços; recebeu "My Shop".
> Escreva o nome em minúsculas, com apenas letras, dígitos e traços, como my-shop.

> [!PROBLEM] A compilação diz package.cpk: version: deve ter a forma 1.0.0; recebeu "1.0".
> Escreva três números separados por pontos, como 1.0.0.

> [!PROBLEM] A compilação diz package.cpk: entry: informe a listagem que o pacote executa.
> Escreva o nome do arquivo da listagem na linha de entrada, como shop.asm.

> [!PROBLEM] A compilação diz package.cpk: entry: deve ser uma listagem compilada; recebeu "shop.sgs".
> Compile o código-fonte com o compilador ([](jsc:sigma_compile)) e depois informe o nome do arquivo .asm que ele escreve.

> [!PROBLEM] A compilação diz package.cpk: files: deve incluir a entrada, "shop.asm".
> Adicione uma linha de arquivo com o mesmo nome da entrada.

> [!PROBLEM] A compilação diz package.cpk: icon: deve ser um de gear, chart, box, bell, wrench, flask, eye, clock; recebeu "star".
> Escolha um dos oito ícones: gear, chart, box, bell, wrench, flask, eye ou clock.

> [!PROBLEM] A compilação diz package.cpk: ram: um pacote precisa de pelo menos um megabyte para rodar.
> Escreva 1 ou um número inteiro maior na linha de ram.

> [!PROBLEM] A compilação diz package.cpk: files: um pacote sem arquivos não é um pacote.
> Adicione uma linha de arquivo para cada arquivo que o pacote carrega.

> [!SEE] jsc:sigma_build_package, jsc:sigma_sharing_errors
