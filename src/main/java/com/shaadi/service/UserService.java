package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.User;
import com.shaadi.entity.Profile;
import com.shaadi.repository.UserRepository;
import com.shaadi.repository.ProfileRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;

    public UserService(UserRepository userRepo, ProfileRepository profileRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
    }

    public User register(User user) {
        // basic check: avoid duplicate email
        Optional<User> existing = userRepo.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        User savedUser = userRepo.save(user);

        // Create a new Profile linked to the user with null/default values
        Profile profile = new Profile();
        profile.setUser(savedUser);
        // age, gender, religion, location, bio, photoUrl are left as null/default
        profileRepo.save(profile);

        return savedUser;
    }

    public Optional<User> login(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public Optional<User> findById(Integer id) {
        return userRepo.findById(id);
    }

    public User updateUser(User user) {
        // Ensure the user exists
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id required for update");
        }
        return userRepo.save(user);
    }

    public void deleteUser(Integer id) {
        // Find the user to delete
        Optional<User> userOpt = userRepo.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Find and delete the associated profile
            Optional<Profile> profileOpt = profileRepo.findByUser(user);
            profileOpt.ifPresent(profile -> profileRepo.delete(profile));
            // Delete the user
            userRepo.deleteById(id);
        }
    }
}
