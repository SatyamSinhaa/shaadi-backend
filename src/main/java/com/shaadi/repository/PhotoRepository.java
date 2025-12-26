package com.shaadi.repository;

import com.shaadi.entity.Photo;
import com.shaadi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByUser(User user);
    void deleteByUser(User user);
}
