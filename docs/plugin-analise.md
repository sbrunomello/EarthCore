# Análise de Prontidão – EarthCore

## 1. Resumo honesto do que já está funcional
- **Economia básica**: saldos persistentes em `currency.yml`, depósitos/saques/transferências e comandos `/bal`, `/pay`, `/eco` já operam com validação anti-valores inválidos e proteção contra autotransferência. Gera integração direta com reinos e clãs para recolher impostos. 【F:src/main/java/mello/EarthCore.java†L68-L123】【F:src/main/java/mello/currency/CurrencyService.java†L11-L43】【F:src/main/java/mello/currency/commands/PayCommand.java†L16-L89】
- **Jobs reativos**: cadastro simples de 1 job por jogador, pagamento automático por quebra de bloco ou abate de entidade, com tabelas de payout carregadas do YAML. Eventos já registrados no enable. 【F:src/main/java/mello/EarthCore.java†L73-L79】【F:src/main/java/mello/jobs/JobService.java†L32-L74】【F:src/main/java/mello/jobs/listeners/JobListener.java†L1-L83】
- **Reinos e Clãs**: criação com custo, convites, entrada/saída, banco compartilhado de reino e clã, claims de chunk (reino multi-claim; clã 1 claim) com validação de conflitos e recolhimento automático de impostos em `/pay`. Comandos e listeners ativos. 【F:src/main/java/mello/EarthCore.java†L80-L123】【F:src/main/java/mello/kingdoms/KingdomService.java†L19-L118】【F:src/main/java/mello/clans/ClanService.java†L16-L120】
- **BlueMap opcional**: se presente, desenha claims de reinos e clãs automaticamente e atualiza em tempo real via hooks. 【F:src/main/java/mello/EarthCore.java†L158-L190】
- **Chat por canais**: global, local (raio 100 blocos), clã, cidade (reino) e admin já funcionam com roteamento de destinatários e prefixos. 【F:src/main/java/mello/chat/ChatService.java†L22-L73】
- **Utilidades centrais**: spawn global, homes individuais, pedidos de teleporte (/tpa, /tphere), mensagens privadas (/msg, /reply) e portais de totem configuráveis via comando admin. Serviços e listeners registrados no ciclo de vida. 【F:src/main/java/mello/EarthCore.java†L94-L129】【F:src/main/resources/plugin.yml†L8-L47】
- **Frontend web embutido**: servidor HTTP interno entrega página que embeda BlueMap e expõe `/api/user` com saldo/clã/reino, com cache de assets e isolamento em executor próprio. Configurações em `config.yml`. 【F:src/main/java/mello/web/FrontendServer.java†L23-L121】【F:src/main/resources/config.yml†L13-L27】

## 2. O que está pela metade ou ainda frágil
- **Progressão e skills inexistentes**: README promete árvore de skills/MMO e progressão por job, mas não há implementação de XP, níveis ou benefícios além do payout direto. 【F:README.md†L34-L38】【F:src/main/java/mello/jobs/JobService.java†L32-L74】
- **Economia avançada**: mercado global, lojas de jogador, impostos complexos e sinks recorrentes não aparecem no código. Fluxo atual limita-se a transferências e depósitos simples. 【F:README.md†L39-L43】【F:src/main/java/mello/currency/CurrencyService.java†L11-L43】
- **Governança de reinos/clãs**: cargos são fixos e não existe sistema de permissões granulares, guerras, alianças ou manutenção diária. Claims não têm proteção real de blocos além de marcação visual (tochas) e taxação em transações. 【F:src/main/java/mello/kingdoms/KingdomService.java†L19-L205】【F:src/main/java/mello/clans/ClanService.java†L83-L174】
- **Auditoria e logs**: não há trilha de auditoria para operações sensíveis (saques, depos, claims) além de mensagens simples; risco de abuso/admin malicioso. 【F:src/main/java/mello/currency/commands/PayCommand.java†L46-L89】【F:src/main/java/mello/kingdoms/KingdomService.java†L97-L205】
- **Segurança do frontend**: autenticação é apenas por nome de usuário e leitura de dados, sem proteção CSRF/Auth real; adequado só para painel informativo. 【F:src/main/java/mello/web/FrontendServer.java†L59-L121】
- **Persistência**: tudo em YAML plano; sem migrations, bloqueio ou async. Para produção com muitos jogadores há risco de I/O lento e corrupção se o servidor travar. 【F:src/main/java/mello/currency/CurrencyStorage.java†L12-L60】【F:src/main/java/mello/jobs/JobStorage.java†L17-L58】

## 3. Buracos de produto
- **Proteção de terreno**: não existe interceptação de eventos de construção/uso para aplicar permissões de reino/clã; claims são apenas registros e tochas, sem impedir grief. Jogadores notarão que “claimar” não protege nada. 【F:src/main/java/mello/kingdoms/KingdomService.java†L123-L205】【F:src/main/java/mello/clans/ClanService.java†L83-L174】
- **Fluxos de cidade/nação**: ausência de capital, nações, alianças, guerras ou conquistas; a macro-política descrita no README ainda não existe, deixando o loop de dominação raso. 【F:README.md†L23-L33】【F:src/main/java/mello/kingdoms/KingdomService.java†L19-L205】
- **Economia sem sinks**: sem manutenção de claims, impostos recorrentes, custos de teleportes ou reparos; risco alto de inflação e riqueza infinita via farm/jogadores afk. 【F:src/main/java/mello/currency/CurrencyService.java†L11-L43】【F:src/main/java/mello/jobs/JobService.java†L32-L74】
- **UI/UX**: não há GUIs para jobs, clãs, reinos ou lojas; interações são só texto e comandos, o que reduz adesão em servidores modernos. 【F:README.md†L56-L60】【F:src/main/resources/plugin.yml†L8-L47】
- **Integração web limitada**: apenas leitura de dados; nada de comandos remotos, mapas dinâmicos com claims coloridos ou painel administrativo. 【F:src/main/java/mello/web/FrontendServer.java†L59-L121】

