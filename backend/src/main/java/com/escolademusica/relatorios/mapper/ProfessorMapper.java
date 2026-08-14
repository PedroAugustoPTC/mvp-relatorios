package com.escolademusica.relatorios.mapper;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.dto.ProfessorListaResponseDto;
import com.escolademusica.relatorios.dto.ProfessorResponseDto;

/** Conversao entre {@link Professor} e os DTOs expostos por ProfessorController. */
public final class ProfessorMapper {

  private ProfessorMapper() {}

  public static ProfessorResponseDto paraResponseDto(Professor professor) {
    return new ProfessorResponseDto(
        professor.getId(),
        professor.getNome(),
        professor.getEmail(),
        professor.getCodigoVinculacao(),
        professor.getCodigoVinculacaoExpiraEm());
  }

  public static ProfessorListaResponseDto paraListaResponseDto(
      Professor professor, boolean vinculadoTelegram) {
    return new ProfessorListaResponseDto(
        professor.getId(), professor.getNome(), professor.getEmail(), vinculadoTelegram);
  }
}
