# Células imunes em H&E com duas classificações

[← README](../README.md) · [Script](../scripts/imunidade/celulas_imunes_he_classificacao_duas_etapas.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `immune_cells.groovy`  
**Arquivo no acervo:** `celulas_imunes_he_classificacao_duas_etapas.groovy`

## O que faz

Define H&E, cria tecido por tissue e detecta células em Positive por watershed. Mede forma/intensidade, aplica redondas e seleciona Immune cells para uma segunda classificação roxas. Compõe nomes Immune cells: <subclasse>.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `tissue`, `redondas`, `roxas`.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

tissue mínimo 170; hematoxilina, resolução 0.5 µm; área nuclear 10–400 µm²; limiar 0.2; expansão 2 µm; redondas e roxas.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Células com medidas e classes compostas; atualização da hierarquia, sem exportação.

## Particularidades para reutilizar

A composição final de classes é feita na lista de Immune cells capturada antes da segunda classificação. O alcance efetivo dos classificadores depende de como a seleção é tratada pela API; o código não cria explicitamente um classificador restrito à lista. Não limpa todos os objetos no início.
