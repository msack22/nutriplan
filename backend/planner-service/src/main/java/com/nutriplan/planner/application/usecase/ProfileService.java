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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserProfileRepository profiles;
    private final Clock clock;
    private final TransactionTemplate newTransaction;

    public ProfileService(UserProfileRepository profiles, Clock clock, PlatformTransactionManager txManager) {
        this.profiles = profiles;
        this.clock = clock;
        this.newTransaction = new TransactionTemplate(txManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * El perfil se crea "lazy" la primera vez que el usuario entra (no hay registro aparte).
     *
     * <p><b>Race condition (check-then-act)</b>: el frontend pide plan y perfil en paralelo; los dos
     * requests ven "no existe" e intentan insertar => uno falla por PK duplicada. Solución: insertar
     * en una transacción NUEVA y, si choca, asumir que el otro request ya lo creó y releerlo con
     * una lectura con lock (que ve la fila recién confirmada).
     */
    @Transactional
    public UserProfile getOrCreate(String userId, String displayName) {
        return profiles.findById(userId).orElseGet(() -> createSafely(userId, displayName));
    }

    private UserProfile createSafely(String userId, String displayName) {
        try {
            newTransaction.executeWithoutResult(s -> profiles.saveAndFlush(new UserProfile(userId, displayName)));
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException alreadyCreated) {
            // Otro request lo insertó al mismo tiempo: está bien, lo leemos.
            // (Como el id es asignado -el sub del JWT- Spring Data hace merge() en vez de persist(),
            // así que el choque puede llegar como PK duplicada o como conflicto de versión.)
        }
        return profiles.findLockedById(userId).orElseThrow();
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
