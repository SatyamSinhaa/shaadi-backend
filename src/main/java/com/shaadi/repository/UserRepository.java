package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.age BETWEEN :minAge AND :maxAge " +
           "AND (:location IS NULL OR u.location LIKE %:location%) " +
           "AND (:religion IS NULL OR u.religion LIKE %:religion%)")
    List<User> findByAgeBetweenAndLocationContainingAndReligionContaining(
        @Param("minAge") int minAge,
        @Param("maxAge") int maxAge,
        @Param("location") String location,
        @Param("religion") String religion);
}
