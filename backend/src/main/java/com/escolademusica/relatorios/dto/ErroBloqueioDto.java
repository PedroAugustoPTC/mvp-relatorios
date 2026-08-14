package com.escolademusica.relatorios.dto;

/**
 * Corpo de erro do bloqueio temporario de autenticacao (HTTP 429, spec 002 FR-004a). Estende o
 * formato padrao {@link ErroDto} com o tempo de espera, para o portal exibir "tente novamente em X
 * minutos" em vez de uma mensagem generica.
 */
public record ErroBloqueioDto(String codigo, String mensagem, long retryAfterSeconds) {}
