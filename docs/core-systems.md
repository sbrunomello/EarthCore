# Sistemas já implementados (documentação detalhada)

Este arquivo descreve o que já está funcional dentro do plugin, com foco em módulos que não estavam detalhados no README. Use-o como referência para operar e configurar os recursos existentes.

## Proteção e fluxo de claims (reinos e clãs)
- **Proteção**: toda tentativa de quebrar, colocar blocos ou interagir com blocos/entidades passa pelo `ClaimProtectionListener`.
  - Verifica o chunk atual via `ClaimService.findClaimAt`, priorizando claims de **reino** e, se não houver, claims de **clã**.
  - Se o jogador não for membro do dono (reino ou clã), a ação é cancelada e uma mensagem é enviada com cooldown para evitar spam.
  - Flags padrão `BUILD`, `INTERACT`, `PVP` e `MOB_DAMAGE` são negadas para não membros; hoje não existem exceções configuráveis além da associação ao claim.
- **Preview e confirmação**: `ClaimPreviewManager` desenha um preview temporário de chunk com vidro + tocha nos cantos (30s, auto limpeza) para evitar grief acidental.
  - `ClaimFlowService` reaproveita o mesmo fluxo para comandos e GUIs: confirma o claim após revalidar elegibilidade e efetuar o pagamento no banco do grupo.
  - Se o chunk ficar indisponível ou o banco estiver sem saldo, o preview é limpo e o jogador recebe mensagem específica.
- **Configuração** (`config.yml`):
  - `claims.protection_enabled` liga/desliga toda a proteção.
  - `claims.deny_message_cooldown_ticks` controla o intervalo (em ticks) para reenviar a mensagem de bloqueio por jogador.
- **Mensagens**: personalize `claims.protected.kingdom` e `claims.protected.clan` em `messages.yml` para indicar quem controla a área. Mensagens do fluxo de claim ficam em `claims.*` dentro do `config.yml`.

## Economia
- **Persistência**: saldos são mantidos em `plugins/EarthCore/currency.yml`, com cache em memória protegido por lock de leitura/escrita.
- **Criação automática**: `EconomyService` garante a criação de conta ao consultar o saldo (`ensureAccount`).
- **Limite antiflood**: até 5 transações por tick por jogador (`MAX_TRANSACTIONS_PER_TICK`); exceder lança exceção de operação.
- **Auditoria**: as últimas 500 transações ficam em um deque em memória para inspeção e para disparar eventos Bukkit (`PlayerMoneyReceiveEvent`/`PlayerMoneySpendEvent`).
- **Eventos e integrações**: depósitos e saques disparam eventos com o motivo (`reason`) e o tipo de transação (`MoneyTransactionType`), permitindo hooks externos.
- **Top e ajustes**: o repositório interno oferece ranking (`getTopBalances`) e localização do jogador mais rico, além de operações de ajuste direto de saldo para admins (`setBalance`).

## Notificações in-game
- **Escopo**: mensagens curtas para ganhos financeiros e avisos críticos são entregues via action bar para aumentar a visibilidade sem poluir o chat.
- **Origem do ganho**: `NotificationListener` escuta `PlayerMoneyReceiveEvent` e chama `NotificationService#notifyMoneyReceived`, que resolve a origem com base no `MoneyTransactionType` (job, impostos, banco do reino, etc.).
- **Fallbacks**: se `Player#sendActionBar` não existir na versão do servidor, o serviço tenta o método do Spigot (`player.spigot().sendMessage`); em último caso envia no chat e registra aviso no log.
- **Personalização**: os textos ficam em `messages.yml` (`notifications.money.receive` e `notifications.important`), com placeholders `{amount}` e `{source}` formatados com separador brasileiro.

## Clãs
- **Criação e convites**: `/clan create <nome> <tag>` cobra o custo configurado e define o criador como líder. Convites são geridos em memória e aceitos via `/clan join`.
- **Cargos e permissões**: líderes podem convidar, reivindicar território e sacar do banco; oficiais podem convidar/claimar se a role permitir (`ClanRole.canInvite/canClaim`).
- **Claim inicial único**: cada clã só mantém um `ClaimedChunk` e precisa dele para evoluir a reino. Claims existentes impedem reclaims de outros clãs/reinos.
- **Banco e impostos internos**: depósitos sofrem imposto configurável (`bankTaxRate`) antes de entrar no cofre; apenas líder saca. O cálculo de imposto (`calculateTax`) permite que ganhos externos direcionem parte ao banco do clã.
- **Dynmap**: quando habilitado, o hook redesenha claims e remove marcadores ao dissolver o clã.

