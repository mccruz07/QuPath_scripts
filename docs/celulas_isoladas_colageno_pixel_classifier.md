# Células isoladas e colágeno — collagen

[← README](../README.md) · [Script](../scripts/colageno/celulas_isoladas_colageno_pixel_classifier.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `glayce.groovy`  
**Arquivo no acervo:** `celulas_isoladas_colageno_pixel_classifier.groovy`

## O que faz

Segmenta tecido com total_tissue, executa Cellpose, mede forma e classifica detecções com isolated_cells. Por fim seleciona anotações muscle e aplica collagen.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `total_tissue`, `isolated_cells`, `collagen`. Extensão Cellpose e ambiente/modelo correspondente configurados.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

total_tissue: mínimo 110; cyto3, Red, pixelSize 3; classificador de objetos isolated_cells; classificador final collagen com mínimo 0.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações de tecido/colágeno e detecções classificadas na hierarquia; sem CSV automático.

## Particularidades para reutilizar

Apesar de selecionar muscle antes da detecção, pathObjects recebe getAnnotationObjects(): Cellpose percorre todas as anotações. As duas variantes diferem no classificador final de colágeno. O cabeçalho de template atribui autoria a Olivier Burri; não implica autoria de todas as adaptações.
