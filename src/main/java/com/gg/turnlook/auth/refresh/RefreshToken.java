package com.gg.turnlook.auth.refresh;
import com.gg.turnlook.features.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;
    
}
