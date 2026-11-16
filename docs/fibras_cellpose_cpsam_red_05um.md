# Fibras com modelo cpsam no canal Red

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_cpsam_red_05um.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `beatriz_fibras.groovy`  
**Arquivo no acervo:** `fibras_cellpose_cpsam_red_05um.groovy`

## O que faz

Seleciona todas as anotações, executa Cellpose e adiciona medidas de forma às detecções.

## O que precisa

QuPath com uma imagem aberta. Extensão Cellpose e ambiente/modelo correspondente configurados. Anotações de região já existentes.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Modelo cpsam; canal Red; pixelSize 0.5. Medidas: área, comprimento, circularidade, solidez, diâmetros máximo/mínimo e razão núcleo/célula.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Detecções e medidas na hierarquia da imagem; não exporta CSV ou imagens.

## Particularidades para reutilizar

A seleção inicial inclui todas as anotações, não apenas as que estavam selecionadas. useCellposeSAM está comentado: o nome cpsam sozinho não ativa essa chamada no código. O bloco IntensityFeaturesPlugin tem todas as opções de medidas desativadas. Opções comentadas não participam do processamento.
