# QuPath scripts

Scripts Groovy para quantificação de colágeno, análise de microglia/GR, transferência de regiões corticais e análise piloto de Sholl.

**Repositório de destino:** [mccruz07/QuPath_scripts](https://github.com/mccruz07/QuPath_scripts).

## Estado desta coleção

Coleção organizada a partir de oito scripts: **quatro scripts principais e uma variante científica preservada**. Três implementações sobrepostas foram retiradas do pacote; o motivo e as diferenças estão em [seleção dos scripts](docs/SELECAO.md). Os cinco arquivos incluídos são cópias integrais dos originais, sem correções de código.

A revisão foi feita por leitura e comparação do código. **Nenhum script foi executado no QuPath nesta preparação.** Os cabeçalhos de microglia, transferência de ROI e Sholl declaram QuPath 0.7, embora os arquivos recebidos estivessem em uma pasta chamada `v0.6`. Isso não comprova compatibilidade com nenhuma das duas versões. `audrei_pol` não declara versão.

## Catálogo

| Script | Finalidade | Guia |
| --- | --- | --- |
| [audrei_pol.groovy](scripts/audrei_pol.groovy) | Área de tecido, colágeno total e classes red/green | [Colágeno](docs/audrei_pol.md) |
| [complete_fast.groovy](scripts/complete_fast.groovy) | Segmentação IBA/GR, associação por sobreposição, morfologia e resumo cortical | [Análise principal](docs/complete_fast.md) |
| [draw_regions.groovy](scripts/draw_regions.groovy) | Copiar uma ROI Cortex para outras imagens do projeto | [Transferência de ROI](docs/draw_regions.md) |
| [Sholl_Microglia_v6.groovy](scripts/Sholl_Microglia_v6.groovy) | Curvas de Sholl, resumo e figuras de controle de qualidade | [Sholl piloto](docs/Sholl_Microglia_v6.md) |
| [Cortex_Microglia_GR.groovy](scripts/variants/Cortex_Microglia_GR.groovy) | Variante sem filtro máximo de área de GR | [Variante](docs/Cortex_Microglia_GR.md) |

A versão `complete_fast` foi escolhida como referência de organização por reunir o índice espacial e mais saídas. O nome FAST não representa um desempenho medido nesta revisão.

## Dependências e dados necessários

- QuPath com editor Groovy e as APIs usadas pelos scripts disponíveis na instalação.
- Para microglia: projeto, imagem calibrada em µm, uma ROI Cortex e classificadores de pixels `IBA` e `GR_1`.
- Para colágeno: classificadores de pixels `tissue`, `col_total` e `col_type`, com classes compatíveis com o guia.
- Para Sholl: anotações celulares previamente segmentadas, projeto e calibração aproximadamente isotrópica em µm.
- As rotinas de morfologia e Sholl importam ImageJ e a integração `qupath.imagej.tools.IJTools`; a análise principal também usa JTS `STRtree`.

**Os classificadores treinados, imagens e projetos não foram fornecidos e não estão incluídos.** Os nomes dos classificadores referem-se a recursos do projeto, não a canais automaticamente detectados. O desempenho depende do treinamento e da segmentação.

## Como começar

1. Abra uma cópia de teste do projeto e a imagem desejada no QuPath.
2. Abra o `.groovy` no editor de scripts e leia seu guia.
3. Ajuste as constantes no início do arquivo; não há diálogos de configuração nesses scripts.
4. Confira classificadores, calibração, classes e região de interesse.
5. Execute na imagem atual e examine o Log, as anotações e os arquivos exportados.
6. Salve os dados da imagem quando desejar persistir as alterações. Os CSVs não substituem o salvamento das anotações; `draw_regions` salva explicitamente as entradas do projeto.

### Fluxo para microglia

Prepare uma ROI de área chamada ou classificada `Cortex` → execute **uma** das análises de microglia → revise a segmentação → execute Sholl, se desejado.

`draw_regions` é opcional e só se aplica quando faz sentido reutilizar as mesmas coordenadas. Ele não registra/alinha imagens. Antes do Sholl, altere `CELL_CLASS = 'iba'` para `CELL_CLASS = 'Microglia'` se usar as anotações produzidas pela análise principal. Essa alteração não foi aplicada ao arquivo distribuído.

### Efeitos que precisam ser conhecidos

- `audrei_pol` começa removendo anotações existentes.
- As análises de microglia removem, por padrão, anotações Microglia/GR com centroide dentro de Cortex e criam novas.
- `draw_regions` modifica e salva várias imagens do projeto em uma única execução. Execute-o apenas na imagem de referência, sem um laço adicional de processamento por imagem.
- Sholl acrescenta anotações de soma/centro por padrão, embora preserve as já existentes com o mesmo identificador de soma.
- Reexecutar colágeno ou microglia acrescenta linhas aos CSVs compilados, podendo duplicar resultados.
- As medidas de esqueleto das análises de microglia exigem verificação da conversão de coordenadas e da polaridade antes de uso quantitativo. Veja [validação](docs/VALIDACAO.md).

## Organização e documentação adicional

```text
QuPath_scripts/
├── README.md
├── scripts/
│   ├── audrei_pol.groovy
│   ├── complete_fast.groovy
│   ├── draw_regions.groovy
│   ├── Sholl_Microglia_v6.groovy
│   └── variants/Cortex_Microglia_GR.groovy
└── docs/
    ├── audrei_pol.md
    ├── complete_fast.md
    ├── draw_regions.md
    ├── Sholl_Microglia_v6.md
    ├── Cortex_Microglia_GR.md
    ├── SELECAO.md
    ├── DICIONARIO_DADOS.md
    ├── VALIDACAO.md
    └── MANIFESTO.json
```

O [dicionário de dados](docs/DICIONARIO_DADOS.md) lista as colunas exportadas. O [manifesto](docs/MANIFESTO.json) registra os hashes dos oito arquivos recebidos e sua destinação.

## Autoria, citação e licença

Os arquivos recebidos não fornecem uma atribuição formal de autoria e licença suficiente para definir esses campos nesta entrega. Não foi criada uma licença em nome do titular. Ao preparar uma versão pública, confirme a autoria e escolha a licença desejada.

Para reprodutibilidade, registre o script, commit do repositório, versão do QuPath, classificadores e seus parâmetros, calibração, critérios de exclusão e versão dos dados. Não há DOI ou versão de lançamento fornecidos.
