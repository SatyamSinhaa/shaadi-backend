package com.shaadi.controller;

import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.Profile;
import com.shaadi.service.ProfileService;

import java.util.*;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PutMapping("/{id}")
    public Profile updateProfile(@PathVariable Integer id, @RequestBody Profile profile) {
        profile.setId(id);
        return profileService.updateProfile(profile);
    }

    @GetMapping
    public List<Profile> getAllProfiles() {
        return profileService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Profile> getProfileById(@PathVariable int id) {
        return profileService.findById(id);
    }

    @GetMapping("/search")
    public List<Profile> search(
            @RequestParam(defaultValue = "18") int minAge,
            @RequestParam(defaultValue = "60") int maxAge,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String religion) {
        return profileService.search(minAge, maxAge, location, religion);
    }


}
