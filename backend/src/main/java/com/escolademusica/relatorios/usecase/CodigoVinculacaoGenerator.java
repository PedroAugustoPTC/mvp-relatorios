package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Gera codigos de vinculacao Telegram-Professor alfanumericos curtos e unicos, com expiracao
 * (research.md secao 7, FR-002). Compartilhado por {@link CadastrarProfessorUseCase} e {@link
 * ReemitirCodigoVinculacaoUseCase} para evitar duplicar a logica de geracao/checagem de unicidade.
 */
@Component
public class CodigoVinculacaoGenerator {

  /** Alfabeto sem caracteres visualmente ambiguos (0/O, 1/I, etc.). */
  private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private static final int TAMANHO_CODIGO = 8;
  private static final int MAX_TENTATIVAS = 10;

  /**
   * Validade do codigo apos a geracao/reemissao: 7 dias. Aumentado a partir das 24h originais
   * (research.md secao 7) para reduzir a friccao de professores que usam o portal web com pouca
   * frequencia — a validade tambem e renovada a cada login bem-sucedido no portal (ver {@link
   * AutenticarProfessorPortalUseCase}), entao o uso continuo nunca deixa o codigo expirar.
   */
  public static final Duration VALIDADE = Duration.ofDays(7);

  private final ProfessorRepository professorRepository;
  private final SecureRandom random = new SecureRandom();

  public CodigoVinculacaoGenerator(ProfessorRepository professorRepository) {
    this.professorRepository = professorRepository;
  }

  /** Gera um codigo garantidamente unico entre os professores cadastrados. */
  public String gerarCodigoUnico() {
    for (int tentativa = 0; tentativa < MAX_TENTATIVAS; tentativa++) {
      String candidato = gerarCandidato();
      if (professorRepository.findByCodigoVinculacao(candidato).isEmpty()) {
        return candidato;
      }
    }
    throw new IllegalStateException(
        "Nao foi possivel gerar um codigo de vinculacao unico apos "
            + MAX_TENTATIVAS
            + " tentativas");
  }

  private String gerarCandidato() {
    StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
    for (int i = 0; i < TAMANHO_CODIGO; i++) {
      sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
    }
    return sb.toString();
  }
}
