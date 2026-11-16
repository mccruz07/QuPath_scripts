# Fibras na imagem inteira com escala fixa

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_sam_imagem_inteira_escala_fixa.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Analises.groovy`  
**Arquivo no acervo:** `fibras_cellpose_sam_imagem_inteira_escala_fixa.groovy`

## O que faz

Apaga todos os objetos, cria uma ROI retangular da imagem inteira e executa Cellpose com useCellposeSAM. Filtra detecções por circularidade e razão entre diâmetros e exporta áreas convertidas por uma escala fixa.

## O que precisa

QuPath com uma imagem aberta. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

micronPerPixel = 0.5/239.0; circularidade ≥0.55; aspect ratio ≤2.5; modelo cyto3 com useCellposeSAM; diâmetro 0; simplificação 1.0.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Fiber_Analysis/<imagem>_fibers.csv: ImageName, FiberID, NumberOfFibers, Area_um2, Circularity. Fiber_Analysis/All_Images_Summary.csv recebe ImageName, NumberOfFibers, MeanFiberArea_um2, MeanCircularity.

## Particularidades para reutilizar

O comentário menciona 225 pixels, mas o cálculo efetivo usa 239. A escala é aplicada à medida Area px^2 ou Area, sem alterar a calibração do servidor. Ajuste-a ao conjunto de imagens. Os nomes das medidas usados nos filtros devem corresponder aos produzidos pelo modelo. O arquivo individual é recriado; o resumo recebe novas linhas.
