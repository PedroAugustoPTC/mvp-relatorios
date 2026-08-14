package com.escolademusica.relatorios.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados de formulario (nao-binarios) de {@code POST /internal/v1/relatorios-aula/transcrever}
 * (T047/T049); o arquivo de audio e tratado separadamente como {@code MultipartFile} no controller.
 */
public record TranscreverRequestDto(UUID alunoId, UUID professorId, LocalDate dataAula) {}
