package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.TranscricaoService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra uma nova aula a partir de um audio (T041): valida o vinculo professor-aluno (FR-018),
 * transcreve o audio e persiste {@link Aula} + o {@link RelatorioAula} inicial (status RASCUNHO)
 * somente apos a transcricao ter sido concluida com sucesso (T050 — nenhum registro e criado se o
 * audio nao puder ser processado).
 */
@Service
public class RegistrarAulaUseCase {

  private final ProfessorAlunoRepository professorAlunoRepository;
  private final AulaRepository aulaRepository;
  private final RelatorioAulaRepository relatorioAulaRepository;
  private final TranscricaoService transcricaoService;

  public RegistrarAulaUseCase(
      ProfessorAlunoRepository professorAlunoRepository,
      AulaRepository aulaRepository,
      RelatorioAulaRepository relatorioAulaRepository,
      TranscricaoService transcricaoService) {
    this.professorAlunoRepository = professorAlunoRepository;
    this.aulaRepository = aulaRepository;
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.transcricaoService = transcricaoService;
  }

  /**
   * @throws AlunoNaoAssociadoException se o aluno nao estiver vinculado ao professor (FR-018)
   * @throws com.escolademusica.relatorios.gateway.AudioNaoProcessavelException se o audio nao puder
   *     ser transcrito — nesse caso nenhuma Aula/RelatorioAula e persistida (T050)
   */
  @Transactional
  public Resultado registrar(
      UUID professorId, UUID alunoId, LocalDate dataAula, byte[] audioBytes, String formato) {
    if (!professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId)) {
      throw new AlunoNaoAssociadoException(
          "Aluno %s nao esta associado ao professor %s".formatted(alunoId, professorId));
    }

    // A transcricao acontece antes de qualquer persistencia: se o SpeechToTextGateway lancar
    // AudioNaoProcessavelException, nem Aula nem RelatorioAula chegam a ser criados (T050).
    String transcricao = transcricaoService.transcrever(audioBytes, formato);

    OffsetDateTime agora = OffsetDateTime.now();

    Aula aula = new Aula();
    aula.setProfessorId(professorId);
    aula.setAlunoId(alunoId);
    aula.setDataAula(dataAula);
    aula.setCriadoEm(agora);
    aula = aulaRepository.save(aula);

    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setAulaId(aula.getId());
    relatorio.setProfessorId(professorId);
    relatorio.setAlunoId(alunoId);
    relatorio.setTranscricao(transcricao);
    relatorio.setVersao(1);
    relatorio.setCriadoEm(agora);
    relatorio.setAtualizadoEm(agora);
    relatorioAulaRepository.save(relatorio);

    return new Resultado(aula.getId(), transcricao);
  }

  /** Resultado de {@link #registrar}, base da resposta de {@code POST .../transcrever}. */
  public record Resultado(UUID aulaId, String transcricao) {}
}
