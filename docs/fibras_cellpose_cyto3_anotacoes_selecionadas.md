# Cellpose cyto3 em anotações selecionadas

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_cyto3_anotacoes_selecionadas.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `fiber_dectection.groovy`  
**Arquivo no acervo:** `fibras_cellpose_cyto3_anotacoes_selecionadas.groovy`

## O que faz

Executa detecção Cellpose dentro dos objetos atualmente selecionados. Se não houver seleção, exibe uma mensagem e encerra.

## O que precisa

QuPath com uma imagem aberta. Extensão Cellpose e ambiente/modelo correspondente configurados. Uma ou mais anotações selecionadas.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Modelo cyto3; canal Red; pixelSize 3. As demais opções do template estão comentadas.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Detecções dentro dos objetos selecionados e mensagem no Log; sem exportação explícita.

## Particularidades para reutilizar

Selecione as anotações de interesse antes de executar. O cabeçalho atribui o template a Olivier Burri; essa atribuição permanece no arquivo. Ao adaptar, diferencie as opções ativas das sugestões comentadas.
