# Feature Specification: MVP Sistema de Relatórios para Escola de Música

**Feature Branch**: `001-mvp-relatorios-escola-musica`

**Created**: 2026-08-13

**Status**: Draft

**Input**: User description: "criar o primeiro mvp para contemplar toda a necessidade de negócio inicial, desde a configuração do n8n até a construção final do relatorio semestral. use o MVP_Escola_de_Musica_IA_n8n_Telegram_Docker.md de base para montar o specify"

## Clarifications

### Session 2026-08-13

- Q: Como um professor autentica na interface web (para consultar histórico, US4), e essa autenticação é a mesma usada pelo administrador para cadastro? → A: O professor não tem login na interface web; sua única interface é o bot do Telegram (orquestrado pelo n8n). A interface web administrativa é de uso exclusivo do administrador (cadastro de professores/alunos e consulta de histórico); professores consultam seu próprio histórico também pelo Telegram.
- Q: Por quanto tempo o sistema deve manter o áudio original enviado pelo professor antes de excluí-lo? → A: O áudio não é retido; é usado somente como insumo para a transcrição e é excluído imediatamente após a transcrição ser concluída com sucesso (mantendo apenas a transcrição em texto e o relatório gerado).
- Q: Como o sistema deve identificar unicamente um aluno para evitar duplicidade de cadastro? → A: O CPF do aluno (ou do responsável, se menor) é usado como identificador único, armazenado de forma criptografada no banco de dados.
- Q: De que forma o professor informa o período desejado ao pedir o relatório semestral pelo Telegram? → A: O bot apresenta um menu com opções pré-definidas de semestre (ex.: "1º semestre 2026", "2º semestre 2026") para o professor escolher, em vez de digitar datas livremente.
- Q: Existe algum limite de duração ou tamanho para o áudio que o professor envia pelo Telegram? → A: Não há limite de negócio adicional no MVP; aplica-se apenas o limite técnico padrão do Telegram/API de transcrição.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar aula por áudio no Telegram e receber relatório (Priority: P1)

Um professor, logo após dar uma aula, abre o bot no Telegram, seleciona o aluno atendido e envia um
áudio narrando o que foi trabalhado. O sistema transcreve o áudio, estrutura o conteúdo em um
relatório (conteúdos trabalhados, evolução, dificuldades, tarefas, observações), pergunta ao
professor caso falte alguma informação relevante, e gera um arquivo em PDF desse relatório para o
professor baixar e conferir se está aderente ao que foi trabalhado na aula. Somente depois que o
professor revisar o PDF e confirmar (aprovar) é que o relatório fica salvo no histórico do aluno.

**Why this priority**: É a dor principal do cliente e a hipótese central do MVP — substituir a
digitação manual do relatório de aula por um fluxo de voz assistido por IA. Sem essa jornada
funcionando, não há produto validável.

**Independent Test**: Pode ser testado de ponta a ponta com um professor e um aluno já cadastrados
(mesmo que via carga inicial de dados): enviar `/relatorio`, escolher o aluno, enviar um áudio de
aula e verificar que um relatório estruturado é apresentado e, após aprovação, passa a constar no
histórico do aluno.

**Acceptance Scenarios**:

1. **Given** um professor com conta vinculada ao Telegram e pelo menos um aluno cadastrado, **When**
   ele envia o comando de registro de aula, seleciona o aluno e envia um áudio descrevendo a aula,
   **Then** o sistema apresenta um relatório estruturado (conteúdo, evolução, dificuldades, tarefas,
   observações) baseado no áudio enviado.
2. **Given** um relatório estruturado gerado a partir do áudio, **When** o sistema termina de
   estruturar o conteúdo, **Then** o sistema disponibiliza um arquivo em PDF desse relatório para o
   professor baixar e conferir antes de decidir se aprova.
3. **Given** um PDF de relatório disponibilizado para conferência, **When** o professor confirma que
   o conteúdo está aderente ao que foi trabalhado na aula, **Then** o relatório é salvo vinculado ao
   aluno, ao professor e à data da aula, e fica disponível no histórico.
