package com.escolademusica.relatorios.dto;

/**
 * Corpo de erro padrao retornado pela API em caso de falha ({@code GlobalExceptionHandler}).
 *
 * @param codigo codigo curto e estavel identificando o tipo de erro (ex.: "RECURSO_NAO_ENCONTRADO")
 * @param mensagem mensagem legivel para o consumidor da API; nunca inclui stack trace
 */
public record ErroDto(String codigo, String mensagem) {}
