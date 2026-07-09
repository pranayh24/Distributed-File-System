package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.model.DfsEvent;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.service.EventService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Live Events", description = "Real-time stream of distributed-system activity")
public class EventController {

    private final EventService eventService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live event stream (SSE)",
            description = "Server-Sent Events of uploads, replication, and self-healing as they happen. "
                    + "Browsers authenticate with a ?token=<jwt> query param since EventSource cannot send headers.")
    public SseEmitter stream() {
        User user = UserContext.getCurrentUser();
        return eventService.subscribe(user != null ? user.getUserId() : null);
    }

    @GetMapping("/recent")
    @Operation(summary = "Recent events", description = "Snapshot of recent events (JSON fallback for non-SSE clients)")
    public ResponseEntity<ApiResponse<List<DfsEvent>>> recent() {
        User user = UserContext.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(eventService.recentEvents(user != null ? user.getUserId() : null)));
    }
}