## Reinos
- **Requisito de clã**: apenas o líder de um clã com claim ativo e mínimo de membros pode fundar um reino; o claim vira a capital e os membros do clã são copiados como cidadãos.
- **Claims e custos**: roles que podem gerenciar territórios adicionam/removem claims com custo crescente (`baseClaimCost` + multiplicador por claim existente). Claims de clãs terceiros ou capitais não podem ser sobrescritos/desfeitos. Claims são marcados visualmente com tochas nos cantos do chunk.
- **Evolução de tier**: `/kingdom upgrade` verifica requisitos por tier (`minMembers`, `minClaimsUsed`, custo) e atualiza o nível com feedback de mensagem/GUI.
- **Banco e impostos automáticos**: depósitos/saques usam `KingdomBankService` (somente líder saca) e podem aplicar imposto automático sobre ganhos de membros (config `kingdom_bank.tax`). A função `calculateTax/applyTax` separa valor líquido do tributo para o reino.
- **Upkeep agendado**: um scheduler periódico cobra manutenção baseada em claims e multiplicadores por tier. Usa saldo do banco, depois do rei; se falhar, acumula dívida, remove claims periodicamente e dissolve após dias de carência.
- **Dynmap**: claims de reino são desenhados/atualizados pelo `KingdomDynmapHook`, sincronizando exclusões ao disband.

## GUIs embutidos
- **Sistema base**: `GuiManager` registra inventários controlados, cancela cliques externos e entrega eventos para GUIs concretas, protegendo o jogador com mensagens de erro seguras.
- **Seleção de Jobs**: `JobSelectGui` lista empregos configurados, exibindo ganhos e permitindo troca via clique.
- **Painéis de clã/reino**: `ClanMainGui` e `KingdomMainGui` mostram saldo, claims, membros e atalhos para ações comuns (banco, upgrade, detalhes de claim), reutilizando mensagens centralizadas.

## Jobs
- **Seleção**: cada jogador mantém **um job ativo** salvo em `jobs-data.yml`; trocar usa `/job join <job>`.
- **Pagamentos**: ganhos são definidos por bloco quebrado ou entidade abatida em `jobs.yml` (por job). Recompensas são creditadas via `EconomyService` com o tipo `JOB_REWARD`.
- **Proteções anti-exploit**:
  - Intervalo mínimo entre pagamentos por jogador (`anti_exploit.min_millis_between_rewards`).
  - Ignora blocos colocados recentemente pelo próprio jogador (`placed_block_ttl_millis`).
  - Para o job **FARMER**, colheitas só pagam se o `Ageable` estiver no nível máximo (maduro).
- **Persistência**: alterações são salvas em `onDisable`, reutilizando `JobStorage`.

## Canais de chat
- **Canais disponíveis**: global (padrão), local (raio de 100 blocos no mesmo mundo), clã, cidade (reino) e admin.
- **Formato**: cada canal adiciona prefixo e cor; operadores recebem tag `[ADMIN]` em qualquer canal.
- **Entrega**: resolução de destinatários é feita pelo `ChatService` conforme o canal escolhido; admins recebem apenas operadores, clã/reino dependem da associação ativa e o local filtra por distância.
- **Comando**: `/chat <global|local|clan|cidade|admin>` alterna o canal do jogador.

## Portais de viagem por aldeão
- **Propósito**: hubs de viagem entre continentes via NPC aldeão, abrindo uma GUI para escolher o destino.
- **Configuração em jogo** (`/portal`, permissão `core.portal`):
  - `/portal settarget <nome>` salva o ponto de chegada daquele portal usando a posição atual do executor.
  - `/portal settotem <nome>` define onde o aldeão do portal ficará (bloco em foco ou posição atual) e respawna o NPC.
  - `/portal list` mostra o status de todos os portais configurados (destino e aldeão presentes ou não).
- **Persistência**: cada portal é salvo em `config.yml` na raiz `portals.<nome>.{target,totem}`; ao iniciar/recarregar, o serviço remove aldeões antigos marcados e respawna NPCs em cada `totem` registrado.
- **Uso**:
  - Interagir com o aldeão abre a GUI “Portais de Viagem” listando todos os outros portais ativos.
  - Clicar em um item teleporta para o `target` configurado do destino; portais sem destino exibem mensagem de erro amigável.

## Utilidades de teleporte e sociais
- **Spawn global**: armazenado em `config.yml` (`spawn.*`); `/setspawn` grava (perm. `core.setspawn`, padrão apenas para OPs), `/spawn` teleporta (aberto para todos, sem permissão). Logs alertam se o mundo não estiver carregado.
- **Homes**: um home por jogador persistido em `homes.yml`; `/home set` grava e `/home` teleporta se existir e o mundo estiver carregado.
- **Pedidos de teleporte**: `/tpa` e `/tphere` armazenam solicitações em memória (`TeleportRequestService`) mapeadas por alvo; cada novo pedido sobrescreve o anterior para o mesmo alvo.
- **Mensagens privadas**: `/msg` registra o último contato bilateral em `PrivateMessageService` para habilitar `/reply` imediato.

