# Anotações pelo classificador c4d_final

[← README](../README.md) · [Script](../scripts/classificadores/anotacoes_pixel_classifier_c4d.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `c4d_script.groovy`  
**Arquivo no acervo:** `anotacoes_pixel_classifier_c4d.groovy`

## O que faz

Executa uma única chamada de criação de anotações a partir do classificador de pixels c4d_final.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `c4d_final`.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Classificador c4d_final; área mínima 20.0; área mínima de buracos 0.0.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações na imagem atual, com as classes definidas no classificador; não exporta arquivos.

## Particularidades para reutilizar

Não configura tipo de imagem, seleção ou calibração. Prepare a imagem e a região de aplicação antes de executar. O nome do classificador é uma dependência do projeto; o modelo não está incluído neste acervo.
