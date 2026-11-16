# Sequência gravada de ajustes de detecção DAB

[← README](../README.md) · [Script](../scripts/ihq/historico_ajustes_deteccao_positividade_dab.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Milena 2 (Lab Lourdes fev.2026).groovy`  
**Arquivo no acervo:** `historico_ajustes_deteccao_positividade_dab.groovy`

## O que faz

Contém uma sequência de chamadas de PositiveCellDetection e WatershedCellDetection intercaladas com remoções de objetos selecionados e seus descendentes. Altera resolução, área mínima, expansão e limiares ao longo do arquivo.

## O que precisa

QuPath com uma imagem aberta.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Começa com H-DAB. Usa resoluções 0.5/1 µm, áreas mínimas 5/10, expansões 5/8, limiares de detecção 0.1/0.05 e positividade citoplasmática 0.2/0.13/0.15. A última chamada usa Nucleus: DAB OD mean, limiar 0.15 e expansão 8.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Objetos e medidas na sessão; nenhum CSV ou comparação consolidada dos parâmetros.

## Particularidades para reutilizar

É um histórico de ações em sequência, não um seletor de alternativas. Executar o arquivo inteiro dispara todas as chamadas e remoções. Para reutilizar um ajuste, escolha o bloco correspondente e prepare a seleção exigida; não suponha que os resultados das etapas anteriores serão preservados.
