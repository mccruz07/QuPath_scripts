# Fibras cpsam com filtros de área e circularidade

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_cpsam_filtro_forma.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `fibers.groovy`  
**Arquivo no acervo:** `fibras_cellpose_cpsam_filtro_forma.groovy`

## O que faz

Define BRIGHTFIELD_OTHER/H-DAB, cria tecido com muscle e usa as anotações Positive para Cellpose. Mede forma/intensidade, calcula aspect ratio e remove detecções por área e circularidade.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`. Extensão Cellpose e ambiente/modelo correspondente configurados.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

cpsam, Red, pixelSize 2; área 50–100000 µm²; circularidade mínima 0.30. Aspect ratio = Max diameter µm / Min diameter µm.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Detecções restantes e medidas na imagem; imprime contagem e nomes das medidas disponíveis no Log. Não exporta arquivos.

## Particularidades para reutilizar

O aspect ratio é calculado, mas não participa do filtro. O filtro percorre todas as detecções da imagem, inclusive preexistentes. useCellposeSAM não é chamado explicitamente. As chaves de área/diâmetro precisam corresponder às medidas disponíveis.
