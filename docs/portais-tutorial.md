# Portais por Totem (Guias para Admins)

Este guia descreve como configurar portais interativos ativados por totens e como teleportar para qualquer coordenada sendo operador (OP). O foco é permitir hubs continentais com portais no spawn principal e totens espalhados pelo mapa.

## Pré-requisitos
- Permissão `core.portal` ou ser operador (OP).
- Servidor já carregado com o mundo alvo (necessário para salvar coordenadas válidas).
- Totem físico (qualquer bloco sólido) que será usado como ponto de clique.

## Visão rápida dos comandos
- `/portal settarget <nome>`: salva o destino do portal com base na sua posição atual (inclui yaw/pitch).
- `/portal settotem <nome>`: vincula o bloco que você está mirando (raio de 6 blocos) como totem de ativação.

> Dica: use nomes simples (ex.: `africa`, `asia`, `europa`) para facilitar suporte e auditoria.

## Criar um portal completo (passo a passo)
1. **Chegue nas coordenadas finais**
   - Se já sabe as coordenadas, use `/tp <x> <y> <z> [yaw pitch]` como OP e confirme que o chunk está carregado.
   - Ajuste a rotação olhando na direção desejada para o jogador spawnar corretamente.
2. **Defina o destino**
   - Execute `/portal settarget <nome>` enquanto está no ponto final.
   - O plugin grava mundo, X/Y/Z e também yaw/pitch da sua visão.
3. **Escolha o totem**
   - Vá até o bloco que funcionará como totem (no spawn ou na área temática).
   - Mire exatamente no bloco (até 6 blocos de distância) e rode `/portal settotem <nome>`.
   - O plugin recusa blocos já usados por outro portal; troque de bloco se receber aviso.
4. **Teste e refine**
   - Como jogador, clique com botão direito no totem para validar o teleporte.
   - Se quiser mudar apenas o destino, repita `/portal settarget <nome>` direto no local novo — não precisa refazer o totem.

## Teleportar para qualquer coordenada como OP
1. Use `/tp <x> <y> <z> [yaw pitch]` para chegar exatamente na posição desejada, mesmo que esteja em outro mundo.
2. Ajuste posição fina com modo criativo/espectador e alinhe a câmera (yaw/pitch) como referência para o portal.
3. Confirme com `/portal settarget <nome>` para salvar essa coordenada como destino do portal correspondente.
4. Opcional: abra o `config.yml` e verifique a seção `portals.<nome>` para auditar valores gravados (mundos precisam estar carregados antes de uso).

## Boas práticas
- Mantenha um portal principal para `/spawn` e `/home`; use totens apenas para hubs temáticos/continentais.
- Evite colocar totens em blocos que serão quebrados com frequência (use materiais protegidos ou área com proteção de construção).
- Após grandes mudanças no mapa, revise os portais com `/portal settarget` para evitar destinos em blocos removidos.
- Sempre reinicie ou use `/earthcore reload` apenas após garantir que os mundos referenciados foram carregados (mundos ausentes são ignorados na leitura).
