package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.JhipsterSampleApplicationApp;
import com.mycompany.myapp.domain.UserLogin;
import com.mycompany.myapp.service.dto.UserDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserMapper}.
 */
@SpringBootTest(classes = JhipsterSampleApplicationApp.class)
public class UserLoginMapperIT {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final Long DEFAULT_ID = 1L;

    @Autowired
    private UserMapper userMapper;

    private UserLogin userLogin;
    private UserDTO userDto;

    @BeforeEach
    public void init() {
        userLogin = new UserLogin();
        userLogin.setLogin(DEFAULT_LOGIN);
        userLogin.setPassword(RandomStringUtils.random(60));
        userLogin.setActivated(true);
        userLogin.setEmail("johndoe@localhost");
        userLogin.setFirstName("john");
        userLogin.setLastName("doe");
        userLogin.setImageUrl("image_url");
        userLogin.setLangKey("en");

        userDto = new UserDTO(userLogin);
    }

    @Test
    public void usersToUserDTOsShouldMapOnlyNonNullUsers() {
        List<UserLogin> userLogins = new ArrayList<>();
        userLogins.add(userLogin);
        userLogins.add(null);

        List<UserDTO> userDTOS = userMapper.usersToUserDTOs(userLogins);

        assertThat(userDTOS).isNotEmpty();
        assertThat(userDTOS).size().isEqualTo(1);
    }

    @Test
    public void userDTOsToUsersShouldMapOnlyNonNullUsers() {
        List<UserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);
        usersDto.add(null);

        List<UserLogin> userLogins = userMapper.userDTOsToUsers(usersDto);

        assertThat(userLogins).isNotEmpty();
        assertThat(userLogins).size().isEqualTo(1);
    }

    @Test
    public void userDTOsToUsersWithAuthoritiesStringShouldMapToUsersWithAuthoritiesDomain() {
        Set<String> authoritiesAsString = new HashSet<>();
        authoritiesAsString.add("ADMIN");
        userDto.setAuthorities(authoritiesAsString);

        List<UserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);

        List<UserLogin> userLogins = userMapper.userDTOsToUsers(usersDto);

        assertThat(userLogins).isNotEmpty();
        assertThat(userLogins).size().isEqualTo(1);
        assertThat(userLogins.get(0).getAuthorities()).isNotNull();
        assertThat(userLogins.get(0).getAuthorities()).isNotEmpty();
        assertThat(userLogins.get(0).getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    public void userDTOsToUsersMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
        userDto.setAuthorities(null);

        List<UserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);

        List<UserLogin> userLogins = userMapper.userDTOsToUsers(usersDto);

        assertThat(userLogins).isNotEmpty();
        assertThat(userLogins).size().isEqualTo(1);
        assertThat(userLogins.get(0).getAuthorities()).isNotNull();
        assertThat(userLogins.get(0).getAuthorities()).isEmpty();
    }

    @Test
    public void userDTOToUserMapWithAuthoritiesStringShouldReturnUserWithAuthorities() {
        Set<String> authoritiesAsString = new HashSet<>();
        authoritiesAsString.add("ADMIN");
        userDto.setAuthorities(authoritiesAsString);

        UserLogin userLogin = userMapper.userDTOToUser(userDto);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.getAuthorities()).isNotNull();
        assertThat(userLogin.getAuthorities()).isNotEmpty();
        assertThat(userLogin.getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    public void userDTOToUserMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
        userDto.setAuthorities(null);

        UserLogin userLogin = userMapper.userDTOToUser(userDto);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.getAuthorities()).isNotNull();
        assertThat(userLogin.getAuthorities()).isEmpty();
    }

    @Test
    public void userDTOToUserMapWithNullUserShouldReturnNull() {
        assertThat(userMapper.userDTOToUser(null)).isNull();
    }

    @Test
    public void testUserFromId() {
        assertThat(userMapper.userFromId(DEFAULT_ID).getId()).isEqualTo(DEFAULT_ID);
        assertThat(userMapper.userFromId(null)).isNull();
    }
}
