package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.ProfessorSessaoResponseDto;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import com.escolademusica.relatorios.usecase.DetectarRascunhoPendenteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessao, selecao de aluno e historico no portal web do professor (spec 002, T015/T026-T028).
 *
 * <p>Autenticados pela cadeia {@code /api/v1/professor/**} de {@code SecurityConfig} — o principal
 * injetado por {@link AuthenticationPrincipal} e o {@code professorId} extraido do JWT por {@code
 * ProfessorJwtAuthenticationFilter}. O {@code professorId} NUNCA vem da URL ou do corpo: usar o
 * valor do token e o que impede um professor de consultar os dados de outro.
 */
@RestController
@RequestMapping("/api/v1/professor")
@Tag(
    name = "Portal do professor - Sessao e alunos",
    description = "Dados do professor autenticado, seus alunos e o historico deles")
public class ProfessorPortalController {

  private final ProfessorRepository professorRepository;
  private final ProfessorAlunoRepository professorAlunoRepository;
  private final AlunoRepository alunoRepository;
  private final JwtService jwtService;
  private final ControleAcessoProfessorService controleAcesso;
  private final DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase;
  private final ConsultarHistoricoUseCase consultarHistoricoUseCase;

  public ProfessorPortalController(
      ProfessorRepository professorRepository,
      ProfessorAlunoRepository professorAlunoRepository,
      AlunoRepository alunoRepository,
      JwtService jwtService,
      ControleAcessoProfessorService controleAcesso,
      DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase,
      ConsultarHistoricoUseCase consultarHistoricoUseCase) {
    this.professorRepository = professorRepository;
    this.professorAlunoRepository = professorAlunoRepository;
    this.alunoRepository = alunoRepository;
    this.jwtService = jwtService;
    this.controleAcesso = controleAcesso;
    this.detectarRascunhoPendenteUseCase = detectarRascunhoPendenteUseCase;
    this.consultarHistoricoUseCase = consultarHistoricoUseCase;
  }

  /** {@code GET /api/v1/professor/me} */
  @Operation(summary = "Dados do professor autenticado e expiracao da sessao vigente (FR-005)")
  @GetMapping("/me")
  public ProfessorSessaoResponseDto me(@AuthenticationPrincipal UUID professorId) {
    Professor professor =
        professorRepository
            .findById(professorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado"));

    // O filtro acabou de reemitir o token da sessao deslizante; a expiracao informada aqui e a
    // desse token novo, nao a do token que veio na requisicao.
    OffsetDateTime expiraEm = OffsetDateTime.now().plus(jwtService.getExpiracaoProfessor());
    return new ProfessorSessaoResponseDto(professor.getId(), professor.getNome(), expiraEm);
  }

  /** {@code GET /api/v1/professor/alunos} */
  @Operation(summary = "Lista os alunos associados ao professor autenticado")
  @GetMapping("/alunos")
  public List<AlunoResumoDto> listarAlunos(@AuthenticationPrincipal UUID professorId) {
    return professorAlunoRepository.findByProfessorId(professorId).stream()
        .map(ProfessorAluno::getAlunoId)
        .map(alunoRepository::findById)
        .flatMap(Optional::stream)
        .map(aluno -> new AlunoResumoDto(aluno.getId(), aluno.getNome()))
        .toList();
  }

  /** {@code GET /api/v1/professor/alunos/{alunoId}/rascunho-pendente} */
  @Operation(summary = "Relatorio em aberto do aluno em qualquer canal, para retomada (FR-022a)")
  @GetMapping("/alunos/{alunoId}/rascunho-pendente")
  public RascunhoPendenteResponseDto rascunhoPendente(
      @AuthenticationPrincipal UUID professorId, @PathVariable UUID alunoId) {
    controleAcesso.exigirAlunoDoProfessor(professorId, alunoId);
    return detectarRascunhoPendenteUseCase.detectar(professorId, alunoId);
  }

  /** {@code GET /api/v1/professor/alunos/{alunoId}/historico} */
  @Operation(
      summary =
          "Historico unificado do aluno: relatorios de aula e semestrais, qualquer canal (FR-023)")
  @GetMapping("/alunos/{alunoId}/historico")
  public HistoricoAlunoResponseDto historico(
      @AuthenticationPrincipal UUID professorId, @PathVariable UUID alunoId) {
    // A sobrecarga com professorId aplica o mesmo controle de acesso ja usado pelo canal Telegram.
    return consultarHistoricoUseCase.executar(alunoId, professorId);
  }
}
