package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.dto.CadastrarProfessorRequestDto;
import com.escolademusica.relatorios.dto.ProfessorListaResponseDto;
import com.escolademusica.relatorios.dto.ProfessorResponseDto;
import com.escolademusica.relatorios.mapper.ProfessorMapper;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.usecase.CadastrarProfessorUseCase;
import com.escolademusica.relatorios.usecase.ReemitirCodigoVinculacaoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de professores da interface web administrativa ({@code /api/v1/professores/**}, T059).
 *
 * <p><b>Nota:</b> o endpoint interno {@code GET /internal/v1/professores/{professorId}/alunos}
 * (lista de selecao do bot, FR-004, T048) e responsabilidade de US1 e pode ser adicionado a esta
 * mesma classe por um agente concorrente, mantendo os dois grupos de rotas (autenticacao distinta:
 * JWT de administrador em {@code /api/v1/**} vs. {@code X-N8N-Service-Token} em {@code
 * /internal/v1/**}) sem conflito.
 */
@RestController
@RequestMapping("/api/v1/professores")
@Tag(
    name = "Professores",
    description = "Cadastro e consulta de professores (interface administrativa)")
public class ProfessorController {

  private final CadastrarProfessorUseCase cadastrarProfessorUseCase;
  private final ReemitirCodigoVinculacaoUseCase reemitirCodigoVinculacaoUseCase;
  private final ProfessorRepository professorRepository;
  private final VinculoTelegramRepository vinculoTelegramRepository;

  public ProfessorController(
      CadastrarProfessorUseCase cadastrarProfessorUseCase,
      ReemitirCodigoVinculacaoUseCase reemitirCodigoVinculacaoUseCase,
      ProfessorRepository professorRepository,
      VinculoTelegramRepository vinculoTelegramRepository) {
    this.cadastrarProfessorUseCase = cadastrarProfessorUseCase;
    this.reemitirCodigoVinculacaoUseCase = reemitirCodigoVinculacaoUseCase;
    this.professorRepository = professorRepository;
    this.vinculoTelegramRepository = vinculoTelegramRepository;
  }

  @Operation(summary = "Cadastra um professor e gera o codigo de vinculacao do Telegram")
  @PostMapping
  public ResponseEntity<ProfessorResponseDto> cadastrar(
      @Valid @RequestBody CadastrarProfessorRequestDto requestDto) {
    Professor professor = cadastrarProfessorUseCase.executar(requestDto.nome(), requestDto.email());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ProfessorMapper.paraResponseDto(professor));
  }

  @Operation(summary = "Lista os professores cadastrados, indicando se ja vincularam o Telegram")
  @GetMapping
  public ResponseEntity<List<ProfessorListaResponseDto>> listar() {
    List<ProfessorListaResponseDto> resposta =
        professorRepository.findAll().stream()
            .map(
                professor -> {
                  boolean vinculado =
                      vinculoTelegramRepository.findByProfessorId(professor.getId()).isPresent();
                  return ProfessorMapper.paraListaResponseDto(professor, vinculado);
                })
            .toList();
    return ResponseEntity.ok(resposta);
  }

  @Operation(summary = "Reemite o codigo de vinculacao do Telegram de um professor")
  @PostMapping("/{id}/codigo-vinculacao")
  public ResponseEntity<ProfessorResponseDto> reemitirCodigoVinculacao(@PathVariable UUID id) {
    Professor professor = reemitirCodigoVinculacaoUseCase.executar(id);
    return ResponseEntity.ok(ProfessorMapper.paraResponseDto(professor));
  }
}
