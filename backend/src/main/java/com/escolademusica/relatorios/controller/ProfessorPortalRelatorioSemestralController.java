package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.CanalOrigem;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.dto.GerarRelatorioSemestralPortalRequestDto;
import com.escolademusica.relatorios.dto.GerarRelatorioSemestralPortalResponseDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.dto.RevisarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
import com.escolademusica.relatorios.usecase.AprovarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.CancelarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ContarRelatoriosPeriodoUseCase;
import com.escolademusica.relatorios.usecase.GerarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ListarSemestresDisponiveisUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioSemestralUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fluxo de relatorio semestral pelo portal web (spec 002, US3): escolher o semestre -> consolidar
 * -> conferir o PDF -> aprovar, revisar ou cancelar.
 *
 * <p>Como o controller de relatorio de aula, este e apenas um adaptador de entrada do canal web
 * sobre os UseCases ja usados pelo Telegram; a unica especificidade do canal e gravar {@code
 * canalOrigem=WEB}.
 */
@RestController
@RequestMapping("/api/v1/professor")
@Tag(
    name = "Portal do professor - Relatorio semestral",
    description = "Consolidacao semestral, revisao e aprovacao pelo portal web")
public class ProfessorPortalRelatorioSemestralController {

  private final ControleAcessoProfessorService controleAcesso;
  private final ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase;
  private final ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  private final GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase;
  private final RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase;
  private final AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase;
  private final CancelarRelatorioSemestralUseCase cancelarRelatorioSemestralUseCase;
  private final RelatorioSemestralRepository relatorioSemestralRepository;
  private final RelatorioSemestralMapper mapper;

  public ProfessorPortalRelatorioSemestralController(
      ControleAcessoProfessorService controleAcesso,
      ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase,
      ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase,
      GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase,
      RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase,
      AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase,
      CancelarRelatorioSemestralUseCase cancelarRelatorioSemestralUseCase,
      RelatorioSemestralRepository relatorioSemestralRepository,
      RelatorioSemestralMapper mapper) {
    this.controleAcesso = controleAcesso;
    this.listarSemestresDisponiveisUseCase = listarSemestresDisponiveisUseCase;
    this.contarRelatoriosPeriodoUseCase = contarRelatoriosPeriodoUseCase;
    this.gerarRelatorioSemestralUseCase = gerarRelatorioSemestralUseCase;
    this.revisarRelatorioSemestralUseCase = revisarRelatorioSemestralUseCase;
    this.aprovarRelatorioSemestralUseCase = aprovarRelatorioSemestralUseCase;
    this.cancelarRelatorioSemestralUseCase = cancelarRelatorioSemestralUseCase;
    this.relatorioSemestralRepository = relatorioSemestralRepository;
    this.mapper = mapper;
  }

  /** {@code GET /api/v1/professor/alunos/{alunoId}/semestres-disponiveis} */
  @Operation(summary = "Menu de semestres selecionaveis para o aluno (FR-015)")
  @GetMapping("/alunos/{alunoId}/semestres-disponiveis")
  public List<SemestreDisponivelDto> semestresDisponiveis(
      @AuthenticationPrincipal UUID professorId, @PathVariable UUID alunoId) {
    controleAcesso.exigirAlunoDoProfessor(professorId, alunoId);
    return listarSemestresDisponiveisUseCase.listar(alunoId);
  }

  /**
   * {@code POST /api/v1/professor/alunos/{alunoId}/relatorios-semestrais}
   *
   * <p>A contagem e feita antes da consolidacao: sem nenhum relatorio de aula aprovado no periodo,
   * a resposta e 409 {@code SEM_DADOS_NO_PERIODO} e nada e criado — nem relatorio, nem PDF, nem
   * chamada ao LLM (FR-016/FR-019).
   */
  @Operation(summary = "Consolida o relatorio semestral do periodo escolhido (FR-016 a FR-019)")
  @PostMapping("/alunos/{alunoId}/relatorios-semestrais")
  public GerarRelatorioSemestralPortalResponseDto gerar(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID alunoId,
      @Valid @RequestBody GerarRelatorioSemestralPortalRequestDto corpo) {
    controleAcesso.exigirAlunoDoProfessor(professorId, alunoId);

    ContagemRelatoriosResponseDto contagem =
        contarRelatoriosPeriodoUseCase.contar(alunoId, corpo.semestre());
    if (contagem.quantidadeRelatoriosAula() == 0) {
      throw new ConflitoException(
          "SEM_DADOS_NO_PERIODO",
          "Nenhum relatório de aula aprovado foi encontrado no período selecionado.");
    }

    RelatorioSemestralResponseDto gerado =
        gerarRelatorioSemestralUseCase.gerar(alunoId, professorId, corpo.semestre());
    marcarCanalWeb(gerado.relatorioId());

    return new GerarRelatorioSemestralPortalResponseDto(
        contagem.quantidadeRelatoriosAula(),
        gerado.relatorioId(),
        gerado.status(),
        gerado.pdfUrl(),
        gerado.versao());
  }

  /** {@code GET /api/v1/professor/relatorios-semestrais/{relatorioId}} */
  @Operation(summary = "Estado atual do relatorio semestral, com o PDF de conferencia")
  @GetMapping("/relatorios-semestrais/{relatorioId}")
  public RelatorioSemestralResponseDto consultar(
      @AuthenticationPrincipal UUID professorId, @PathVariable UUID relatorioId) {
    RelatorioSemestral relatorio =
        controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId);
    return mapper.paraResponseDto(relatorio);
  }

  /** {@code POST /api/v1/professor/relatorios-semestrais/{relatorioId}/revisar} */
  @Operation(summary = "Solicita alteracao em linguagem natural, gerando nova versao (FR-020)")
  @PostMapping("/relatorios-semestrais/{relatorioId}/revisar")
  public RelatorioSemestralResponseDto revisar(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID relatorioId,
      @Valid @RequestBody RevisarRelatorioRequestDto corpo) {
    controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId);
    return revisarRelatorioSemestralUseCase.revisar(relatorioId, corpo.instrucao());
  }

  /** {@code POST /api/v1/professor/relatorios-semestrais/{relatorioId}/aprovar} */
  @Operation(summary = "Aprova explicitamente a versao vigente do relatorio semestral (FR-020)")
  @PostMapping("/relatorios-semestrais/{relatorioId}/aprovar")
  public AprovarRelatorioResponseDto aprovar(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID relatorioId,
      @Valid @RequestBody AprovarRelatorioRequestDto corpo) {
    controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId);
    return aprovarRelatorioSemestralUseCase.aprovar(relatorioId, corpo.versaoConfirmada());
  }

  /** {@code POST /api/v1/professor/relatorios-semestrais/{relatorioId}/cancelar} */
  @Operation(summary = "Cancela o relatorio semestral quando o professor decide nao seguir")
  @PostMapping("/relatorios-semestrais/{relatorioId}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelar(@AuthenticationPrincipal UUID professorId, @PathVariable UUID relatorioId) {
    controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId);
    cancelarRelatorioSemestralUseCase.cancelar(relatorioId);
  }

  /** Marca a origem web no relatorio recem-consolidado, sem tocar no UseCase (FR-002). */
  private void marcarCanalWeb(UUID relatorioId) {
    relatorioSemestralRepository
        .findById(relatorioId)
        .ifPresent(
            relatorio -> {
              relatorio.setCanalOrigem(CanalOrigem.WEB);
              relatorio.setAtualizadoEm(OffsetDateTime.now());
              relatorioSemestralRepository.save(relatorio);
            });
  }
}
