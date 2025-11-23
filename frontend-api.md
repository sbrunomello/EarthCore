# EarthCore Frontend API

Documentação dos endpoints expostos pelo servidor HTTP embutido. Todas as rotas `*/secure/*` exigem header `Authorization: Bearer <token>` emitido pelo `POST /api/auth/login`.

## Autenticação

### `POST /api/auth/login`
Body JSON: `{ "username": "player", "password": "senha" }`

Retorna `{ token, uuid, username }` com expiração de 24h.

## Perfil e estatísticas

- `GET /api/secure/userinfo` → dados do jogador (saldo, clã, reino, coords online).
- `GET /api/secure/user/transactions` → últimas transações pessoais.
- `GET /api/secure/stats` → kills, mortes, horas jogadas.
- `GET /api/secure/claims` → claims do clã e reino relacionados.

## Reino

- `GET /api/secure/kingdom/info` → nome, tier, banco, claims, dívida.
- `GET /api/secure/kingdom/members` → lista de membros e cargos.
- `GET /api/secure/kingdom/bank` → saldo e histórico (quando disponível).
- `GET /api/secure/kingdom/claims` → claims do reino.
- `GET /api/secure/kingdom/upkeep` → visão de débito e saldo atual.
- `GET /api/secure/kingdom/transactions` → placeholder de histórico financeiro do reino.

## Clã

- `GET /api/secure/clan/info` → nome, tag e saldo.
- `GET /api/secure/clan/members` → membros e cargos.
- `GET /api/secure/clan/bank` → saldo e histórico (quando disponível).
- `GET /api/secure/clan/claim` → claim único do clã.
- `GET /api/secure/clan/transactions` → placeholder de histórico financeiro do clã.

## Economia e lojas

- `GET /api/secure/shops` → lojas do jogador logado.
- `GET /api/secure/shops/{id}` → detalhes e itens da loja.

## Jobs

- `GET /api/jobs` → tabela de jobs e pagamentos configurados.
- `GET /api/secure/jobs/current` → job atual do jogador (se houver).

## Portais

- `GET /api/secure/portals` → lista de portais configurados com localização e status.

## Legado

- `GET /api/user?username=name` → endpoint original sem autenticação para compatibilidade mínima.

## Segurança

- Rate limit: 30 requisições/min por IP (configurável via `frontend.rateLimitPerMinute`).
- CORS: permitido para qualquer origem por padrão (`*` em `frontend.allowedOrigins`), podendo ser restrito via configuração.
- JWT HMAC256 com emissor configurável em `frontend.jwtIssuer`.
