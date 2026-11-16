# Fibras com resumo de áreas e processamento em CPU

[← README](../README.md) · [Script](../scripts/fibras/fibras_cellpose_sam_resumo_area_cpu.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `muscle_fibers.groovy`  
**Arquivo no acervo:** `fibras_cellpose_sam_resumo_area_cpu.groovy`

## O que faz

Limpa todos os objetos, cria tecido por muscle e executa Cellpose-SAM nas Positive. Filtra anotações Fiber por área/circularidade e calcula contagens, média e desvio padrão amostral da área.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle mínimo 2000; cp_sam, Red, pixelSize 1, GPU desativada. Área 1000–4000; circularidade mínima 0.45.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Acrescenta linha em consolidado_medidas_fibras.csv na raiz do projeto. Registra contagens e estatísticas na primeira Positive.

## Particularidades para reutilizar

Area_Total_Tecido usa ROI.getArea() em pixels²; áreas das fibras usam medidas em µm² ou fallback em pixels. Confira a unidade usada nos limiares. CSV recebe novas linhas a cada execução. Modelo cp_sam e classificador muscle precisam estar disponíveis.
