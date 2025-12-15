package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.Block;
import com.shaadi.entity.User;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    @Query("SELECT b FROM Block b JOIN FETCH b.blocked WHERE b.blocker = :blocker")
    List<Block> findByBlocker(User blocker);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    @Query("SELECT b FROM Block b WHERE b.blocker = :blocker AND b.blocked = :blocked")
    Optional<Block> findBlock(@Param("blocker") User blocker, @Param("blocked") User blocked);

    @Query("SELECT COUNT(b) > 0 FROM Block b WHERE b.blocker = :blocker AND b.blocked = :blocked")
    boolean existsByBlockerAndBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);
}
