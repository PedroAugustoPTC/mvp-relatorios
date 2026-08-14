package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma a aprovacao explicita do professor sobre a versao vigente do relatorio de aula (T045,
 * FR-008a/FR-010). So transiciona para APROVADO se a versao confirmada pelo professor corresponder
 * exatamente a versao atual do relatorio — protege contra aprovar um PDF desatualizado quando uma
 * nova revisao foi gerada nesse meio tempo, respondendo com 409 (via {@link IllegalStateException},
 * mapeado pelo GlobalExceptionHandler) tanto para PDF desatualizado quanto para relatorio ja
 * aprovado anteriormente.
 */
@Service
public class AprovarRelatorioAulaUseCase {

  private final RelatorioAulaRepository relatorioAulaRepository;

  public AprovarRelatorioAulaUseCase(RelatorioAulaRepository relatorioAulaRepository) {
    this.relatorioAulaRepository = relatorioAulaRepository;
  }

  @Transactional
  public AprovarRelatorioResponseDto aprovar(UUID relatorioId, int versaoConfirmada) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado: " + relatorioId));

    if (relatorio.getVersao() != versaoConfirmada) {
      String mensagem =
          ("Versao confirmada (%d) nao corresponde a versao vigente (%d) do relatorio; o PDF"
                  + " pode estar desatualizado")
              .formatted(versaoConfirmada, relatorio.getVersao());
      throw new IllegalStateException(mensagem);
    }

    // RelatorioAula.aprovar ja lanca IllegalStateException (-> 409) se o status atual nao for
    // PENDENTE_REVISAO, cobrindo tambem o caso de relatorio ja aprovado anteriormente.
    relatorio.aprovar(OffsetDateTime.now());
    relatorio.setAtualizadoEm(relatorio.getAprovadoEm());

    relatorioAulaRepository.save(relatorio);
    return new AprovarRelatorioResponseDto(
        relatorio.getId(), relatorio.getStatus().name(), relatorio.getAprovadoEm());
  }
}
