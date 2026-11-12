package com.atlasrh.controller;

import com.atlasrh.dto.request.ContractEndRequest;
import com.atlasrh.dto.request.ContractRequest;
import com.atlasrh.dto.response.ContractResponse;
import com.atlasrh.exception.ApiError;
import com.atlasrh.service.ContractService;
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

@RestController
@RequestMapping("/api")
@Tag(name = "Contrats", description = "Création et suivi des contrats des employés")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Dates ou salaire invalides, ou contrat non terminable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Employé ou contrat introuvable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Un contrat est déjà en cours",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/employees/{employeeId}/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un contrat", description = "Crée un contrat CDI, CDD ou STAGE pour l'employé indiqué.")
    public ContractResponse create(@PathVariable Long employeeId, @Valid @RequestBody ContractRequest request) {
        return contractService.create(employeeId, request);
    }

    @GetMapping("/employees/{employeeId}/contracts")
    @Operation(summary = "Consulter l'historique des contrats", description = "Retourne les contrats de l'employé, du plus récent au plus ancien.")
    public Page<ContractResponse> history(@PathVariable Long employeeId,
                                          @ParameterObject @PageableDefault(size = 20, sort = "startDate,desc") Pageable pageable) {
        return contractService.history(employeeId, pageable);
    }

    @PatchMapping("/contracts/{contractId}/terminate")
    @Operation(summary = "Terminer un contrat en cours", description = "Enregistre une date de fin qui ne peut pas être future.")
    public ContractResponse terminate(@PathVariable Long contractId,
                                      @Valid @RequestBody ContractEndRequest request) {
        return contractService.terminate(contractId, request.endDate());
    }
}
