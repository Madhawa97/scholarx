package org.sefglobal.scholarx.service;

import org.sefglobal.scholarx.exception.OAuth2AuthenticationProcessingException;
import org.sefglobal.scholarx.exception.UserAlreadyExistsAuthenticationException;
import org.sefglobal.scholarx.exception.ResourceNotFoundException;
import org.sefglobal.scholarx.model.Profile;
import org.sefglobal.scholarx.oauth.GoogleAuthUserInfo;
import org.sefglobal.scholarx.repository.ProfileRepository;
import org.sefglobal.scholarx.util.ProfileType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileService {
    private final static Logger log = LoggerFactory.getLogger(ProfileService.class);
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile processUserRegistration(Map<String, Object> attributes)
            throws OAuth2AuthenticationProcessingException, UserAlreadyExistsAuthenticationException {
        GoogleAuthUserInfo googleAuthUserInfo = new GoogleAuthUserInfo(attributes);
        if (StringUtils.isEmpty(googleAuthUserInfo.getName())) {
            throw new OAuth2AuthenticationProcessingException("Name not found from OAuth2 provider");
        } else if (StringUtils.isEmpty(googleAuthUserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }
        Optional<Profile> profile = profileRepository.findByUid(googleAuthUserInfo.getId());
        if (profile.isPresent()) {
            profile.get().setFirstName(googleAuthUserInfo.getName().split(" ",2)[0]);
            profile.get().setLastName(googleAuthUserInfo.getName().split(" ",2)[1]);
            profile.get().setImageUrl(googleAuthUserInfo.getImageUrl());
            return profileRepository.save(profile.get());
        } else {
            return createProfile(googleAuthUserInfo);
        }
    }

    public Profile createProfile(GoogleAuthUserInfo oAuth2UserInfo) throws UserAlreadyExistsAuthenticationException {
        if (oAuth2UserInfo.getId() != null && profileRepository.existsByUid(oAuth2UserInfo.getId())) {
            throw new UserAlreadyExistsAuthenticationException(
                    "User with Uid " + oAuth2UserInfo.getId() + " already exist");
        } else if (profileRepository.existsByEmail(oAuth2UserInfo.getEmail())) {
            throw new UserAlreadyExistsAuthenticationException(
                    "User with email id " + oAuth2UserInfo.getEmail() + " already exist");
        }
        Profile profile = buildProfile(oAuth2UserInfo);
        return profileRepository.save(profile);
    }

    private Profile buildProfile(GoogleAuthUserInfo oAuth2UserInfo) {
        Profile profile = new Profile();
        profile.setFirstName(oAuth2UserInfo.getName().split(" ", 2)[0]);
        profile.setLastName(oAuth2UserInfo.getName().split(" ", 2)[1]);
        profile.setEmail(oAuth2UserInfo.getEmail());
        profile.setUid(oAuth2UserInfo.getId());
        profile.setType(ProfileType.DEFAULT);
        profile.setImageUrl(oAuth2UserInfo.getImageUrl());
        Date now = Calendar.getInstance().getTime();
        profile.setHasConfirmedUserDetails(false);
        profile.setCreatedAt(now);
        profile.setLastUpdatedAt(now);
        return profile;
    }

    public Profile getLoggedUser(long profileId)
            throws ResourceNotFoundException {
        Optional<Profile> optionalProfile = profileRepository.findById(profileId);
        if (!optionalProfile.isPresent()) {
            String msg = "Error, User with id: " + profileId + " doesn't exist.";
            log.error(msg);
            throw new ResourceNotFoundException(msg);
        }
        return optionalProfile.get();
    }

    public Profile updateUserDetails(long profileId, Profile profile)
            throws ResourceNotFoundException {
        Optional <Profile> optionalUser = profileRepository.findById(profileId);
        if (!optionalUser.isPresent()) {
            String msg = "Error, Unable update details. " +
                    "Profile with id: " + profileId + " doesn't exist.";
            log.error(msg);
            throw new ResourceNotFoundException(msg);
        }
        optionalUser.get().setEmail(profile.getEmail());
        optionalUser.get().setHasConfirmedUserDetails(true);
        return profileRepository.save(optionalUser.get());
    }
}
