# Lojas (NPCs + estoque físico)

## Fluxo rápido
- **Criação**: `/shop create <nome>` entrega um bastão de posicionamento. Clique em um bloco para marcar onde o aldeão ficará, clique novamente para rotacionar 90º a orientação do aldeão/baú e finalize com `/shop confirm`. Variantes `/shop clan create` e `/shop kingdom create` respeitam as mesmas regras de liderança.
- **Edição**: clicar no aldeão dono abre a GUI de edição (somente dono). Jogadores comuns veem a GUI de compra.
- **Exclusão em massa**: `/shop delete <nome>` remove todas as lojas do jogador com o nome informado. Os baús são limpos e removidos, perdendo qualquer item armazenado.

## Posicionamento com bastão
- O bastão personalizado traz o tipo (pessoal, clã ou reino) no nome e só funciona para o jogador que iniciou o fluxo.
- O bloco clicado fica destacado em vermelho (apenas para o jogador) e define o ponto base do aldeão; o baú é posicionado no bloco imediatamente acima das duas bases atrás do aldeão.
- O aldeão e o baú são criados apenas após `/shop confirm`, permitindo ajustar o local até não haver obstáculos.

## Comportamento do NPC
- O aldeão é criado olhando para quem executou o comando, permanece imóvel (AI e gravidade desligadas) e é marcado como invulnerável.
- Um listener (`NpcProtectionListener`) cancela dano e alvo de mobs para aldeões de **loja** e **portal**, garantindo que não sejam afetados mesmo por outros plugins.

## Armazenamento e persistência
- Dados ficam em `shops-data.yml` (pasta do plugin), incluindo localização do aldeão e baús. O salvamento é imediato após criar/editar ou excluir uma loja.
- Baús são registrados internamente para checar interações e são limpos/removidos ao deletar a loja para evitar duplicação de itens.

## Boas práticas
- Nomeie lojas de forma única para facilitar exclusões em lote.
- Sempre crie a loja em um claim onde você tenha permissão; lojas de clã/reino só podem existir dentro dos respectivos claims.
