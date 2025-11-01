package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Profile;
import com.shaadi.repository.ProfileRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepo;

    public ProfileService(ProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    public Profile updateProfile(Profile profile) {
        if (profile.getId() == null || profile.getId() == 0) {
            throw new IllegalArgumentException("Profile id required for update");
        }
        Profile existing = profileRepo.findById(profile.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + profile.getId()));
        if (profile.getUser() != null) {
            existing.setUser(profile.getUser());
        }
        if (profile.getAge() != 0) {
            existing.setAge(profile.getAge());
        }
        if (profile.getGender() != null) {
            existing.setGender(profile.getGender());
        }
        if (profile.getReligion() != null) {
            existing.setReligion(profile.getReligion());
        }
        if (profile.getLocation() != null) {
            existing.setLocation(profile.getLocation());
        }
        if (profile.getBio() != null) {
            existing.setBio(profile.getBio());
        }
        if (profile.getPhotoUrl() != null) {
            existing.setPhotoUrl(profile.getPhotoUrl());
        }
        return profileRepo.save(existing);
    }

    public Optional<Profile> findById(int id) {
        return profileRepo.findById(id);
    }

    public List<Profile> findAll() {
        return profileRepo.findAll();
    }

    /**
     * Basic search: age range + substring match for location & religion.
     * Keep defaults in controller if needed.
     */
    public List<Profile> search(int minAge, int maxAge, String location, String religion) {
        return profileRepo.findByAgeBetweenAndLocationContainingAndReligionContaining(
                minAge, maxAge,
                location == null ? "" : location,
                religion == null ? "" : religion
        );
    }

    public void deleteProfile(int id) {
        profileRepo.deleteById(id);
    }
}
