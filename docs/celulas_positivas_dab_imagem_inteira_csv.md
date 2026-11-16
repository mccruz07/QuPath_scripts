# Positividade DAB na imagem inteira com resumo CSV

[← README](../README.md) · [Script](../scripts/ihq/celulas_positivas_dab_imagem_inteira_csv.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `positive_cell_detection_whole_image.groovy`  
**Arquivo no acervo:** `celulas_positivas_dab_imagem_inteira_csv.groovy`

## O que faz

Define H-DAB, limpa todos os objetos, cria um retângulo da imagem inteira, executa PositiveCellDetection e conta classes que contêm Negative ou Positive.

## O que precisa

QuPath com uma imagem aberta. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Positividade citoplasmática 0.21, singleThreshold=true; limiares adicionais 0.40/0.60. Parâmetros sem sufixo Microns: backgroundRadius 0, medianRadius 2, sigma 6, minArea 150, maxArea 2500, cellExpansion 20; threshold de detecção 0.06.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Acrescenta linhas em PositiveCellDetection_Threshold_0.21.csv ao lado do projeto: Image, Num Detections, Num Negative, Num Positive, Positive %. O limiar entra no nome do arquivo.

## Particularidades para reutilizar

O percentual divide o número de positivas por todas as detecções, inclusive as sem classe reconhecida. Não há definição de vetores de deconvolução no arquivo. Os parâmetros sem sufixo físico não devem ser interpretados automaticamente como µm. O percentual é formatado pelo locale padrão; vírgula decimal pode conflitar com o separador CSV.
