package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.Favourite;
import com.shaadi.entity.User;

import java.util.List;
import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {
    @Query("SELECT f FROM Favourite f JOIN FETCH f.favouritedUser WHERE f.user = :user")
    List<Favourite> findByUser(User user);

    Optional<Favourite> findByUserAndFavouritedUser(User user, User favouritedUser);

    @Query("SELECT f FROM Favourite f JOIN FETCH f.favouritedUser WHERE f.user = :user")
    List<Favourite> findFavouritedUsersByUser(@Param("user") User user);
}
