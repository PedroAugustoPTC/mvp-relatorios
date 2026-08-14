package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela um relatorio de aula a pedido explicito do professor (spec 002): ele decidiu nao seguir
 * com o registro daquela aula.
 *
 * <p>O relatorio recebe o status terminal {@code CANCELADO} em vez de ser removido — preserva
 * rastreabilidade e faz o rascunho deixar de ser oferecido por {@link
 * DetectarRascunhoPendenteUseCase}. Cancelar um relatorio ja aprovado e recusado pelo dominio (409
 * via {@link IllegalStateException}): um documento oficial ja entregue nao volta atras por aqui.
 */
@Service
public class CancelarRelatorioAulaUseCase {

  private final RelatorioAulaRepository relatorioAulaRepository;

  public CancelarRelatorioAulaUseCase(RelatorioAulaRepository relatorioAulaRepository) {
    this.relatorioAulaRepository = relatorioAulaRepository;
  }

  @Transactional
  public void cancelar(UUID relatorioId) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado: " + relatorioId));

    relatorio.cancelar();
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    relatorioAulaRepository.save(relatorio);
  }
}
