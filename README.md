# EarthCore (nome provisório)

Plugin monolítico de gameplay para servidores Minecraft estilo Earth, focado em dominação de território, economia profunda e progressão de personagens. O objetivo é reduzir a dependência de plugins de terceiros, concentrando sistemas críticos como economia, jobs, reinos, clãs e skills em um único .jar.

## Visão Geral do Projeto

EarthCore é pensado para servidores com mapa da Terra, onde jogadores constroem reinos, disputam território e desenvolvem personagens em um ambiente persistente. O plugin atua como o "cérebro de gameplay" do servidor, orquestrando dinheiro, progressão, interações políticas e integrações externas (Dynmap e um futuro cliente web estilo RTS).

## Principais Funcionalidades

### Economia / Moeda
- Moeda principal do servidor (ex.: `Coins`).
- Saldos individuais, depósitos, saques e transferências entre jogadores.
- Ganchos para impostos de reinos/cidades e recompensas de jobs/quests.
- API interna para outros módulos consumirem (Jobs, Reinos, Clãs, Skills).

### Jobs (Empregos)
- Seleção de um ou mais jobs (Minerador, Lenhador, Fazendeiro, Caçador etc.).
- Ganhos automáticos por ações (quebrar bloco, matar mob, plantar, pescar). 
- Progressão de nível por job, com multiplicadores configuráveis e limites por jogador.
- Integração com Economia (pagamento direto) e Skills (sinergias de bônus).

### Reinos / Nações / Cidades
- Sistema próprio de macroestrutura política: reinos controlam regiões (claims/chunks).
- Criação de reino com custo, convites, cargos (rei, conselheiro, membro) e impostos.
- Administração de território: permissões de construção/uso de containers e fronteiras.
- Integração futura com Dynmap (fronteiras, marcadores) e web RTS (ordens remotas).

### Clãs
- Grupos paralelos aos reinos, focados em PvP ou cooperação.
- Criação de clã, tag, home, cargos e banco compartilhado.
- Integração futura com skills, jobs e eventos/guerras.

### Skills / MMO
- Skills de Mineração, Escavação, Combate, Arco, Agricultura, Pesca, entre outras.
- XP por ação relacionada, concedendo benefícios (drops, dano, velocidade etc.).
- Sinergia com Jobs (maior eficiência) e impactos em ganhos econômicos.

### Economia Avançada
- Lojas de jogadores (físicas ou via GUI) e mercado global/leilão (AH).
- Impostos de reinos em transações locais e custos de manutenção territorial.
- Pontos de extensão para integrações externas (web market, moeda externa/cripto em estudo).

### Integração com Dynmap
- Exposição de dados de reinos, cidades, clãs e áreas de influência para markers/áreas.
- Base para visualização macro do mapa e consumo por clientes web.

### Ponte com Sistema Web RTS
- Roadmap para APIs REST/WebSocket que permitam ações via navegador.
- Recebimento de comandos do backend web (ordens agendadas que afetam o mundo).

### NPCs Interativos
- NPCs no spawn/cidades para oferecer jobs, tutoriais, quests e menus GUI.
- Interações por clique direito disparando comandos internos do plugin.

### Comandos e Permissões
- Comandos administrativos: `/earthcore reload`, `/coins add`, `/kingdom create`, etc.
- Comandos de jogador: `/balance`, `/jobs`, `/skills`, `/clan`, `/kingdom`, `/map`.
- Integração com LuckPerms usando permission nodes organizados (ex.: `earthcore.admin.*`, `earthcore.economy.balance`).

## Arquitetura de Alto Nível
- **Stack**: Java 21, Paper/Spigot 1.21.x+.
- **Estrutura interna**: monorepo em um único .jar, com pacotes por domínio (core, economy, jobs, kingdoms, clans, skills, web, infra).
- **Camadas**:
  - Domínio: serviços como `EconomyService`, `JobService`, `KingdomService`, `ClanService`, `SkillService`.
  - Infraestrutura: persistência (MySQL/PostgreSQL), configuração, gateways externos (Dynmap, Web APIs).
  - API/Bukkit: comandos, listeners de eventos e integrações com LuckPerms/Dynmap.