4. **Given** um PDF de relatório disponibilizado para conferência, **When** o professor identifica que
   o conteúdo não está aderente e solicita uma alteração em linguagem natural (ex.: "mude a
   dificuldade para ritmo"), **Then** o sistema estrutura uma nova versão do relatório refletindo a
   alteração e gera um novo PDF para conferência, antes de pedir aprovação novamente.
5. **Given** que a IA não conseguiu extrair uma informação relevante do áudio (ex.: tarefa de casa),
   **When** o relatório é gerado, **Then** o sistema pergunta ativamente ao professor sobre essa
   informação antes de gerar o PDF para conferência.
6. **Given** um relatório ainda não aprovado, **When** o professor não confirma a aderência do PDF
   (nem aprova, nem solicita alteração), **Then** o relatório permanece pendente de revisão e não é
   salvo como aprovado no histórico do aluno.

---

### User Story 2 - Cadastrar professores, alunos e vincular conta do Telegram (Priority: P2)

Um administrador cadastra professores e alunos na interface web administrativa. Cada professor,
no primeiro contato com o bot do Telegram, precisa vincular sua conta do Telegram ao seu cadastro
no sistema usando um código de vinculação exibido na interface web, para que o bot saiba
identificar quem está enviando a aula.

**Why this priority**: É pré-requisito operacional para a User Story 1 funcionar com dados reais
(sem professor e aluno cadastrados e sem vínculo Telegram↔professor, o fluxo de registro de aula
não tem quem nem o quê registrar), mas não é, por si só, a entrega de valor central do produto.

**Independent Test**: Pode ser testado isoladamente criando um professor e um aluno pela interface
web, gerando um código de vinculação, e confirmando pelo Telegram que a conta foi associada
corretamente ao professor — sem depender do fluxo de áudio.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado na interface web, **When** ele cadastra um novo professor,
   **Then** o sistema gera um código de vinculação único para esse professor.
2. **Given** um professor recém-cadastrado ainda não vinculado, **When** ele abre o bot do Telegram
   pela primeira vez e informa o código de vinculação, **Then** sua conta do Telegram passa a ser
   associada ao professor correspondente e ele pode iniciar o fluxo de registro de aula.
3. **Given** um administrador autenticado, **When** ele cadastra um novo aluno e o associa a um ou
   mais professores, **Then** esse aluno passa a estar disponível na lista de seleção do bot para os
   professores associados a ele.
4. **Given** uma conta do Telegram ainda não vinculada a nenhum professor, **When** o usuário tenta
   usar comandos de registro de aula, **Then** o sistema solicita a vinculação antes de prosseguir.

---

### User Story 3 - Gerar relatório semestral consolidado (Priority: P3)

Um professor solicita, pelo Telegram, o relatório semestral de um aluno. O sistema busca os
relatórios de aula registrados no período informado, consolida essas informações por IA em um
relatório pedagógico semestral completo (seguindo a estrutura de referência da escola: informações
gerais do aluno, frequência e estudo, técnica, musicalidade, leitura e memorização, pontos de
atenção, estratégias pedagógicas aplicadas, acompanhamento familiar, planejamento do próximo
semestre e parecer final) e gera um arquivo em PDF para o professor baixar, conferir se está
aderente à evolução real do aluno e só então aprovar.

**Why this priority**: Atende à segunda dor de negócio do cliente (produção do relatório semestral
consolidado a cada 6 meses), mas depende de já existir um histórico de relatórios de aula gerado
pela User Story 1, por isso vem depois dela em prioridade.

**Independent Test**: Pode ser testado isoladamente com um aluno que já tenha relatórios de aula
no histórico (carregados previamente): solicitar o relatório semestral pelo Telegram, informar
aluno e período, e verificar que um relatório consolidado é gerado a partir dos relatórios
existentes e pode ser aprovado.

**Acceptance Scenarios**:

1. **Given** um aluno com relatórios de aula registrados em um período, **When** o professor solicita
   o relatório semestral desse aluno pelo Telegram e informa o período desejado, **Then** o sistema
   apresenta quantos relatórios de aula foram encontrados no período antes de gerar a consolidação.
2. **Given** a confirmação do professor para gerar o relatório semestral, **When** o sistema processa
   os relatórios do período, **Then** um relatório consolidado é estruturado cobrindo, no mínimo:
   informações gerais do aluno, frequência e estudo, técnica (conteúdos trabalhados, evolução, pontos
   a desenvolver), musicalidade, leitura e memorização, pontos de atenção, estratégias pedagógicas
   aplicadas, acompanhamento familiar, planejamento do próximo semestre (metas, repertório proposto,
   estudos técnicos) e parecer final.
3. **Given** um relatório semestral estruturado, **When** o sistema termina de consolidá-lo, **Then**
   o sistema disponibiliza um arquivo em PDF desse relatório semestral para o professor baixar e
   conferir antes de decidir se aprova.
4. **Given** um PDF de relatório semestral disponibilizado para conferência, **When** o professor
   confirma que o conteúdo está aderente à evolução real do aluno, **Then** o relatório semestral fica
   salvo e disponível no histórico do aluno, distinto dos relatórios de aula individuais.
5. **Given** um PDF de relatório semestral disponibilizado para conferência, **When** o professor
   identifica divergências e solicita alteração em linguagem natural, **Then** o sistema gera uma nova
   versão do relatório e um novo PDF para conferência, antes de pedir aprovação novamente.
6. **Given** um aluno sem nenhum relatório de aula registrado no período solicitado, **When** o
   professor solicita o relatório semestral, **Then** o sistema informa que não há dados suficientes
   e não gera um relatório vazio nem um PDF.

---

### User Story 4 - Consultar histórico de relatórios na interface web (Priority: P4)

Um administrador acessa a interface web administrativa para consultar o histórico de relatórios de
aula e relatórios semestrais de qualquer aluno. O professor não possui login na interface web; ele
consulta o histórico dos seus próprios alunos exclusivamente pelo bot do Telegram.

**Why this priority**: Complementa a experiência oferecendo um canal de consulta mais adequado para
revisão de histórico completo, mas não é crítico para validar a hipótese central do MVP (que é o
fluxo de registro por voz) — pode ser entregue por último sem comprometer o valor das demais
histórias.

**Independent Test**: Pode ser testado isoladamente acessando a interface web com um administrador
autenticado e um aluno que já possua relatórios (de aula e/ou semestral) previamente cadastrados,
verificando que a listagem e o detalhe de cada relatório são exibidos corretamente; e, em paralelo,
verificando pelo Telegram que um professor consegue consultar o histórico dos seus próprios alunos.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado na interface web, **When** ele acessa o histórico de um
   aluno, **Then** o sistema lista todos os relatórios de aula e relatórios semestrais aprovados
   desse aluno, em ordem cronológica.
2. **Given** um professor vinculado ao Telegram, **When** ele solicita pelo bot o histórico de um
   aluno associado a ele, **Then** o sistema lista, na conversa do Telegram, os relatórios de aula e
   semestrais aprovados desse aluno, em ordem cronológica.
3. **Given** um professor vinculado ao Telegram, **When** ele solicita pelo bot o histórico de um
   aluno que não está associado a ele, **Then** o sistema nega o acesso.

---

### Edge Cases

- O que acontece quando o áudio enviado pelo professor não pode ser processado (arquivo corrompido,
  formato inválido, silêncio)? O sistema deve informar o problema e pedir novo envio, sem criar um
  relatório vazio.
- Não há limite de negócio adicional para duração/tamanho do áudio no MVP; aplica-se apenas o limite
  técnico padrão do Telegram/API de transcrição. Se o áudio exceder esse limite técnico, o sistema
  deve informar o professor e pedir novo envio, sem criar um relatório vazio.
- Como o sistema lida com transcrição de baixa qualidade ou incompreensível? Deve avisar o professor
  e solicitar reenvio do áudio ou complemento por texto.
- O que acontece se o professor tentar registrar uma aula sem antes selecionar um aluno, ou
  selecionar um aluno que não está associado a ele?
- O que acontece se a IA retornar uma estrutura de dados incompleta ou inválida ao tentar organizar
  o conteúdo do áudio? O sistema deve tentar validar/corrigir antes de expor o problema ao professor.
- O que acontece se o professor abandonar o fluxo no meio (ex.: não responde a uma pergunta de
  refinamento)? O estado da conversa deve poder ser retomado ou reiniciado sem duplicar relatórios.
- O que acontece se dois relatórios de aula forem enviados para o mesmo aluno na mesma data?
  Ambos devem ser preservados como registros distintos (podem ser aulas diferentes no mesmo dia).
- O que acontece se um professor tentar acessar ou gerar relatório (de aula ou semestral) de um
  aluno que não está associado a ele? O acesso deve ser negado.
- O que acontece se a conta do Telegram de um professor for usada por outra pessoa sem autorização?
  A vinculação por código, sozinha, não deve ser considerada autenticação suficiente para ações
  administrativas sensíveis fora do fluxo normal de registro de aula.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE permitir que um administrador cadastre professores e alunos através de
  uma interface web administrativa.
- **FR-001a**: O sistema DEVE identificar cada aluno de forma única pelo CPF (do aluno ou do
  responsável, quando menor de idade), impedindo cadastro duplicado com o mesmo CPF, e armazenando
  esse dado de forma criptografada no banco de dados.
- **FR-002**: O sistema DEVE gerar um código de vinculação único por professor, usado para associar
  a conta do Telegram desse professor ao seu cadastro no sistema.
- **FR-003**: O sistema DEVE impedir que um usuário do Telegram ainda não vinculado a um professor
  utilize os comandos de registro ou consulta de relatórios, solicitando a vinculação primeiro.
- **FR-004**: O sistema DEVE permitir que o professor, pelo Telegram, selecione um aluno dentre os
  alunos associados a ele antes de iniciar o envio do áudio de uma aula.
- **FR-005**: O sistema DEVE receber um áudio enviado pelo professor pelo Telegram e convertê-lo em
  texto (transcrição).
- **FR-005a**: O sistema NÃO DEVE reter o arquivo de áudio original além do necessário para a
  transcrição; o áudio DEVE ser excluído imediatamente após a transcrição ser concluída com sucesso,
  mantendo-se apenas o texto transcrito e o relatório gerado a partir dele.
- **FR-006**: O sistema DEVE transformar a transcrição da aula em um relatório estruturado contendo,
  no mínimo: conteúdos trabalhados, evolução, dificuldades, atividades/tarefas propostas e
  observações.
- **FR-007**: O sistema DEVE identificar quando informações relevantes não puderem ser extraídas do
  áudio e perguntar ativamente ao professor por essas informações antes de finalizar o relatório.
- **FR-008**: O sistema DEVE gerar um arquivo em PDF do relatório de aula estruturado e disponibilizá-lo
  para download pelo professor, como etapa de revisão obrigatória antes da aprovação.
- **FR-008a**: O sistema DEVE permitir que o professor, após conferir o PDF do relatório, aprove,
  solicite alteração em linguagem natural, ou cancele o registro; o relatório NÃO DEVE ser salvo como
  aprovado sem essa confirmação explícita do professor sobre o conteúdo do PDF.
- **FR-009**: O sistema DEVE permitir que o professor solicite alterações no relatório em linguagem
  natural, gerar uma nova versão do relatório e um novo PDF refletindo a alteração, antes de pedir
  aprovação novamente.
- **FR-010**: O sistema DEVE persistir o relatório de aula aprovado, associado ao aluno, ao professor,
  à data da aula, ao PDF gerado e mantendo a transcrição de origem.
- **FR-011**: O sistema DEVE permitir que o professor solicite, pelo Telegram, um relatório semestral
  de um aluno específico, selecionando o período desejado a partir de um menu com opções
  pré-definidas de semestre (ex.: "1º semestre 2026", "2º semestre 2026"), em vez de digitar datas
  livremente.
- **FR-012**: O sistema DEVE informar ao professor quantos relatórios de aula foram encontrados no
  período solicitado antes de gerar o relatório semestral.
- **FR-013**: O sistema DEVE consolidar, via IA, os relatórios de aula do período em um único
  relatório pedagógico semestral contendo, no mínimo, as seguintes seções (modelo de referência da
  escola): informações gerais do aluno (nome, idade, nível, professor, período e semestre
  correspondente); frequência e estudo (frequência às aulas, regularidade de estudo em casa,
  engajamento, comentário geral); técnica (conteúdos trabalhados, evolução técnica, pontos a
  desenvolver, estudos realizados); musicalidade (repertório estudado, avaliação de
  som/ritmo/fraseado/articulação/estilo, comentário musical); leitura e memorização; pontos de
  atenção; estratégias pedagógicas aplicadas; acompanhamento familiar (nível de presença dos
  responsáveis e observação do professor); planejamento do próximo semestre (metas técnicas,
  musicais e de hábitos de estudo, repertório proposto, estudos técnicos, leitura e teoria, projeto
  do semestre); e parecer final do professor.
- **FR-013a**: O sistema DEVE gerar um arquivo em PDF do relatório semestral consolidado, seguindo a
  estrutura descrita em FR-013, e disponibilizá-lo para download pelo professor como etapa de
  revisão obrigatória antes da aprovação.
- **FR-014**: O sistema NÃO DEVE gerar um relatório semestral (nem seu PDF) quando não houver nenhum
  relatório de aula no período solicitado, informando o professor sobre a ausência de dados.
- **FR-015**: O sistema DEVE permitir que, após conferir o PDF do relatório semestral, o professor
  aprove, solicite alteração em linguagem natural (gerando nova versão e novo PDF), ou cancele; o
  relatório semestral NÃO DEVE ser salvo como aprovado sem essa confirmação explícita do professor
  sobre o conteúdo do PDF.
- **FR-016**: O sistema DEVE persistir o relatório semestral aprovado e o respectivo PDF,
  distinguindo-o dos relatórios de aula individuais, associado ao aluno e ao período coberto.
- **FR-017**: O sistema DEVE permitir a consulta, pela interface web (restrita ao administrador), do
  histórico completo de relatórios de aula e relatórios semestrais de qualquer aluno, em ordem
  cronológica.
- **FR-017a**: O sistema DEVE permitir que o professor, exclusivamente pelo Telegram, consulte o
  histórico de relatórios de aula e relatórios semestrais dos alunos associados a ele, em ordem
  cronológica; o professor NÃO possui login na interface web.
- **FR-018**: O sistema DEVE restringir o acesso de cada professor apenas aos alunos e relatórios
  associados a ele; um professor não pode consultar, gerar ou aprovar relatórios de alunos que não
  estejam sob sua responsabilidade.
- **FR-019**: O sistema DEVE informar ao professor, de forma clara, quando um áudio não puder ser
  processado ou a transcrição resultar em baixa confiança, solicitando novo envio sem criar um
  relatório vazio ou incompleto.
- **FR-020**: O sistema NÃO DEVE tratar o identificador da conta do Telegram como prova suficiente de
  autenticação para ações administrativas sensíveis (ex.: cadastro de professores/alunos), que
  permanecem restritas à interface web autenticada.
- **FR-021**: O sistema DEVE registrar, para cada relatório, professor, aluno, data e status
  (rascunho/pendente de revisão/aprovado), permitindo rastreabilidade de todo o histórico.

### Key Entities

- **Professor**: pessoa que ministra aulas e usa o Telegram para registrar relatórios; possui
  cadastro na interface web, uma conta do Telegram vinculada e um conjunto de alunos associados.
- **Aluno**: pessoa (possivelmente menor de idade) que recebe aulas; possui cadastro na interface
  web e um histórico de relatórios de aula e relatórios semestrais. É identificado de forma única
  pelo CPF (do próprio aluno ou do responsável, quando menor de idade), armazenado de forma
  criptografada no banco de dados.
- **Aula**: evento pontual de ensino entre um professor e um aluno, com data e associada a um único
  relatório de aula.
- **Relatório de Aula**: registro estruturado gerado a partir do áudio de uma aula, contendo
  conteúdos trabalhados, evolução, dificuldades, tarefas e observações; possui status
  (pendente/aprovado), mantém a transcrição de origem e um PDF gerado para conferência do professor.
- **Relatório Semestral**: consolidação de múltiplos relatórios de aula de um aluno em um período
  (tipicamente 6 meses), estruturado conforme o modelo pedagógico de referência da escola
  (informações gerais, frequência e estudo, técnica, musicalidade, leitura e memorização, pontos de
  atenção, estratégias pedagógicas, acompanhamento familiar, planejamento do próximo semestre e
  parecer final); possui status próprio de revisão/aprovação e um PDF gerado para conferência do
  professor.
- **Vínculo Telegram-Professor**: associação entre o identificador de usuário do Telegram e o
  cadastro de um professor, criada por meio de um código de vinculação de uso único.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um professor consegue registrar o relatório de uma aula, do início do áudio até a
  aprovação final, em menos de 3 minutos de interação ativa.
- **SC-002**: Pelo menos 90% dos relatórios de aula gerados a partir do áudio são aprovados pelo
  professor com no máximo uma rodada de alteração/refinamento.
- **SC-003**: O tempo total gasto pelo professor para documentar uma aula é reduzido em pelo menos
  50% em comparação ao processo manual anterior (redigir o relatório por escrito).
- **SC-004**: Um professor consegue gerar um relatório semestral consolidado de um aluno, do pedido
  até a aprovação, em menos de 5 minutos de interação ativa, para um histórico de até 30 aulas.
- **SC-005**: 100% dos relatórios (de aula e semestrais) ficam corretamente vinculados ao aluno e
  professor corretos, sem misturar dados entre alunos ou professores diferentes.
- **SC-006**: Um novo professor consegue concluir a vinculação de sua conta do Telegram ao cadastro
  do sistema em menos de 2 minutos, sem suporte manual.
- **SC-007**: Nenhum professor consegue visualizar, gerar ou aprovar relatório de um aluno que não
  esteja associado a ele, verificado por teste de controle de acesso.
- **SC-008**: 100% dos relatórios (de aula e semestrais) são disponibilizados em PDF para download e
  conferência do professor antes de serem salvos como aprovados; nenhum relatório é salvo como
  aprovado sem essa etapa de revisão.

## Assumptions

- O MVP atende inicialmente a uma única escola de música (um único cliente/tenant); suporte a
  múltiplas escolas isoladas entre si não está no escopo desta primeira entrega.
- O cadastro de professores e alunos é feito por um administrador através da interface web; não há
  autoatendimento (self-service) de cadastro nesta primeira entrega.
- O Telegram é o único canal de entrada de áudio no MVP; outros canais (WhatsApp, upload direto na
  web) ficam fora de escopo desta entrega, conforme o documento de referência do produto.
- A geração do relatório semestral é sempre sob demanda, iniciada pelo professor pelo Telegram;
  geração automática/agendada (ex.: aviso automático quando o aluno completa 6 meses) fica fora de
  escopo desta primeira entrega.
- Um relatório semestral cobre um período de 6 meses (semestre), selecionado pelo professor a partir
  de um menu de opções pré-definidas apresentado pelo bot (ex.: semestres disponíveis a partir da
  data de cadastro do aluno), sem digitação livre de datas.
- A aprovação de relatórios (de aula e semestrais) é feita exclusivamente pelo professor responsável
  pelo aluno; não há fluxo de aprovação por um segundo revisor nesta primeira entrega.
- Falhas de transcrição ou de estruturação por IA são tratadas com nova tentativa solicitada ao
  professor, não com preenchimento manual alternativo dentro do MVP.
- A estrutura do PDF do relatório semestral segue como modelo o documento de referência do cliente
  (`Arthur Formigari Caldas- Relatório 2-2025.pdf`), incluindo as seções de informações gerais,
  frequência e estudo, técnica, musicalidade, leitura e memorização, pontos de atenção, estratégias
  pedagógicas, acompanhamento familiar, planejamento do próximo semestre e parecer final; dados de
  identidade visual (logo, endereço, contato) no cabeçalho/rodapé do PDF são configuráveis conforme
  os dados cadastrados da escola, e não fixos como no modelo de referência.
- O PDF do relatório de aula individual pode adotar um layout mais simples e resumido do que o PDF do
  relatório semestral, desde que contenha todos os campos definidos em FR-006.
</content>
