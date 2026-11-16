# Colágeno total em regiões muscle_picro

[← README](../README.md) · [Script](../scripts/colageno/colageno_total_regioes_muscle_picro.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `col_total.groovy`  
**Arquivo no acervo:** `colageno_total_regioes_muscle_picro.groovy`

## O que faz

Define BRIGHTFIELD_OTHER e vetores H-DAB, cria anotações com muscle_picro, seleciona a classe Negative e aplica collagen_total nessas regiões.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle_picro`, `collagen_total`.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle_picro: mínimo 1500; collagen_total: mínimo 3; buracos 0 em ambos.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações dos classificadores na imagem atual; não calcula percentuais nem exporta CSV.

## Particularidades para reutilizar

A segunda etapa usa Negative, não Positive. Os nomes e o significado dessas classes dependem dos modelos do projeto. A mudança de tipo de imagem e de vetores de cor faz parte do processamento.