## Kit inicial
- **Entrega automática**: o `StarterKitListener` concede o kit apenas para jogadores realmente novos (que nunca entraram no servidor). Jogadores antigos são ignorados, mas marcados em cache para não serem processados a cada login.
- **Configuração** (`starter-kit.yml`): ativa/desativa o sistema (`enabled`), define mensagens e lista de itens com material, quantidade e encantamentos. O arquivo é criado no primeiro start com exemplos e é regravado preservando correções básicas.
- **Comando** (`/starterkit`): depende da permissão `core.starterkit` (true por padrão) e respeita cooldown configurável (`command.cooldown-seconds`). As mensagens de sucesso/bloqueio são parametrizadas com `%time%` e reutilizam o mesmo kit do fluxo automático.
- **Persistência e auditoria**: `starter-kit-data.yml` guarda quem já recebeu o kit e o último uso do comando por UUID. Sempre que um kit é entregue, sobram itens são dropados aos pés do jogador e mensagens opcionais orientam sobre o drop.

## Frontend embutido e Dynmap
- **Inicialização**: `FrontendServer` sobe automaticamente se `frontend.enabled` estiver `true`, usando host/porta da config e apontando o iframe para `frontend.dynmapUrl`.
- **Dados expostos**: endpoint `/api/user?username=<nick>` retorna saldo, reino e clã consultando os serviços internos; ideal para painel informativo.
- **Dynmap**: se o plugin Dynmap estiver carregado, o hook adiciona áreas de claims de reinos/clãs no mapa com logging de status na inicialização.

## Autenticação offline (register/login)
- **Persistência segura**: hashes bcrypt ficam em `plugins/EarthCore/auth.yml` via `AuthStorage`; nenhuma senha é salva em texto puro.
- **Sessão**: `AuthSessionManager` mantém apenas UUIDs logados em memória, liberando players após `/login` ou `/register` bem-sucedidos.
- **Restrições pré-login**: o `AuthRestrictionListener` bloqueia movimento, comandos (exceto `/login` e `/register`), chat, dano e interações até autenticar, evitando abuso por bots.
- **Mensagens**: mensagens rápidas de bloqueio ficam em `AuthMessages`, voltadas para lembrar que o login é obrigatório.

## Lojas físicas com NPC
- **Tipos de loja**: pessoal (`/shop create <nome>`), clã (`/shop clan create <nome>`, apenas líder, dentro de claim do clã) e reino (`/shop kingdom create <nome>`, apenas rei, dentro de claim do reino). Pessoais exigem estar em área onde você tenha permissão de construir. O posicionamento é confirmado com `/shop confirm` usando um bastão personalizado.
- **Infraestrutura**: ao criar, o `ShopService` spawna um aldeão fixo (invulnerável, sem IA) e gera um baú duplo atrás do NPC para ser o estoque físico; a posição é pré-visualizada com blocos vermelhos antes de materializar o baú e o aldeão.
- **Edição**: o dono abre uma GUI de edição (`ShopEditGui`) que pede preço via chat e quantidade por clique (1 ou 64). O item é copiado do que está na mão para garantir fidelidade de metadados.
- **Compra**: `ShopBuyGui` lista os itens; ao comprar o sistema verifica saldo (`EconomyService`), espaço no inventário e estoque antes de debitar o comprador e creditar o dono (tipo `PLAYER_TRADE`). Estoque é removido diretamente do baú.
- **Persistência e resiliência**: lojas e itens são salvos em disco (`ShopStorage`). NPCs e baús são recriados no carregamento; remoção limpa o estoque e destrói o aldeão. NPCs de loja também são protegidos por `NpcProtectionListener` contra dano/target.

## Scoreboard dinâmico
- **Modelo configurável**: `scoreboard.*` em `config.yml` define título, rodapé, cores e separadores. Ícones têm fallback para fontes antigas.
- **Dados exibidos**: coordenadas, horário e mundo atuais, clã, reino, saldo (money/coins), kills de jogadores, mobs, deaths e placeholder de conquistas.
- **Atualização**: `ScoreboardService` cria um holder por jogador no login e atualiza a cada 20 ticks via `ScoreboardUpdaterTask`, reaproveitando times para evitar flicker. Usa `EconomyService`, `ClanService`, `KingdomService` e `PlayerStatsService` como fontes.
- **Fallback seguro**: ao desabilitar via config, o serviço apenas loga e não registra placares; no desligamento restaura o scoreboard principal para evitar lixo visual.

## Proteção de NPCs sensíveis
- `NpcProtectionListener` anula dano e target em aldeões ligados a portais (`PortalService`) e lojas (`ShopService`), reforçando a invulnerabilidade mesmo se outros plugins alterarem flags.
