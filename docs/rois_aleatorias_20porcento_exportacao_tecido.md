# ROIs aleatórias para 20% do tecido e exportação

[← README](../README.md) · [Script](../scripts/regioes/rois_aleatorias_20porcento_exportacao_tecido.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `raissa_crop.groovy`  
**Arquivo no acervo:** `rois_aleatorias_20porcento_exportacao_tecido.groovy`

## O que faz

Cria tecido com raissa_tissue, escolhe a primeira anotação e estima quantos quadrados de 2048×2048 pixels cobrem 20% de sua área. Tenta gerar retângulos sem sobreposição subtraindo cada um da geometria disponível e atribui classes Tissue/ROI.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `raissa_tissue`. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Fração 0.2; quadrados 2048×2048; quantidade arredondada; exportação downsample 1.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Cria anotações ROI e arquivos image_export/<imagem>/<imagem>region <índice>.tif.

## Particularidades para reutilizar

O laço de exportação percorre todas as anotações, mas sempre usa anno.getROI(), a ROI original: os arquivos podem repetir o mesmo recorte do tecido em vez dos quadrados. A região base é a primeira anotação, não uma classe buscada por nome. O intervalo 1..num_ROIs precisa de tratamento quando a quantidade calculada é zero. A geração não define semente aleatória. O exportador usa caixa delimitadora.
