# Tiles de 320 µm e exportação TIFF

[← README](../README.md) · [Script](../scripts/regioes/exportar_tiles_320um_tecido_retangulo_fixo.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Crop_tiles.groovy`  
**Arquivo no acervo:** `exportar_tiles_320um_tecido_retangulo_fixo.groovy`

## O que faz

Cria anotações com raissa_tissue, acrescenta um retângulo fixo, seleciona todas as anotações e as divide com TilerPlugin. Depois exporta a caixa delimitadora de cada anotação.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `raissa_tissue`. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Retângulo X=35000, Y=20000, largura/altura 1000 pixels, Z=0/T=0. Tiles 320 µm, trimToROI=false, makeAnnotations=true e removeParentAnnotation=true. Exportação downsample 1.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

image_export/<imagem>/<imagem>region <índice>.tif dentro do projeto.

## Particularidades para reutilizar

Crop_tiles_raissa.groovy é idêntico byte a byte e aponta para esta mesma cópia. Embora o comentário fale em anotações sem classe, o laço exporta todas. O recorte é a caixa delimitadora, não uma máscara do contorno. Ajuste as coordenadas fixas e o tamanho dos tiles à imagem; o Tiler remove as anotações parentais conforme sua configuração.
