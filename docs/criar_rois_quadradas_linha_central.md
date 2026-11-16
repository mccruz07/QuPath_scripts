# Criar ROIs quadradas em uma linha central

[← README](../README.md) · [Script](../scripts/regioes/criar_rois_quadradas_linha_central.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `ROI_creation.groovy`  
**Arquivo no acervo:** `criar_rois_quadradas_linha_central.groovy`

## O que faz

Remove as anotações existentes e cria quadrados distribuídos horizontalmente no centro da imagem, com espaçamento calculado pela largura disponível.

## O que precisa

QuPath com uma imagem aberta.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

roiSize 200 pixels; nROIs 5; ImagePlane.getDefaultPlane().

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações ROI_1 a ROI_5, sem classe; não exporta arquivos.

## Particularidades para reutilizar

A chamada removeObjects(getAnnotationObjects(), false) também remove descendentes das anotações conforme o argumento keepChildren=false. A limpeza está ativa apesar de o comentário chamá-la de opcional. Quando a imagem é pequena para o conjunto de ROIs, o espaçamento pode ficar negativo e as regiões podem se sobrepor.
