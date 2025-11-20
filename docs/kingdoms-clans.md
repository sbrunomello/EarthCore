# Reinos e Clãs

Este módulo adiciona gerenciamento político e social nativo ao plugin. Reinos controlam território por meio de claims, enquanto clãs oferecem grupos paralelos focados em cooperação e PvP.

## Comandos

### Reinos
- `/kingdom create <nome>`: cria um novo reino (cobra `create-cost`).
- `/kingdom invite <jogador>`: envia convite para o reino (rei/conselheiro).
- `/kingdom join <nome>`: aceita o convite recebido.
- `/kingdom claim` / `/kingdom unclaim`: adiciona ou remove o claim do chunk atual.
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
