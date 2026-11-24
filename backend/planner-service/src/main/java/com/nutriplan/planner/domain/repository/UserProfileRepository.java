package com.nutriplan.planner.domain.repository;

import com.nutriplan.planner.domain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}
