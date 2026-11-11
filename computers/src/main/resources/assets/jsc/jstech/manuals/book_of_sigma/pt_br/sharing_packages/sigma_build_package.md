---
section: sharing_packages
order: 494
---
# Gerando um pacote

Gerar transforma o manifesto e os seus arquivos em um único arquivo de pacote. Nada é compilado nesta etapa: a listagem que o manifesto cita precisa já existir, criada pelo compilador ([](jsc:sigma_compile)). A geração só lê os seus arquivos e os junta.

Um pacote gerado é um arquivo de texto, com o nome do pacote, como mypkg-1.0.0.cpk. Ele começa com a linha ".pkg 1", depois vêm as linhas do manifesto e, em seguida, cada arquivo em um bloco que começa com três traços seguidos do nome do arquivo. Como é texto puro, você pode ler todas as linhas antes de instalar.

A geração marca qualquer linha de um arquivo que começa com três traços ou com uma barra invertida, para que o leitor não a confunda com o início de outro arquivo. O leitor remove a marca de novo, então o seu arquivo volta exatamente como estava.

## Gerando o pacote

1. Confirme que o package.cpk e todos os arquivos que ele cita estão na pasta.
2. Digite sgpack build e pressione [[Enter]].
3. Leia a linha que a geração exibe.

Uma geração bem-sucedida exibe uma linha como gerado mypkg-1.0.0.cpk (2 arquivos, 512 bytes): o nome do arquivo, quantos arquivos ele contém e o tamanho do texto em caracteres. O arquivo gerado fica na mesma pasta, ao lado do manifesto, para que você possa publicá-lo logo em seguida.

## Nomes de arquivos dentro de um pacote

Cada arquivo de um pacote mantém um nome que fica dentro da pasta do próprio pacote: nomes simples separados por barras, sem partes . ou .., sem barra invertida e sem dois-pontos. O jogo recusa qualquer outro nome.

## Se algo der errado

> [!PROBLEM] A geração informa que não há package.cpk aqui; execute 'sgpack init' antes.
> Execute sgpack init nesta pasta, ou vá até a pasta que contém o seu manifesto.

> [!PROBLEM] A geração informa que um arquivo é citado no manifesto, mas não está aqui.
> Copie esse arquivo para a pasta, ou remova a linha de arquivo dele do package.cpk.

> [!PROBLEM] A compilação diz package.cpk: files: e um nome, e depois "não é um nome de arquivo que um pacote pode carregar; ele precisa ficar dentro da pasta do próprio pacote".
> Renomeie o arquivo para um nome simples, como tools/run.asm, e ajuste a linha de arquivo dele para corresponder.

> [!SEE] jsc:sigma_publish_mirror, jsc:sigma_sharing_errors
