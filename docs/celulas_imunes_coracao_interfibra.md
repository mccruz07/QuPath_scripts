# Células imunes em regiões de fibras cardíacas

[← README](../README.md) · [Script](../scripts/imunidade/celulas_imunes_coracao_interfibra.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Larissa_immune_cells.groovy`  
**Arquivo no acervo:** `celulas_imunes_coracao_interfibra.groovy`

## O que faz

Define H&E, cria tecido, aplica inter_fibra em Positive e detecta células nas anotações fibers por watershed. Mede forma/intensidade e aplica redondas2.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `coracao`, `inter_fibra`, `redondas2`.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

coracao: mínimo 1000. inter_fibra: mínimo 250; watershed em Hematoxylin OD, resolução 0.5 µm, área 10–400 µm², limiar 0.2 e expansão 2 µm; classificador de objetos redondas2.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações, células classificadas e medidas na hierarquia; não exporta arquivos.

## Particularidades para reutilizar

Limpa todos os objetos no início.
