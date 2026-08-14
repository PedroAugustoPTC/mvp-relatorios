package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Administrador;
import com.escolademusica.relatorios.domain.exception.CredenciaisInvalidasException;
import com.escolademusica.relatorios.repository.AdministradorRepository;
import com.escolademusica.relatorios.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autentica o administrador da interface web (e-mail/senha com BCrypt) e emite um JWT de curta
 * duracao em caso de sucesso (research.md secao 6, T058).
 */
@Service
public class AutenticarAdminUseCase {

  private final AdministradorRepository administradorRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AutenticarAdminUseCase(
      AdministradorRepository administradorRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.administradorRepository = administradorRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional(readOnly = true)
  public JwtService.TokenGerado executar(String email, String senha) {
    Administrador administrador =
        administradorRepository
            .findByEmail(email)
            .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha invalidos."));

    if (!passwordEncoder.matches(senha, administrador.getSenhaHash())) {
      throw new CredenciaisInvalidasException("E-mail ou senha invalidos.");
    }

    return jwtService.gerarToken(administrador.getEmail());
  }
}