- **Eventos Bukkit**: uso intensivo para reagir a ações de jogo (quebras de bloco, combate, login, interação com NPCs) e despachar lógicas de jobs/skills/economia.
- **Configuração modular**: cada domínio possui seu próprio arquivo YAML, facilitando tuning sem recompilar.

## Instalação e Requisitos
- Java 21+.
- Paper ou Spigot 1.21.x (ou superior compatível).
- Dependências recomendadas/esperadas: Dynmap, LuckPerms, EssentialsX.
- Instalação: colocar o `EarthCore.jar` em `plugins/`, iniciar o servidor para gerar configs e reiniciar após ajustes.

### Frontend web embutido (Dynmap + login simples)
- O plugin sobe um servidor HTTP integrado que embeda o Dynmap em `http://<host>:<port>/` e oferece login por nome de usuário para exibir saldo, clã e reino.
- Configure `frontend.host`, `frontend.port` e `frontend.dynmapUrl` em `plugins/EarthCore/config.yml` (padrão: `0.0.0.0:8210` apontando para `http://localhost:8123/`).
- Veja `docs/frontend.md` para detalhes de uso, endpoints (`/api/user`) e boas práticas de exposição.

## Configuração Básica
Exemplos ilustrativos (valores fictícios):

```yml
# config.yml
storage:
  type: mysql
  host: localhost
  database: earthcore
  user: earth
  password: supersecreto

economy:
  currency: Coins
  start-balance: 1000
```

```yml
# economy.yml
transfer-fee: 0.02   # 2% por transferência
kingdom-tax-rate: 0.05
```

```yml
# jobs.yml
max-jobs-per-player: 2
xp-multiplier: 1.0
jobs:
  miner:
    display-name: "Minerador"
    payout:
      STONE: 0.5
      IRON_ORE: 2.0
    xp:
      STONE: 1
      IRON_ORE: 8
  farmer:
    display-name: "Fazendeiro"
    payout:
      WHEAT: 0.8
      CARROT: 0.6
```

```yml
# kingdoms.yml
create-cost: 5000
chunk-claim-cost: 250
upkeep-per-chunk: 50
roles:
  king:
    permissions: ["all"]
  advisor:
    permissions: ["manage.members", "manage.claims"]
  member:
    permissions: ["build", "interact"]
```

## Comandos e Permissões (Visão Geral)
- Economia: `/balance`, `/pay <jogador> <valor>`, `/coins add <jogador> <valor>` (admin).
- Jobs: `/jobs`, `/jobs join <job>`, `/jobs leave <job>`, `/jobs top`.
- Reinos: `/kingdom create`, `/kingdom invite`, `/kingdom claim`, `/kingdom setcapital`.
- Clãs: `/clan create`, `/clan invite`, `/clan home`, `/clan bank deposit`.
- Skills: `/skills`, `/skills top`, `/skills info <skill>`.
- Administração geral: `/earthcore reload`.

### Estrutura sugerida de permission nodes
- `earthcore.admin.*`
- `earthcore.economy.*` (ex.: `earthcore.economy.balance`, `earthcore.economy.pay`)
- `earthcore.jobs.*` (ex.: `earthcore.jobs.join`, `earthcore.jobs.admin`)
- `earthcore.kingdoms.*` (ex.: `earthcore.kingdoms.create`, `earthcore.kingdoms.claim`)
- `earthcore.clans.*` (ex.: `earthcore.clans.create`, `earthcore.clans.bank`)
- `earthcore.skills.*` (ex.: `earthcore.skills.view`, `earthcore.skills.admin`)
- `earthcore.npc.*`

## Roadmap / Futuras Implementações
- 🟢 Núcleo de economia e comandos básicos.
- 🟢 Sistema inicial de jobs com payouts configuráveis.
- 🟢 Reinos e clãs com claims, cargos e impostos.
- 🟡 Skills/MMO com árvore de benefícios.
- 🟡 Lojas de jogador e mercado global.
- 🟡 Integração completa com Dynmap (markers dinâmicos e áreas).
- 🟡 APIs REST/WebSocket para cliente web estilo RTS e automações.
- 🟠 Integração opcional com moeda externa/cripto (em estudo).

## Contribuição
O projeto poderá receber contribuições no futuro. Sugestão inicial: abrir issues com propostas, seguir o estilo de codificação (Java 21, Paper API) e manter documentação clara para cada módulo.

## Licença
A definir.
