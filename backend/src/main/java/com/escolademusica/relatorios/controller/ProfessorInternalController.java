package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno (T048, FR-004) usado pelo n8n para montar o menu de selecao de aluno do bot do
 * Telegram: {@code GET /internal/v1/professores/{professorId}/alunos}.
 *
 * <p>Mantido em uma classe separada de {@link ProfessorController} (que expoe {@code
 * /api/v1/professores/**}, interface administrativa web com autenticacao JWT, T059) porque este
 * endpoint vive sob {@code /internal/v1/**} com autenticacao por {@code X-N8N-Service-Token}
 * (SecurityConfig) — bases de rota e cadeias de seguranca distintas, evitando qualquer conflito de
 * merge com o controller administrativo desenvolvido em paralelo (US2).
 */
@RestController
@Tag(
    name = "n8n - Selecao de aluno",
    description = "Endpoints internos usados pelo bot do Telegram (n8n)")
public class ProfessorInternalController {

  private final ProfessorAlunoRepository professorAlunoRepository;
  private final AlunoRepository alunoRepository;

  public ProfessorInternalController(
      ProfessorAlunoRepository professorAlunoRepository, AlunoRepository alunoRepository) {
    this.professorAlunoRepository = professorAlunoRepository;
    this.alunoRepository = alunoRepository;
  }

  @Operation(summary = "Lista os alunos associados ao professor, para o menu do bot")
  @GetMapping("/internal/v1/professores/{professorId}/alunos")
  public List<AlunoResumoDto> listarAlunos(@PathVariable UUID professorId) {
    List<ProfessorAluno> vinculos = professorAlunoRepository.findByProfessorId(professorId);
    return vinculos.stream()
        .map(ProfessorAluno::getAlunoId)
        .map(alunoRepository::findById)
        .flatMap(java.util.Optional::stream)
        .map(aluno -> new AlunoResumoDto(aluno.getId(), aluno.getNome()))
        .toList();
  }
}
