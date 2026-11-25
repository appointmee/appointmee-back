package com.gg.turnlook.features.chat;

import com.gg.turnlook.features.chat.dto.ConversacionCrearDTO;
import com.gg.turnlook.features.chat.dto.ConversacionDetalleDTO;
import com.gg.turnlook.features.chat.dto.ConversacionResumenDTO;
import com.gg.turnlook.features.chat.dto.MensajeCrearDTO;
import com.gg.turnlook.features.chat.dto.MensajeResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/conversaciones")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /// ENDPOINTS

    @PostMapping
    public ResponseEntity<ConversacionDetalleDTO> crear(
            @Valid @RequestBody ConversacionCrearDTO dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.crear(dto, email));
    }

    @GetMapping
    public ResponseEntity<List<ConversacionResumenDTO>> listar(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.listar(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversacionDetalleDTO> obtener(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.obtener(id, email));
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<MensajeResponseDTO>> listarMensajes(
            @PathVariable("id") Integer id,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "size", defaultValue = "30") int size,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.listarMensajes(id, beforeId, size, email));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<MensajeResponseDTO> enviar(
            @PathVariable("id") Integer id,
            @Valid @RequestBody MensajeCrearDTO dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.enviar(id, dto, email));
    }

    @PatchMapping("/{id}/leido")
    public ResponseEntity<?> marcarLeido(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal String email) {
        chatService.marcarLeido(id, email);
        return ResponseEntity.ok().body("Conversación marcada como leída");
    }
}
