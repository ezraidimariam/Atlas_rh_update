package com.atlasrh.controller;

import com.atlasrh.dto.request.LeaveDecisionRequest;
import com.atlasrh.dto.request.LeaveRequestCreate;
import com.atlasrh.dto.response.LeaveBalanceResponse;
import com.atlasrh.dto.response.LeaveRequestResponse;
import com.atlasrh.dto.response.LeaveTypeResponse;
import com.atlasrh.dto.response.SeniorityResponse;
import com.atlasrh.exception.ApiError;
import com.atlasrh.enums.RequestStatus;
import com.atlasrh.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Congés", description = "Demandes, décisions manager, soldes et ancienneté")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Dates, solde ou règle métier invalides",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Employé, demande ou type de congé introuvable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Chevauchement ou conflit de demande",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping("/employees/{employeeId}/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une demande de congé",
            description = "Calcule les jours ouvrés inclusifs et refuse les périodes incohérentes ou chevauchantes.")
    public LeaveRequestResponse create(@PathVariable Long employeeId,
                                      @Valid @RequestBody LeaveRequestCreate request) {
        return leaveRequestService.create(employeeId, request);
    }

    @GetMapping("/employees/{employeeId}/leave-requests")
    @Operation(summary = "Consulter les demandes d'un employé", description = "Filtre facultatif par statut, avec pagination.")
    public Page<LeaveRequestResponse> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) RequestStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "startDate,desc") Pageable pageable) {
        return leaveRequestService.listForEmployee(employeeId, status, pageable);
    }

    @PatchMapping("/employees/{employeeId}/leave-requests/{requestId}/cancel")
    @Operation(summary = "Annuler une demande", description = "Seul l'employé concerné peut annuler une demande en attente.")
    public LeaveRequestResponse cancel(@PathVariable Long employeeId, @PathVariable Long requestId) {
        return leaveRequestService.cancel(employeeId, requestId);
    }

    @GetMapping("/managers/{managerId}/leave-requests/pending")
    @Operation(summary = "Voir les demandes en attente de l'équipe", description = "Liste les demandes en attente des collaborateurs directs.")
    public Page<LeaveRequestResponse> pendingForManager(
            @PathVariable Long managerId,
            @ParameterObject @PageableDefault(size = 20, sort = "startDate") Pageable pageable) {
        return leaveRequestService.pendingForManager(managerId, pageable);
    }

    @PostMapping("/managers/{managerId}/leave-requests/{requestId}/approve")
    @Operation(summary = "Approuver une demande", description = "Vérifie le solde de chaque année couverte avant approbation.")
    public LeaveRequestResponse approve(@PathVariable Long managerId, @PathVariable Long requestId) {
        return leaveRequestService.approve(managerId, requestId);
    }

    @PostMapping("/managers/{managerId}/leave-requests/{requestId}/reject")
    @Operation(summary = "Refuser une demande", description = "Un commentaire non vide est obligatoire.")
    public LeaveRequestResponse reject(@PathVariable Long managerId, @PathVariable Long requestId,
                                       @Valid @RequestBody LeaveDecisionRequest request) {
        return leaveRequestService.reject(managerId, requestId, request.comment());
    }

    @GetMapping("/employees/{employeeId}/leave-balances")
    @Operation(summary = "Consulter les soldes de congés", description = "Calcule le solde restant pour chaque type de congé pour l'année demandée, par défaut l'année courante.")
    public List<LeaveBalanceResponse> balances(@PathVariable Long employeeId,
                                               @RequestParam(required = false) Integer year) {
        return leaveRequestService.balances(employeeId, year == null ? LocalDate.now().getYear() : year);
    }

    @GetMapping("/employees/{employeeId}/seniority")
    @Operation(summary = "Consulter l'ancienneté", description = "Calcule les années et mois écoulés depuis la date d'embauche.")
    public SeniorityResponse seniority(@PathVariable Long employeeId) {
        return leaveRequestService.seniority(employeeId);
    }

    @GetMapping("/leave-types")
    @Operation(summary = "Lister les types de congé", description = "Retourne les types configurés et leurs jours autorisés par an.")
    public List<LeaveTypeResponse> leaveTypes() {
        return leaveRequestService.leaveTypes();
    }
}
