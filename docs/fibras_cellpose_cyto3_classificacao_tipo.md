# Fibras com classificação de tipo

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_cyto3_classificacao_tipo.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `classificacao.groovy`  
**Arquivo no acervo:** `fibras_cellpose_cyto3_classificacao_tipo.groovy`

## O que faz

Cria anotações usando tecido, seleciona todas as anotações, detecta objetos com Cellpose e mede forma. Acrescenta médias de OD/Red e aplica o classificador de objetos fiber_type.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `tecido`, `fiber_type`. Extensão Cellpose e ambiente/modelo correspondente configurados.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

tecido: mínimo 5000; cyto3, Red, pixelSize 3. Medidas de intensidade em resolução 2 µm, média de OD e Red. Classificador de objetos fiber_type.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações e detecções com medidas e classes atribuídas por fiber_type; sem exportação automática.

## Particularidades para reutilizar

O processamento usa todas as anotações selecionadas pelo próprio script. Não há filtro explícito de classe positiva nessa seleção. As classes finais dependem do classificador treinado.
