package com.nutriplan.planner.domain.repository;

import com.nutriplan.planner.domain.model.UserProfile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    /**
     * Lectura con lock compartido ({@code SELECT ... FOR SHARE}). En InnoDB una "locking read" ve la
     * última versión confirmada, a diferencia de un SELECT normal que en REPEATABLE READ usa la foto
     * tomada al inicio de la transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from UserProfile p where p.userId = :id")
    Optional<UserProfile> findLockedById(@Param("id") String id);
}
