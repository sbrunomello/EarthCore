# Interface gráfica in-game (GUIs)

Este documento descreve a infraestrutura de GUIs embutida no plugin e como ela é usada hoje para seleção de jobs. Tudo já está funcional e não requer dependências externas além do próprio Paper/Spigot.

## Componentes principais
- **`GuiManager`** (`mello.core.gui.GuiManager`): registrador único de GUIs abertos por jogador. Cancela cliques em inventários gerenciados e roteia eventos de clique/fechamento para o GUI correto, fechando e limpando o registro em caso de erro para evitar inventários presos.
- **`AbstractGui`** (`mello.core.gui.AbstractGui`): classe base que encapsula a criação de inventário, construção de layout no `build()` e callbacks `handleClick`/`handleClose`. Toda GUI deve herdar dela.
- **Mensagens**: `GuiMessages` centraliza textos (ex.: `gui.open.error`, `gui.job.selected`) e é injetado no `GuiManager` e nas GUIs para feedback padronizado.

## JobSelectGui (GUI de seleção de jobs)
- **Acesso rápido**: rodar `/job` sem argumentos abre a GUI automaticamente; `/job list`/`join` continuam funcionando por texto.
- **Layout**: inventário 3x9 com cards para cada job habilitado, ordenados pelo enum `JobType`. Slot 26 é um botão de fechar (`BARRIER`).
- **Interação**:
  - Clique em um card seleciona o job e reenfileira o layout com destaque para o job atual.
  - O lore de cada card mostra descrições rápidas e até três payouts configurados para blocos, além de etiqueta "(Seu job)" quando ativo.
  - Mensagens de sucesso/erro são exibidas no chat e erros inesperados fecham a GUI, registrando log.
- **Proteções**: cliques em slots não mapeados são ignorados; o manager cancela todos os eventos no inventário para evitar mover itens do jogador.

## Boas práticas para novas GUIs
- Crie subclasses de `AbstractGui` e registre-as via `GuiManager.registerGui(...)` no momento da abertura.
- Sempre trate exceções em `handleClick` e garanta mensagens amigáveis; o manager já faz fallback para `gui.open.error` e fecha o inventário em falhas.
- Prefira usar `GuiMessages` para textos e injeção de dependências de serviço (ex.: EconomyService) para manter a lógica fora da camada de exibição.
