# draw_regions — transferência da ROI Cortex

[← README](../README.md) · [Código](../scripts/draw_regions.groovy)

## Finalidade e pré-requisitos

Copiar a geometria de uma anotação da imagem atual para todas as outras imagens do projeto. Apesar do nome do arquivo, o script não desenha interativamente a região: ela precisa existir antes. Requer projeto aberto, imagem atual pertencente ao projeto e uma anotação de área válida.

## Parâmetros

| Constante | Padrão | Efeito |
| --- | --- | --- |
| ROI_NAME | Cortex | Nome da anotação fonte e das cópias |
| ROI_CLASS | Cortex | Classe da anotação fonte e das cópias |
| SKIP_IF_EXISTS | true | Pula imagem com anotação de nome ou classe Cortex |

## Seleção da região fonte

O código usa `getAnnotationObjects().last()`. Ele não usa a anotação selecionada nem verifica o horário de criação. O comentário recomenda desenhar a ROI por último, mas a ordem da coleção precisa ser conferida; para o primeiro uso, uma imagem fonte com apenas a ROI pretendida evita ambiguidade.

## Uso

1. Prepare uma cópia do projeto e abra a imagem de referência.
2. Confirme qual anotação será a última na coleção e que representa Cortex.
3. Ajuste os parâmetros e execute **uma vez na referência**, sem executar novamente para cada imagem do projeto.
4. Confira no Log as quantidades copiadas, puladas e com falha.
5. Abra as imagens de destino e revise posição, escala e plano da ROI.

## Alterações e saídas

A fonte é renomeada e reclassificada e salva explicitamente por `sourceEntry.saveImageData`. Cada destino elegível recebe uma cópia da geometria, tem sua hierarquia resolvida e é salvo por `entry.saveImageData`. Não há CSV ou figura exportada.

As coordenadas X/Y em pixels são copiadas diretamente, sem transformação. A ROI de destino usa `ImagePlane.getDefaultPlane()`, não necessariamente o Z/T da fonte. Diferença nas dimensões das imagens gera aviso, mas **não impede a cópia**. Diferenças de calibração, rotação e deslocamento não são corrigidas. Imagens com dimensões iguais também podem estar desalinhadas.

Com `SKIP_IF_EXISTS=true`, qualquer Cortex existente faz a imagem ser pulada; não há verificação de equivalência da ROI. Com `false`, adiciona outra ROI, sem substituir a anterior, podendo criar duplicatas incompatíveis com a análise que exige exatamente uma Cortex.

## Validação

Teste em projeto com três imagens: referência, destino vazio e destino com Cortex. Confira geometria, classe e persistência após reabrir o projeto. Teste separadamente imagem com dimensões diferentes. A contagem de puladas inclui a própria referência. Falhas por imagem são registradas e o laço continua, sem desfazer imagens já salvas.
