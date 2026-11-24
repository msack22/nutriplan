package com.nutriplan.planner.web;

import com.nutriplan.planner.application.dto.ProfileDto;
import com.nutriplan.planner.application.dto.UpdateProfileRequest;
import com.nutriplan.planner.application.usecase.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil del usuario logueado. Notar que NO hay {id} en la URL: el usuario sale del JWT
 * ({@code @AuthenticationPrincipal}), así es imposible leer/editar el perfil de otro.
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Perfil", description = "Datos corporales, preferencias y objetivos calóricos")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    @Operation(summary = "Mi perfil + objetivos diarios calculados (BMR, TDEE, kcal, macros)")
    public ProfileDto get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(CurrentJwt.userId(jwt), CurrentJwt.name(jwt));
    }

    @PutMapping
    @Operation(summary = "Actualiza mi perfil")
    public ProfileDto update(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UpdateProfileRequest request) {
        return profiles.update(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), request);
    }
}
