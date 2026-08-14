package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.dto.AlunoResponseDto;
import com.escolademusica.relatorios.dto.CadastrarAlunoRequestDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.mapper.AlunoMapper;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.usecase.CadastrarAlunoUseCase;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de alunos da interface web administrativa ({@code /api/v1/alunos}, T060).
 *
 * <p>{@code GET /api/v1/alunos/{id}/historico} (US4/T086) nao aplica escopo por professor: a
 * autenticacao JWT de administrador ja garante acesso irrestrito ao historico de qualquer aluno
 * (FR-017a).
 */
@RestController
@RequestMapping("/api/v1/alunos")
@Tag(name = "Alunos", description = "Cadastro e historico de alunos (interface administrativa)")
public class AlunoController {

  private final CadastrarAlunoUseCase cadastrarAlunoUseCase;
  private final AlunoRepository alunoRepository;
  private final ProfessorAlunoRepository professorAlunoRepository;
  private final ConsultarHistoricoUseCase consultarHistoricoUseCase;

  public AlunoController(
      CadastrarAlunoUseCase cadastrarAlunoUseCase,
      AlunoRepository alunoRepository,
      ProfessorAlunoRepository professorAlunoRepository,
      ConsultarHistoricoUseCase consultarHistoricoUseCase) {
    this.cadastrarAlunoUseCase = cadastrarAlunoUseCase;
    this.alunoRepository = alunoRepository;
    this.professorAlunoRepository = professorAlunoRepository;
    this.consultarHistoricoUseCase = consultarHistoricoUseCase;
  }

  @Operation(summary = "Cadastra um aluno, associando um ou mais professores")
  @PostMapping
  public ResponseEntity<AlunoResponseDto> cadastrar(
      @Valid @RequestBody CadastrarAlunoRequestDto requestDto) {
    Aluno aluno =
        cadastrarAlunoUseCase.executar(
            requestDto.nome(),
            requestDto.dataNascimento(),
            requestDto.cpf(),
            requestDto.nomeResponsavel(),
            requestDto.professorIds());
    return ResponseEntity.status(HttpStatus.CREATED).body(AlunoMapper.paraResponseDto(aluno));
  }

  @Operation(summary = "Lista alunos, com filtro opcional por professor")
  @GetMapping
  public ResponseEntity<List<AlunoResponseDto>> listar(
      @RequestParam(required = false) UUID professorId) {
    List<Aluno> alunos;
    if (professorId != null) {
      List<UUID> alunoIds =
          professorAlunoRepository.findByProfessorId(professorId).stream()
              .map(ProfessorAluno::getAlunoId)
              .collect(Collectors.toList());
      alunos = alunoRepository.findAllById(alunoIds);
    } else {
      alunos = alunoRepository.findAll();
    }
    return ResponseEntity.ok(alunos.stream().map(AlunoMapper::paraResponseDto).toList());
  }

  @Operation(summary = "Historico completo (aula + semestral aprovados) de um aluno")
  @GetMapping("/{id}/historico")
  public ResponseEntity<HistoricoAlunoResponseDto> historico(@PathVariable UUID id) {
    return ResponseEntity.ok(consultarHistoricoUseCase.executar(id));
  }
}
