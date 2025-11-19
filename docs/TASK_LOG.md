# Registro de Tarefas e Decisões

Este documento registra, em ordem cronológica, o que foi feito em cada tarefa, as principais decisões técnicas e como validar as alterações. Mantenha-o sempre atualizado para que qualquer pessoa consiga entender rapidamente o histórico do trabalho.

## 2025-11-20 – Melhoria de validação e resiliência do armazenamento
- **Contexto:** Endurecimento das operações de moeda para evitar valores inválidos e proteger comandos administrativos e de transferência.
- **Ações executadas:**
  - Validação de quantias contra valores negativos, `NaN` ou infinitos em operações de saldo.
  - Bloqueio de auto-transferências e de valores não positivos no `/pay`, com mensagens amigáveis ao usuário.
  - Parsing administrativo no `/eco` protegido contra quantidades não finitas.
  - Criação segura do arquivo `currency.yml` e registro de entradas malformadas no cache.
- **Riscos mitigados:** Corrupção de saldo, exploits por entradas inválidas e falhas ao inicializar o armazenamento.
- **Validação:** `bash gradlew test --console=plain --no-daemon`.

## 2025-11-20 – Documentação de processos
- **Contexto:** Solicitação para documentar de forma permanente tudo o que é feito em cada tarefa.
- **Ações executadas:**
  - Criação deste arquivo `docs/TASK_LOG.md` para registrar decisões e passos futuros.
  - Definição de um modelo de registro: contexto, ações, riscos mitigados e validação.
- **Próximos passos sugeridos:**
  - Adicionar novas entradas a cada tarefa concluída, detalhando decisões e cobertura de testes.
  - Referenciar comandos ou arquivos modificados sempre que possível.

> **Boas práticas:**
> - Descrever o impacto e o motivo de cada mudança.
> - Anotar comandos de teste executados, incluindo flags relevantes.
> - Registrar riscos mitigados ou pendências.
