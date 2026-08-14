package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Detecta se o professor ja tem um relatorio em aberto para o aluno, em QUALQUER canal (spec 002,
 * FR-022a).
 *
 * <p>Um relatorio iniciado pelo Telegram e deixado incompleto deve ser oferecido para retomada
 * quando o professor abre o portal web (e vice-versa), evitando dois relatorios paralelos para a
 * mesma aula. Por isso a consulta ignora {@code canalOrigem} — ele e apenas informado na resposta,
 * para a UI dizer de onde veio o rascunho.
 *
 * <p>Nao e uma entidade nova: reaproveita os status ja modelados ({@code RASCUNHO}/{@code
 * PENDENTE_REVISAO}); relatorios aprovados ou cancelados estao encerrados e nunca sao oferecidos.
 * Quando existem pendencias dos dois tipos, a de aula tem precedencia por ser o fluxo mais comum e
 * o mais provavel de ter sido interrompido no meio.
 */
@Service
public class DetectarRascunhoPendenteUseCase {

  private static final List<StatusRelatorioAula> STATUS_ABERTOS_AULA =
      List.of(StatusRelatorioAula.RASCUNHO, StatusRelatorioAula.PENDENTE_REVISAO);

  private static final List<StatusRelatorioSemestral> STATUS_ABERTOS_SEMESTRAL =
      List.of(StatusRelatorioSemestral.PENDENTE_REVISAO);

  private final RelatorioAulaRepository relatorioAulaRepository;
  private final RelatorioSemestralRepository relatorioSemestralRepository;

  public DetectarRascunhoPendenteUseCase(
      RelatorioAulaRepository relatorioAulaRepository,
      RelatorioSemestralRepository relatorioSemestralRepository) {
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.relatorioSemestralRepository = relatorioSemestralRepository;
  }

  public RascunhoPendenteResponseDto detectar(UUID professorId, UUID alunoId) {
    Optional<RelatorioAula> pendenteAula =
        relatorioAulaRepository.findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
            professorId, alunoId, STATUS_ABERTOS_AULA);
    if (pendenteAula.isPresent()) {
      RelatorioAula relatorio = pendenteAula.get();
      return new RascunhoPendenteResponseDto(
          true,
          "AULA",
          relatorio.getId(),
          relatorio.getStatus().name(),
          relatorio.getCanalOrigem().name(),
          relatorio.getAtualizadoEm());
    }

    Optional<RelatorioSemestral> pendenteSemestral =
        relatorioSemestralRepository
            .findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
                professorId, alunoId, STATUS_ABERTOS_SEMESTRAL);
    if (pendenteSemestral.isPresent()) {
      RelatorioSemestral relatorio = pendenteSemestral.get();
      return new RascunhoPendenteResponseDto(
          true,
          "SEMESTRAL",
          relatorio.getId(),
          relatorio.getStatus().name(),
          relatorio.getCanalOrigem().name(),
          relatorio.getAtualizadoEm());
    }

    return RascunhoPendenteResponseDto.vazio();
  }
}
