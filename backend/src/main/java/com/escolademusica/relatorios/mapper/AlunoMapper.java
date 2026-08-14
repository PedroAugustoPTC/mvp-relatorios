package com.escolademusica.relatorios.mapper;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.dto.AlunoResponseDto;

/** Conversao entre {@link Aluno} e os DTOs expostos por AlunoController. Nunca expoe o CPF. */
public final class AlunoMapper {

  private AlunoMapper() {}

  public static AlunoResponseDto paraResponseDto(Aluno aluno) {
    return new AlunoResponseDto(
        aluno.getId(),
        aluno.getNome(),
        aluno.getDataNascimento(),
        aluno.getNomeResponsavel(),
        aluno.isMenorDeIdade());
  }
}
