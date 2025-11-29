# Reinos e Clãs

Este módulo adiciona gerenciamento político e social nativo ao plugin. Reinos controlam território por meio de claims, enquanto clãs oferecem grupos paralelos focados em cooperação e PvP.

## Comandos

### Reinos
- `/kingdom create <nome>`: cria um novo reino (cobra `create-cost`).
- `/kingdom invite <jogador>`: envia convite para o reino (rei/conselheiro).
- `/kingdom join <nome>`: aceita o convite recebido.
- `/kingdom claim` / `/kingdom unclaim`: adiciona ou remove o claim do chunk atual.
- `/kingdom request <nome>`: envia solicitação para entrar em um reino específico.
- `/kingdom requests`: lista solicitações pendentes para o seu reino (rei/nobre).
- `/kingdom acceptrequest <jogador>`: aceita uma solicitação pendente e adiciona o jogador como cidadão.
- `/kingdom info [nome]`: mostra detalhes, claims e tesouraria.
- `/kingdom leave`: sai do reino (o rei só pode sair se estiver sozinho).

### Clãs
- `/clan create <nome> <tag>`: cria um clã e registra a tag.
- `/clan invite <jogador>` / `/clan join <nome>`: fluxo de convite/entrada.
- `/clan deposit <valor>`: deposita no banco do clã (aplica imposto de banco).
- `/clan withdraw <valor>`: saque do banco (apenas líder).
- `/clan info [nome]`: mostra membros, líder e saldo do banco.
- `/clan leave`: sai do clã (o líder deve transferir/ficar sozinho para sair).

## Impostos
- **Reinos**: toda transferência via `/pay` destina uma porcentagem para a tesouraria do reino do recebedor, definida em `kingdoms.yml` (`transaction-tax-rate`).
- **Clãs**: depósitos voluntários e recebimentos via `/pay` podem direcionar uma fração para o banco do clã (`bank-tax-rate` em `clans.yml`).

## Tesouraria dos reinos (banco + impostos)
- **Habilitação**: controlada por `kingdom_bank.enabled` em `kingdoms.yml` (gera o arquivo na pasta de dados na primeira execução).
- **Imposto automático**: `kingdom_bank.tax.enabled`, `rate` e `min_amount` definem quanto do `/pay` recebido pelos membros é enviado ao banco do reino antes de o jogador receber o valor líquido.
- **Uso nos comandos**:
  - `/kingdom deposit <valor>` desconta do saldo pessoal (com `MoneyTransactionType.KINGDOM_DEPOSIT`) e credita no banco do reino.
  - `/kingdom withdraw <valor>` só pode ser usado pelo rei; saca do banco do reino para o jogador.
- **Top balances**: o banco é usado como fonte de verdade para custos de manutenção e exibição de saldo interno; falhas geram mensagens formatadas via `KingdomMessages`.

## Upkeep automático dos reinos
- **Scheduler**: iniciado no `onEnable` (`KingdomService.startUpkeepScheduler`), roda a cada `upkeep.check_interval_minutes` minutos.
- **Cálculo**: `base_cost_per_claim` multiplicado pelo total de claims (inclui capital) e pelo multiplicador do tier (`upkeep.tier_multipliers.<TIER>`).
- **Cobrança**: tenta usar primeiro o banco do reino; faltando saldo, desconta do rei (`MoneyTransactionType.KINGDOM_UPKEEP`). Upkeeps parciais são revertidos se o saque do rei falhar.
- **Dívida e penalidades**:
  - Incrementa `debtDays` e marca `atRiskSince` ao falhar uma cobrança.
  - Após `grace_days_before_penalty`, remove o claim mais recente a cada 24h sem pagamento (`kingdom.upkeep.claim_lost`).
  - Após `grace_days_before_disband`, dissolve automaticamente o reino e remove markers do BlueMap (`kingdom.upkeep.disbanded`).

## Marcação visual e BlueMap
- **Tochas nos claims**: sempre que um chunk é claimado pelo reino, o serviço coloca tochas nos quatro cantos da superfície do chunk para feedback imediato sem GUIs.
- **BlueMap**: hooks de reinos e clãs são carregados automaticamente se o plugin BlueMap estiver presente; claims são redesenhados após depósitos, claims/unclaims e disband, ou removidos junto ao reino/clã.
- **Feedback imediato**: após confirmar um claim (GUI ou comando), partículas verdes contornam o chunk por alguns segundos para facilitar a visualização da área protegida.

## Configuração

### kingdoms.yml
- `create-cost`: custo para fundar um reino.
- `chunk-claim-cost`: custo por claim de chunk.
- `upkeep-per-chunk`: valor base para cálculos futuros de manutenção.
- `transaction-tax-rate`: percentual recolhido nas transações recebidas pelos membros.

### clans.yml
- `create-cost`: custo para criar um clã.
- `bank-tax-rate`: percentual retido em depósitos/recebimentos para o banco do clã.

## Persistência
- `kingdoms-data.yml`: armazena reinos, membros, claims e tesouraria.
- `clans-data.yml`: armazena clãs, cargos e saldo do banco.

## Boas práticas
- Sempre valide valores numéricos vindos de comandos antes de aplicar operações financeiras.
- Apenas cargos autorizados (rei/conselheiro, líder) executam ações sensíveis como convite, claim e saque.
- Salve dados em `onDisable` para evitar perda de estado em reinícios inesperados.
