package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shaadi.entity.Message;
import com.shaadi.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findBySenderOrReceiver(User sender, User receiver);
}
