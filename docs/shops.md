# Lojas (NPCs + estoque físico)

## Fluxo rápido
- **Criação**: `/shop create <nome>` abre o setup de slots; variantes `/shop clan create` e `/shop kingdom create` exigem líder.
- **Edição**: clicar no aldeão dono abre a GUI de edição (somente dono). Jogadores comuns veem a GUI de compra.
- **Exclusão em massa**: `/shop delete <nome>` remove todas as lojas do jogador com o nome informado. Os baús são limpos e removidos, perdendo qualquer item armazenado.

## Comportamento do NPC
- O aldeão é criado olhando para quem executou o comando, permanece imóvel (AI e gravidade desligadas) e é marcado como invulnerável.
- Um listener (`NpcProtectionListener`) cancela dano e alvo de mobs para aldeões de **loja** e **portal**, garantindo que não sejam afetados mesmo por outros plugins.

## Armazenamento e persistência
- Dados ficam em `shops-data.yml` (pasta do plugin), incluindo localização do aldeão e baús. O salvamento é imediato após criar/editar ou excluir uma loja.
- Baús são registrados internamente para checar interações e são limpos/removidos ao deletar a loja para evitar duplicação de itens.

## Boas práticas
- Nomeie lojas de forma única para facilitar exclusões em lote.
- Sempre crie a loja em um claim onde você tenha permissão; lojas de clã/reino só podem existir dentro dos respectivos claims.
