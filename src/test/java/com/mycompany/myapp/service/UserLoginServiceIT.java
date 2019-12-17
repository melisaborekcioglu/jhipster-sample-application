package com.mycompany.myapp.service;

import com.mycompany.myapp.JhipsterSampleApplicationApp;
import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.UserLogin;
import com.mycompany.myapp.repository.UserLoginRepository;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.service.util.RandomUtil;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link UserLoginService}.
 */
@SpringBootTest(classes = JhipsterSampleApplicationApp.class)
@Transactional
public class UserLoginServiceIT {

    private static final String DEFAULT_LOGIN = "johndoe";

    private static final String DEFAULT_EMAIL = "johndoe@localhost";

    private static final String DEFAULT_FIRSTNAME = "john";

    private static final String DEFAULT_LASTNAME = "doe";

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";

    private static final String DEFAULT_LANGKEY = "dummy";

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private AuditingHandler auditingHandler;

    @Mock
    private DateTimeProvider dateTimeProvider;

    private UserLogin userLogin;

    @BeforeEach
    public void init() {
        userLogin = new UserLogin();
        userLogin.setLogin(DEFAULT_LOGIN);
        userLogin.setPassword(RandomStringUtils.random(60));
        userLogin.setActivated(true);
        userLogin.setEmail(DEFAULT_EMAIL);
        userLogin.setFirstName(DEFAULT_FIRSTNAME);
        userLogin.setLastName(DEFAULT_LASTNAME);
        userLogin.setImageUrl(DEFAULT_IMAGEURL);
        userLogin.setLangKey(DEFAULT_LANGKEY);

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(LocalDateTime.now()));
        auditingHandler.setDateTimeProvider(dateTimeProvider);
    }

    @Test
    @Transactional
    public void assertThatUserMustExistToResetPassword() {
        userLoginRepository.saveAndFlush(userLogin);
        Optional<UserLogin> maybeUser = userLoginService.requestPasswordReset("invalid.login@localhost");
        assertThat(maybeUser).isNotPresent();

        maybeUser = userLoginService.requestPasswordReset(userLogin.getEmail());
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.orElse(null).getEmail()).isEqualTo(userLogin.getEmail());
        assertThat(maybeUser.orElse(null).getResetDate()).isNotNull();
        assertThat(maybeUser.orElse(null).getResetKey()).isNotNull();
    }

    @Test
    @Transactional
    public void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        userLogin.setActivated(false);
        userLoginRepository.saveAndFlush(userLogin);

        Optional<UserLogin> maybeUser = userLoginService.requestPasswordReset(userLogin.getLogin());
        assertThat(maybeUser).isNotPresent();
        userLoginRepository.delete(userLogin);
    }

    @Test
    @Transactional
    public void assertThatResetKeyMustNotBeOlderThan24Hours() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        userLogin.setActivated(true);
        userLogin.setResetDate(daysAgo);
        userLogin.setResetKey(resetKey);
        userLoginRepository.saveAndFlush(userLogin);

        Optional<UserLogin> maybeUser = userLoginService.completePasswordReset("johndoe2", userLogin.getResetKey());
        assertThat(maybeUser).isNotPresent();
        userLoginRepository.delete(userLogin);
    }

    @Test
    @Transactional
    public void assertThatResetKeyMustBeValid() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        userLogin.setActivated(true);
        userLogin.setResetDate(daysAgo);
        userLogin.setResetKey("1234");
        userLoginRepository.saveAndFlush(userLogin);

        Optional<UserLogin> maybeUser = userLoginService.completePasswordReset("johndoe2", userLogin.getResetKey());
        assertThat(maybeUser).isNotPresent();
        userLoginRepository.delete(userLogin);
    }

    @Test
    @Transactional
    public void assertThatUserCanResetPassword() {
        String oldPassword = userLogin.getPassword();
        Instant daysAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        userLogin.setActivated(true);
        userLogin.setResetDate(daysAgo);
        userLogin.setResetKey(resetKey);
        userLoginRepository.saveAndFlush(userLogin);

        Optional<UserLogin> maybeUser = userLoginService.completePasswordReset("johndoe2", userLogin.getResetKey());
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.orElse(null).getResetDate()).isNull();
        assertThat(maybeUser.orElse(null).getResetKey()).isNull();
        assertThat(maybeUser.orElse(null).getPassword()).isNotEqualTo(oldPassword);

        userLoginRepository.delete(userLogin);
    }

    @Test
    @Transactional
    public void assertThatNotActivatedUsersWithNotNullActivationKeyCreatedBefore3DaysAreDeleted() {
        Instant now = Instant.now();
        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)));
        userLogin.setActivated(false);
        userLogin.setActivationKey(RandomStringUtils.random(20));
        UserLogin dbUserLogin = userLoginRepository.saveAndFlush(userLogin);
        dbUserLogin.setCreatedDate(now.minus(4, ChronoUnit.DAYS));
        userLoginRepository.saveAndFlush(userLogin);
        List<UserLogin> userLogins = userLoginRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(userLogins).isNotEmpty();
        userLoginService.removeNotActivatedUsers();
        userLogins = userLoginRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(userLogins).isEmpty();
    }

    @Test
    @Transactional
    public void assertThatNotActivatedUsersWithNullActivationKeyCreatedBefore3DaysAreNotDeleted() {
        Instant now = Instant.now();
        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)));
        userLogin.setActivated(false);
        UserLogin dbUserLogin = userLoginRepository.saveAndFlush(userLogin);
        dbUserLogin.setCreatedDate(now.minus(4, ChronoUnit.DAYS));
        userLoginRepository.saveAndFlush(userLogin);
        List<UserLogin> userLogins = userLoginRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(userLogins).isEmpty();
        userLoginService.removeNotActivatedUsers();
        Optional<UserLogin> maybeDbUser = userLoginRepository.findById(dbUserLogin.getId());
        assertThat(maybeDbUser).contains(dbUserLogin);
    }

    @Test
    @Transactional
    public void assertThatAnonymousUserIsNotGet() {
        userLogin.setLogin(Constants.ANONYMOUS_USER);
        if (!userLoginRepository.findOneByLogin(Constants.ANONYMOUS_USER).isPresent()) {
            userLoginRepository.saveAndFlush(userLogin);
        }
        final PageRequest pageable = PageRequest.of(0, (int) userLoginRepository.count());
        final Page<UserDTO> allManagedUsers = userLoginService.getAllManagedUsers(pageable);
        assertThat(allManagedUsers.getContent().stream()
            .noneMatch(user -> Constants.ANONYMOUS_USER.equals(user.getLogin())))
            .isTrue();
    }

}
