package com.example.demo.controller;

import com.example.demo.dto.SessionCompleteRequest;
import com.example.demo.dto.SessionCreateRequest;
import com.example.demo.dto.SessionResponse;
import com.example.demo.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/api/sessions")
    public ResponseEntity<SessionResponse> createSession(
            @RequestHeader("X-User-Id") Long actorUserId,
            @Valid @RequestBody SessionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(actorUserId, request));
    }

    @PatchMapping("/api/sessions/{id}/check-in")
    public ResponseEntity<SessionResponse> checkIn(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(sessionService.checkIn(id, actorUserId));
    }

    @PatchMapping("/api/sessions/{id}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(sessionService.cancelSession(id, actorUserId));
    }

    @PatchMapping("/api/sessions/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long actorUserId,
            @Valid @RequestBody(required = false) SessionCompleteRequest request) {
        return ResponseEntity.ok(sessionService.completeSession(id, actorUserId, request));
    }

    @GetMapping("/api/sessions/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @GetMapping("/api/users/{userId}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessionsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(sessionService.getSessionsByUser(userId));
    }

    @GetMapping("/api/admin/sessions")
    public ResponseEntity<List<SessionResponse>> getAllSessionsForAdmin(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(sessionService.getAllSessionsForAdmin(status));
    }
}