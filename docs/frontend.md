# Frontend Web Embutido (BlueMap + Contexto do Jogador)

Este documento resume onde o frontend está localizado no projeto, como ele é servido pelo plugin e como utilizá-lo em produção/local.

## Onde ficam os arquivos
- **Código do servidor web**: `src/main/java/mello/web/FrontendServer.java` e `FrontendSettings.java` – responsáveis por inicializar/parar o HTTP server, expor os endpoints (`/`, `/assets/app.js`, `/api/user`) e ler a configuração.
- **Recursos estáticos**: `src/main/resources/web/index.html` e `src/main/resources/web/app.js` – HTML/JS empacotados no jar e servidos diretamente pelo servidor embutido.
- **Configuração**: seção `frontend` em `src/main/resources/config.yml` controla host, porta, URL do BlueMap e se o módulo está habilitado.

## Como funciona
- Ao iniciar o plugin, o `FrontendServer` sobe automaticamente (quando `frontend.enabled: true`) e atende em `http://<host>:<port>/`.
- A página principal embeda o BlueMap via `<iframe>` usando `frontend.mapUrl` (padrão: `http://localhost:8100/`).
- O login é apenas por nome de usuário. O frontend faz `GET /api/user?username=<nick>` e recebe JSON com saldo, clã e reino, consultando os serviços internos do plugin.
- Todos os assets são servidos do próprio jar, evitando dependência externa e mantendo o conteúdo sincronizado com o plugin.
- A compra de gems pelo painel é mockada: o endpoint `/api/secure/gems/purchase` credita o pacote selecionado direto no saldo in-game usando a currency `GEMS` e registra a transação como `GEM_PURCHASE`.

## Como usar no servidor
1. **Habilite e configure** em `plugins/EarthCore/config.yml`:
   ```yml
   frontend:
     enabled: true
     host: "0.0.0.0"   # ajuste para 127.0.0.1 se quiser expor apenas localmente
     port: 8210         # altere se houver conflito
     mapUrl: "http://localhost:8100/"  # URL pública do BlueMap já existente
   ```
2. **Reinicie ou use `/earthcore reload`** para aplicar.
3. **Acesse** `http://<host>:<port>/` no navegador. Informe o username para carregar o contexto do jogador e visualizar o BlueMap já embedado.

## Boas práticas e observações
- Garanta que o BlueMap esteja acessível no endereço configurado em `mapUrl` para que o iframe carregue corretamente.
- Mantenha o `host` restrito (ex.: `127.0.0.1`) e faça proxy/reverse proxy se precisar expor publicamente, adicionando TLS no proxy.
- A resposta de `/api/user` é sensível às dependências internas (Economia, Reinos, Clãs); se algum serviço estiver desabilitado, ajuste o frontend conforme necessário.
- Logs do servidor web aparecem junto ao logger do plugin. Em caso de porta ocupada ou falha de bind, o plugin continua ativo e registra o erro.
- A listagem de pacotes de gems está disponível em `/api/secure/gems/packs` e pode ser atualizada no código Java; ela serve apenas para UI/UX e não realiza cobrança real.
