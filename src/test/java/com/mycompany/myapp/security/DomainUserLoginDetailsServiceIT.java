package com.mycompany.myapp.security;

import com.mycompany.myapp.JhipsterSampleApplicationApp;
import com.mycompany.myapp.domain.UserLogin;
import com.mycompany.myapp.repository.UserLoginRepository;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integrations tests for {@link DomainUserDetailsService}.
 */
@SpringBootTest(classes = JhipsterSampleApplicationApp.class)
@Transactional
public class DomainUserLoginDetailsServiceIT {

    private static final String USER_ONE_LOGIN = "test-user-one";
    private static final String USER_ONE_EMAIL = "test-user-one@localhost";
    private static final String USER_TWO_LOGIN = "test-user-two";
    private static final String USER_TWO_EMAIL = "test-user-two@localhost";
    private static final String USER_THREE_LOGIN = "test-user-three";
    private static final String USER_THREE_EMAIL = "test-user-three@localhost";

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private UserDetailsService domainUserDetailsService;

    private UserLogin userLoginOne;
    private UserLogin userLoginTwo;
    private UserLogin userLoginThree;

    @BeforeEach
    public void init() {
        userLoginOne = new UserLogin();
        userLoginOne.setLogin(USER_ONE_LOGIN);
        userLoginOne.setPassword(RandomStringUtils.random(60));
        userLoginOne.setActivated(true);
        userLoginOne.setEmail(USER_ONE_EMAIL);
        userLoginOne.setFirstName("userLoginOne");
        userLoginOne.setLastName("doe");
        userLoginOne.setLangKey("en");
        userLoginRepository.save(userLoginOne);

        userLoginTwo = new UserLogin();
        userLoginTwo.setLogin(USER_TWO_LOGIN);
        userLoginTwo.setPassword(RandomStringUtils.random(60));
        userLoginTwo.setActivated(true);
        userLoginTwo.setEmail(USER_TWO_EMAIL);
        userLoginTwo.setFirstName("userLoginTwo");
        userLoginTwo.setLastName("doe");
        userLoginTwo.setLangKey("en");
        userLoginRepository.save(userLoginTwo);

        userLoginThree = new UserLogin();
        userLoginThree.setLogin(USER_THREE_LOGIN);
        userLoginThree.setPassword(RandomStringUtils.random(60));
        userLoginThree.setActivated(false);
        userLoginThree.setEmail(USER_THREE_EMAIL);
        userLoginThree.setFirstName("userLoginThree");
        userLoginThree.setLastName("doe");
        userLoginThree.setLangKey("en");
        userLoginRepository.save(userLoginThree);
    }

    @Test
    @Transactional
    public void assertThatUserCanBeFoundByLogin() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_LOGIN);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    @Transactional
    public void assertThatUserCanBeFoundByLoginIgnoreCase() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_LOGIN.toUpperCase(Locale.ENGLISH));
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    @Transactional
    public void assertThatUserCanBeFoundByEmail() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_TWO_EMAIL);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_TWO_LOGIN);
    }

    @Test
    @Transactional
    public void assertThatUserCanBeFoundByEmailIgnoreCase() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_TWO_EMAIL.toUpperCase(Locale.ENGLISH));
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_TWO_LOGIN);
    }

    @Test
    @Transactional
    public void assertThatEmailIsPrioritizedOverLogin() {
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_ONE_EMAIL);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    @Transactional
    public void assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        assertThatExceptionOfType(UserNotActivatedException.class).isThrownBy(
            () -> domainUserDetailsService.loadUserByUsername(USER_THREE_LOGIN));
    }

}
