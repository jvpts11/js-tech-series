---
section: sigma_start
order: 411
---
# Projetos e versões

Um projeto mantém juntos os arquivos de um programa, com as configurações que o compilam. Ao criar um projeto, as suas configurações ficam no arquivo de projeto, um arquivo de texto na pasta do projeto. O arquivo do Sigma# se chama NAME.sgsproj, e o arquivo do Sigma se chama NAME.sgproj.

Cada linha do arquivo guarda uma configuração, como uma chave, dois-pontos e um valor. Uma linha escrita de outro jeito é ignorada.

Table: O que um arquivo de projeto guarda

| Chave | O que guarda |
| --- | --- |
| `name` | O nome do projeto. |
| `kind` | Um de console, script, library ou empty. |
| `language` | jsc:sigma para o Sigma, ou jsc:sigma_sharp para o Sigma#. |
| `source` | Uma linha para cada arquivo-fonte, em relação à pasta do projeto. |
| `reference` | Uma linha para cada projeto de biblioteca que ele usa. |
| `entry` | A listagem que ele compila, como build/NAME.asm. Vazio para uma biblioteca. |
| `platform` | O conjunto de instruções para o qual ele compila, como jsc:x86. |
| `langversion` | A versão da linguagem em que ele está em uso. Escrita apenas quando definida. Sem ela, o projeto segue o compilador instalado. |

O tipo diz ao compilador o que compilar. Um programa de console roda e termina. Um script fica ativo. Uma biblioteca guarda classes compartilhadas e não tem início próprio. Um projeto vazio ainda não tem nada dentro.

Uma solução mantém os seus projetos juntos, uma pasta para cada projeto. O arquivo dela lista cada projeto pelo caminho, e informa o projeto de inicialização, aquele que Start executa. Quando o projeto de inicialização está vazio, Start usa o primeiro projeto que pode rodar.

Um projeto compila para um conjunto de instruções, que você escolhe na janela chamada Platform target.

> [!DEFINE] langversion
> A versão da linguagem em que um projeto está em uso. A versão 1 é a primeira linguagem, e a versão 2 é a atual.

> [!DEFINE] em uso
> A versão que uma compilação realmente usa: a menor entre a langversion do projeto e a versão do compilador instalado.

O Sigma e o Sigma# compartilham os seus números de versão. O Sigma 2 é o subconjunto do Sigma# 2, então um projeto tem um único número para os dois.

## Definindo a versão da linguagem

1. Abra a janela Propriedades do projeto no Virtual Studio.
2. Encontre a linha Versão da linguagem.
3. Pressione o botão Padrão para acompanhar o compilador instalado, ou pressione um botão numerado para manter o projeto nessa versão.

Abaixo dos botões, o Virtual Studio explica a escolha. "Padrão acompanha o compilador instalado (sgsc 2.0)." é a primeira dica. "Uma versão mais baixa recusa o que veio depois dela:" é a segunda. Depois que você escolhe um número, a janela diz "Escrito no projeto como langversion" com esse número.

Escolher Padrão remove a linha langversion do arquivo de projeto. A sua compilação então usa a versão do compilador instalado. Com um número, a sua compilação usa a menor entre esse número e a versão do compilador instalado.

Na linha de comando o arquivo de projeto não é lido. A opção --lang define a versão para uma única compilação ([](jsc:sigma_compile)). O comando sigma run em um arquivo-fonte usa a versão mais nova, e não a do projeto.

A versão 1 é a primeira versão. A versão 2 acrescentou os tipos para a rede, as máquinas e as janelas, como Bus, CraftInterface, Table e Chart. Acrescentou membros a tipos antigos, como Console.Read e File.Move. Também permite que Main devolva um int e receba os argumentos, e o Sigma menor ganhou Random.

## Se algo der errado

> [!PROBLEM] Aparece "'puts' precisa de Σ# 2; este projeto é Σ# 1".
> Defina a Versão da linguagem do projeto como Padrão, ou para a versão 2.

> [!PROBLEM] Aparece "este compilador conhece o Σ# 1 no máximo; o Σ# 2 precisa de um mais novo".
> Atualize o pacote do compilador, ou use uma versão mais baixa depois de --lang.

> [!PROBLEM] Aparece "'3' não é uma versão do Σ#; há de 1 a 2".
> Use 1 ou 2 depois de --lang.

> [!SEE] jsc:sigma_sharing, jsc:sigma_compile
