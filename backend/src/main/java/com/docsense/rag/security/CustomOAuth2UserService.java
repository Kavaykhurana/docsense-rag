package com.docsense.rag.security;

import com.docsense.rag.entity.User;
import com.docsense.rag.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the Google profile and creates/updates the local user account on
 * first sign-in (spec §3.4). Only the minimum profile attributes are stored.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return createUser(oAuth2User);
    }

    private OAuth2User createUser(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException("Google profile missing required identifiers");
        }

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(User::new);

        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setProfilePicture(picture);
        user = userRepository.save(user);

        return new UserPrincipal(user, oAuth2User.getAttributes());
    }
}
