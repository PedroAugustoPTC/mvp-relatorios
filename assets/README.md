# Assets do cliente

Material de referência/negócio compartilhado pelo cliente (escola de música) e usado pela equipe
neste projeto — por exemplo: logo e identidade visual, documentos de escopo, exemplos de
relatório, materiais de apresentação.

## O que NÃO deve ir aqui

Nenhum dado pessoal real de aluno, responsável ou professor (nome, CPF, data de nascimento,
planilhas de cadastro, etc.). O projeto lida com dados de menores de idade sob LGPD — dado pessoal
real não deve ser versionado no Git em nenhuma hipótese, nem aqui nem em outro lugar do
repositório. Se precisar compartilhar algo assim com o time, use um canal apropriado (não o Git).

**Exceção documentada**: as gravações de aula em `assets/` (`.ogg`/`.mp4`) são **fictícias**,
produzidas pelo próprio cliente especificamente como insumo de teste para a geração de relatórios
— não são gravações reais de aluno. Nomes usados nelas são fictícios/de teste.

## Convenção

- Organize por subpasta quando fizer sentido (ex.: `assets/identidade-visual/`,
  `assets/documentos/`).
- Prefira formatos abertos/leves quando possível (PNG/SVG para imagens, PDF para documentos).
- Arquivos muito grandes (vídeos, PDFs de dezenas de MB) podem inchar o histórico do Git — avise o
  time antes de adicionar algo assim.
