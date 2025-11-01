package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shaadi.entity.Profile;
import com.shaadi.entity.User;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {
    List<Profile> findByAgeBetweenAndLocationContainingAndReligionContaining(
        int minAge, int maxAge, String location, String religion);

    Optional<Profile> findByUser(User user);
}
