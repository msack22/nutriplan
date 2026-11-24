package com.nutriplan.planner.application.usecase;

import com.nutriplan.planner.application.dto.ProfileDto;
import com.nutriplan.planner.application.dto.UpdateProfileRequest;
import com.nutriplan.planner.domain.model.UserProfile;
import com.nutriplan.planner.domain.repository.UserProfileRepository;
import com.nutriplan.planner.domain.service.EnergyCalculator;
import com.nutriplan.planner.domain.service.NutritionTargets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserProfileRepository profiles;
    private final Clock clock;

    public ProfileService(UserProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    /** El perfil se crea "lazy" la primera vez que el usuario entra (no hay registro aparte). */
    @Transactional
    public UserProfile getOrCreate(String userId, String displayName) {
        return profiles.findById(userId).orElseGet(() -> profiles.save(new UserProfile(userId, displayName)));
    }

    @Transactional
    public ProfileDto get(String userId, String displayName) {
        return toDto(getOrCreate(userId, displayName));
    }

    @Transactional
    public ProfileDto update(String userId, String displayName, UpdateProfileRequest r) {
        UserProfile p = getOrCreate(userId, displayName);
        p.updateBody(r.sex(), r.birthDate(), r.heightCm(), r.currentWeightKg(), r.targetWeightKg(),
                r.activityLevel(), r.goal());
        p.updatePreferences(r.defaultPeople(), r.vegetarian(), r.enabledMeals(), r.excludedIngredients());
        return toDto(p);
    }

    /** null si el perfil está incompleto. */
    public NutritionTargets targetsOrNull(UserProfile p) {
        return p.isComplete() ? EnergyCalculator.targetsFor(p, LocalDate.now(clock)) : null;
    }

    private ProfileDto toDto(UserProfile p) {
        return new ProfileDto(p.getUserId(), p.getDisplayName(), p.getSex(), p.getBirthDate(), p.getHeightCm(),
                p.getCurrentWeightKg(), p.getTargetWeightKg(), p.getActivityLevel(), p.getGoal(), p.getDefaultPeople(),
                p.isVegetarian(), Set.copyOf(p.getEnabledMeals()), Set.copyOf(p.getExcludedIngredients()),
                p.isComplete(), targetsOrNull(p));
    }
}
