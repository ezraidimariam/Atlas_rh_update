package com.atlasrh.controller;

import com.atlasrh.dto.request.DepartmentRequest;
import com.atlasrh.dto.response.DepartmentResponse;
import com.atlasrh.exception.ApiError;
import com.atlasrh.service.DepartmentService;
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
@RequestMapping("/api/departments")
@Tag(name = "Départements", description = "Gestion des départements par le responsable RH")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Données ou règle métier invalides",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Département introuvable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Nom déjà utilisé ou suppression impossible",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un département", description = "Crée un département dont le nom doit être unique.")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un département", description = "Remplace le nom et la description d'un département.")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un département", description = "Retourne un département à partir de son identifiant.")
    public DepartmentResponse get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @GetMapping
    @Operation(summary = "Lister les départements", description = "Retourne la liste paginée et triable des départements.")
    public Page<DepartmentResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return departmentService.list(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un département", description = "Refuse la suppression si le département contient des employés.")
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
