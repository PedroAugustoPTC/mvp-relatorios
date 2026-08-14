package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela um relatorio semestral a pedido explicito do professor (spec 002), equivalente semestral
 * de {@link CancelarRelatorioAulaUseCase}: status terminal {@code CANCELADO} em vez de remocao, e
 * recusa (409) se o relatorio ja foi aprovado.
 */
@Service
public class CancelarRelatorioSemestralUseCase {

  private final RelatorioSemestralRepository relatorioSemestralRepository;

  public CancelarRelatorioSemestralUseCase(
      RelatorioSemestralRepository relatorioSemestralRepository) {
    this.relatorioSemestralRepository = relatorioSemestralRepository;
  }

  @Transactional
  public void cancelar(UUID relatorioId) {
    RelatorioSemestral relatorio =
        relatorioSemestralRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio semestral nao encontrado: " + relatorioId));

    relatorio.cancelar();
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    relatorioSemestralRepository.save(relatorio);
  }
}
