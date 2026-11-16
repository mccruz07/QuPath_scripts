# Células imunes com filtro de foco por tiles

[← README](../README.md) · [Script](../scripts/imunidade/celulas_imunes_interfibra_filtro_foco.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Larissa_immune_cells_withQC.groovy`  
**Arquivo no acervo:** `celulas_imunes_interfibra_filtro_foco.groovy`

## O que faz

Define H&E, cria tecido, aplica inter_fibra em Positive e detecta células nas anotações fibers por watershed. Mede forma/intensidade e aplica redondas2. Divide o tecido em tiles de 150 µm e calcula média/desvio de hematoxilina: média >0.15 e desvio <0.05 marcam Desfocado. Encerra se mais de 40% das detecções forem marcadas assim; caso contrário prossegue para as células.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `tissue`, `inter_fibra`, `redondas2`.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

tissue: mínimo 170. inter_fibra: mínimo 250; watershed em Hematoxylin OD, resolução 0.5 µm, área 10–400 µm², limiar 0.2 e expansão 2 µm; classificador de objetos redondas2.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações, células classificadas e medidas na hierarquia; não exporta arquivos.

## Particularidades para reutilizar

A lista usada como tiles é getDetectionObjects(), incluindo detecções preexistentes. A limpeza dos tiles remove todas as detecções nesse ponto. O percentual usa número de objetos, não área. As regiões desfocadas não são subtraídas individualmente do tecido quando a imagem passa pelo limite global.
