package com.atlasrh.controller;

import com.atlasrh.dto.request.EmployeeRequest;
import com.atlasrh.dto.response.EmployeeResponse;
import com.atlasrh.exception.ApiError;
import com.atlasrh.service.EmployeeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employés", description = "Gestion RH des fiches employés")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Données ou règle métier invalides",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Employé ou département introuvable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Matricule ou e-mail déjà utilisé",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un employé", description = "Crée une fiche avec un département obligatoire et un manager facultatif.")
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un employé", description = "Modifie les informations de la fiche employé.")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un employé", description = "Retourne la fiche d'un employé.")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @GetMapping
    @Operation(summary = "Rechercher et lister les employés",
            description = "Recherche par prénom ou nom, filtre par département, et applique pagination et tri.")
    public Page<EmployeeResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return employeeService.search(search, departmentId, pageable);
    }

    @GetMapping("/managers/{managerId}/team")
    @Operation(summary = "Consulter l'équipe d'un manager", description = "Retourne les employés rattachés directement au manager.")
    public List<EmployeeResponse> team(@PathVariable Long managerId) {
        return employeeService.getTeam(managerId);
    }
}
