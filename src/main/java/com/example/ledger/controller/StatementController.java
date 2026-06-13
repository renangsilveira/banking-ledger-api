package com.example.ledger.controller;

import com.example.ledger.dto.response.StatementResponse;
import com.example.ledger.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Statement", description = "Account statement endpoints")
public class StatementController {

    private final BalanceService balanceService;

    public StatementController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Get paginated account statement with optional date range filter")
    public ResponseEntity<Page<StatementResponse>> getStatement(
            @PathVariable UUID id,

            @Parameter(description = "Filter from date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Filter to date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(balanceService.getStatement(id, from, to, pageable));
    }
}
