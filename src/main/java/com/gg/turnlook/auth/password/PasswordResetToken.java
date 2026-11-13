package com.gg.turnlook.auth.password;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;
    @Column(name = "fecha_uso") private Instant fechaUso;
}
