# Sistemas já implementados (documentação detalhada)

Este arquivo descreve o que já está funcional dentro do plugin, com foco em módulos que não estavam detalhados no README. Use-o como referência para operar e configurar os recursos existentes.

## Proteção de claims (reinos e clãs)
- **Escopo**: toda tentativa de quebrar, colocar blocos ou interagir com blocos/entidades passa pelo `ClaimProtectionListener`.
- **Critérios de bloqueio**:
  - Verifica o chunk atual via `ClaimService.findClaimAt`, que prioriza claims de **reino** e, se não houver, claims de **clã**.
  - Se o jogador não for membro do dono (reino ou clã), a ação é cancelada e uma mensagem é enviada com cooldown para evitar spam.
- **Flags padrão**: `BUILD`, `INTERACT`, `PVP` e `MOB_DAMAGE` são negadas para não membros; hoje não existem exceções configuráveis além da associação ao dono do claim.
- **Configuração** (`config.yml`):
  - `claims.protection_enabled` liga/desliga toda a proteção.
  - `claims.deny_message_cooldown_ticks` controla o intervalo (em ticks) para reenviar a mensagem de bloqueio por jogador.
- **Mensagens**: personalize `claims.protected.kingdom` e `claims.protected.clan` em `messages.yml` para indicar quem controla a área.

## Economia
- **Persistência**: saldos são mantidos em `plugins/EarthCore/currency.yml`, com cache em memória protegido por lock de leitura/escrita.
- **Criação automática**: `EconomyService` garante a criação de conta ao consultar o saldo (`ensureAccount`).
- **Limite antiflood**: até 5 transações por tick por jogador (`MAX_TRANSACTIONS_PER_TICK`); exceder lança exceção de operação.
- **Auditoria**: as últimas 500 transações ficam em um deque em memória para inspeção e para disparar eventos Bukkit (`PlayerMoneyReceiveEvent`/`PlayerMoneySpendEvent`).
- **Eventos e integrações**: depósitos e saques disparam eventos com o motivo (`reason`) e o tipo de transação (`MoneyTransactionType`), permitindo hooks externos.

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

## Portais de totem
- **Propósito**: permitir teleporte ao clicar em um bloco/totem específico, sem comando de jogador.
- **Configuração em jogo** (`/portal`, permissão `core.portal`):
  - `/portal settarget <nome>` define o destino usando a posição atual do executor.
  - `/portal settotem <nome>` vincula o bloco focado (até 6 blocos de distância) como totem do portal.
- **Persistência**: cada portal é salvo em `config.yml` na raiz `portals.<nome>.{target,totem}`; recarregados no start do plugin.
- **Uso**: ao clicar no totem configurado, o jogador é teleportado para o destino correspondente; portais incompletos avisam o usuário.

## Utilidades de teleporte e sociais
- **Spawn global**: armazenado em `config.yml` (`spawn.*`); `/setspawn` grava, `/spawn` teleporta. Logs alertam se o mundo não estiver carregado.
- **Homes**: um home por jogador persistido em `homes.yml`; `/home set` grava e `/home` teleporta se existir e o mundo estiver carregado.
- **Pedidos de teleporte**: `/tpa` e `/tphere` armazenam solicitações em memória (`TeleportRequestService`) mapeadas por alvo; cada novo pedido sobrescreve o anterior para o mesmo alvo.
- **Mensagens privadas**: `/msg` registra o último contato bilateral em `PrivateMessageService` para habilitar `/reply` imediato.

## Frontend embutido e Dynmap
- **Inicialização**: `FrontendServer` sobe automaticamente se `frontend.enabled` estiver `true`, usando host/porta da config e apontando o iframe para `frontend.dynmapUrl`.
- **Dados expostos**: endpoint `/api/user?username=<nick>` retorna saldo, reino e clã consultando os serviços internos; ideal para painel informativo.
- **Dynmap**: se o plugin Dynmap estiver carregado, o hook adiciona áreas de claims de reinos/clãs no mapa com logging de status na inicialização.
