package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Administrador;
import com.escolademusica.relatorios.domain.exception.CredenciaisInvalidasException;
import com.escolademusica.relatorios.repository.AdministradorRepository;
import com.escolademusica.relatorios.service.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AutenticarAdminUseCaseTest {

  @Mock private AdministradorRepository administradorRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  private AutenticarAdminUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new AutenticarAdminUseCase(administradorRepository, passwordEncoder, jwtService);
  }

  private Administrador administrador() {
    Administrador administrador = new Administrador();
    administrador.setEmail("admin@escola.com");
    administrador.setSenhaHash("hash-bcrypt");
    return administrador;
  }

  @Test
  void deveAutenticarERetornarTokenQuandoCredenciaisValidas() {
    Administrador administrador = administrador();
    when(administradorRepository.findByEmail("admin@escola.com"))
        .thenReturn(Optional.of(administrador));
    when(passwordEncoder.matches("senha123", "hash-bcrypt")).thenReturn(true);
    JwtService.TokenGerado tokenGerado =
        new JwtService.TokenGerado("token-jwt", java.time.OffsetDateTime.now());
    when(jwtService.gerarToken("admin@escola.com")).thenReturn(tokenGerado);

    JwtService.TokenGerado resultado = useCase.executar("admin@escola.com", "senha123");

    assertThat(resultado).isEqualTo(tokenGerado);
  }

  @Test
  void deveLancarQuandoEmailNaoEncontrado() {
    when(administradorRepository.findByEmail("desconhecido@escola.com"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar("desconhecido@escola.com", "senha123"))
        .isInstanceOf(CredenciaisInvalidasException.class);
  }

  @Test
  void deveLancarQuandoSenhaIncorreta() {
    Administrador administrador = administrador();
    when(administradorRepository.findByEmail("admin@escola.com"))
        .thenReturn(Optional.of(administrador));
    when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

    assertThatThrownBy(() -> useCase.executar("admin@escola.com", "senha-errada"))
        .isInstanceOf(CredenciaisInvalidasException.class);
  }
}
