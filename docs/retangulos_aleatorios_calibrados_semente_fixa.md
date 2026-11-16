# Retângulos aleatórios com dimensões físicas e semente fixa

[← README](../README.md) · [Script](../scripts/regioes/retangulos_aleatorios_calibrados_semente_fixa.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `rectangles random.groovy`  
**Arquivo no acervo:** `retangulos_aleatorios_calibrados_semente_fixa.groovy`

## O que faz

Calcula o tamanho de um retângulo pela área de um tile e pela quantidade de tiles em largura/altura. Sorteia posições dentro da anotação chamada Tissue; na ausência dela, usa os limites da imagem.

## O que precisa

QuPath com uma imagem aberta.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Área de tile 62680.4666 µm²; composição 3×6 tiles; 10 amostras; seed 42; até nSamples×100 tentativas. Conversão separada para tamanho de pixel X/Y.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações Sample_1, Sample_2 etc.; informações de dimensões/contagens no Log. Não cria tiles internos nem exporta TIFF.

## Particularidades para reutilizar

Os retângulos podem se sobrepor: só há verificação de contenção no tecido. Reexecutar acrescenta novas anotações, podendo repetir nomes e posições. Tissue é procurado pelo nome, não pela classe. Precisa de calibração em µm; imagens/regiões menores que o retângulo impedem a criação.