## 4. Fluxos críticos que precisam existir antes do lançamento
- **Proteção de terreno**: listeners de place/break/interact aplicando permissões por claim de reino/clã, flags para containers e pvp, e feedback claro ao jogador. (Crítico)
- **Economia saudável**: manutenção periódica de claims, taxas em homes/teleportes, sinks em upgrades de reino/clã e limites anti-abuso em jobs. (Crítico)
- **Progressão do jogador**: níveis de job/skill com bonificações, desbloqueios e limites por player para evitar multiconta farm. (Importante)
- **Governança**: cargos configuráveis, transferência de liderança, alianças/guerra e auditoria de banco/claims. (Importante)
- **Ferramentas admin**: comandos de reset parcial (economia, clãs, reinos), logs de ações sensíveis e rollback mínimo para portais/homes. (Importante)
- **BlueMap/visualização**: desenhar claims em tempo real com cores e labels, além de bordas/popup com dono e impostos; hoje é apenas hook vazio de apresentação. (Importante)
- **Integração web**: auth real (token/whitelist de IP), endpoints seguros e, se for exposto, limiter de requisições. (Importante)

## 5. Maturidade dos sistemas (0–10)
- Economia básica: **6/10** – comandos e impostos funcionam, mas sem sinks, sem limites antifraude e sem backend robusto. 【F:src/main/java/mello/currency/CurrencyService.java†L11-L43】
- Jobs: **5/10** – paga por eventos, mas só 1 job ativo, sem níveis ou balanceamento de ganhos. 【F:src/main/java/mello/jobs/JobService.java†L32-L74】
- Reinos/claims: **4/10** – criação/claim/tesouro ok, porém sem proteção de blocos, sem manutenção e sem guerra/alianças. 【F:src/main/java/mello/kingdoms/KingdomService.java†L90-L205】
- Clãs: **5/10** – banco e convite funcionam; apenas 1 claim e sem progressão ou perks. 【F:src/main/java/mello/clans/ClanService.java†L43-L174】
- Experiência/skills: **1/10** – apenas mencionado em docs, nenhuma implementação real. 【F:README.md†L34-L38】
- Eventos competitivos/guerras: **1/10** – inexistentes. 【F:README.md†L23-L33】
- Integração web: **5/10** – painel informativo com BlueMap embed, mas sem segurança forte ou ações remotas. 【F:src/main/java/mello/web/FrontendServer.java†L59-L121】
- UI/menus: **2/10** – tudo via texto/comando, sem GUIs ou painéis in-game. 【F:src/main/resources/plugin.yml†L8-L47】
- Comandos de player: **7/10** – básicos de economia, teleporte, chat, portais e social estão presentes. 【F:src/main/resources/plugin.yml†L8-L47】
- Admin tools: **3/10** – faltam auditorias, resets, logs estruturados e configurações avançadas. 【F:src/main/java/mello/EarthCore.java†L68-L190】

## 6. O que falta para ser “Production Ready”
- **Crítico (MUST)**
  - Proteção real de claims (blocagem de break/place/interact, flags pvp/container) para reinos e clãs.
  - Sinks e manutenção econômica: custo periódico por claim, taxas em tp/home/portais, limites de payout e antifarm.
  - Progressão mínima: níveis de jobs/skills com rewards graduais e limites por conta.
  - Auditoria/logs para depósitos/saques/claims e comandos administrativos.
  - Endurecer frontend: autenticação real e restrições de exposição se for usado publicamente.

- **Importante (SHOULD)**
  - GUIs para jobs, clãs, reinos e mercado para reduzir fricção de comandos.
  - Alianças/guerra/relações diplomáticas entre reinos; capital/nações.
  - Ferramentas de administração: resets seletivos, export/import de dados, modo manutenção.
  - Visualização BlueMap aprimorada (cores, labels, popups com owner/impostos, refresh automático).

- **Opcional (NICE TO HAVE)**
  - Mercado global/leilão, lojas de player e NPCs utilitários.
  - Integração web bidirecional (ordens RTS, painel admin) com autenticação forte.
  - Missões/quests e eventos sazonais para retenção.

## 7. Avaliação final
- **Nota geral de prontidão**: **4/10** – núcleo funcional para testes internos, mas sem os fluxos críticos de proteção, progressão e economia para players reais.
- **Riscos reais ao lançar agora**: griefing livre mesmo com claims; inflação rápida; abuso de `/pay` e banco sem auditoria; frontend exposto sem autenticação; ausência de progressão e guerra deixando o servidor sem meta de longo prazo.
- **Principais oportunidades de evolução rápida**: adicionar listeners de proteção de blocos, custos recorrentes de claim e níveis básicos de job/skill; reforçar autenticação do frontend e criar logs de auditoria.
- **Prognóstico**: focando em (a) proteção de terrenos + sinks econômicos e (b) progressão/jobs/skills, é viável chegar a um MVP jogável em 2–3 semanas de trabalho dedicado, antes de atacar GUIs e guerra/alianças.
