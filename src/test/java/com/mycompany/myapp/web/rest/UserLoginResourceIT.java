package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.JhipsterSampleApplicationApp;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.UserLogin;
import com.mycompany.myapp.repository.UserLoginRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.UserLoginService;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.service.mapper.UserMapper;
import com.mycompany.myapp.web.rest.errors.ExceptionTranslator;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link UserLoginResource} REST controller.
 */
@SpringBootTest(classes = JhipsterSampleApplicationApp.class)
public class UserLoginResourceIT {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String UPDATED_LOGIN = "jhipster";

    private static final Long DEFAULT_ID = 1L;

    private static final String DEFAULT_PASSWORD = "passjohndoe";
    private static final String UPDATED_PASSWORD = "passjhipster";

    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String UPDATED_EMAIL = "jhipster@localhost";

    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String UPDATED_FIRSTNAME = "jhipsterFirstName";

    private static final String DEFAULT_LASTNAME = "doe";
    private static final String UPDATED_LASTNAME = "jhipsterLastName";

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private CacheManager cacheManager;

    private MockMvc restUserMockMvc;

    private UserLogin userLogin;

    @BeforeEach
    public void setup() {
        cacheManager.getCache(UserLoginRepository.USERS_BY_LOGIN_CACHE).clear();
        cacheManager.getCache(UserLoginRepository.USERS_BY_EMAIL_CACHE).clear();
        UserLoginResource userLoginResource = new UserLoginResource(userLoginService, userLoginRepository, mailService);

        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userLoginResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    /**
     * Create a UserLogin.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the UserLogin entity.
     */
    public static UserLogin createEntity(EntityManager em) {
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(DEFAULT_LOGIN + RandomStringUtils.randomAlphabetic(5));
        userLogin.setPassword(RandomStringUtils.random(60));
        userLogin.setActivated(true);
        userLogin.setEmail(RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL);
        userLogin.setFirstName(DEFAULT_FIRSTNAME);
        userLogin.setLastName(DEFAULT_LASTNAME);
        userLogin.setImageUrl(DEFAULT_IMAGEURL);
        userLogin.setLangKey(DEFAULT_LANGKEY);
        return userLogin;
    }

    @BeforeEach
    public void initTest() {
        userLogin = createEntity(em);
        userLogin.setLogin(DEFAULT_LOGIN);
        userLogin.setEmail(DEFAULT_EMAIL);
    }

    @Test
    @Transactional
    public void createUser() throws Exception {
        int databaseSizeBeforeCreate = userLoginRepository.findAll().size();

        // Create the UserLogin
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isCreated());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeCreate + 1);
        UserLogin testUserLogin = userLoginList.get(userLoginList.size() - 1);
        assertThat(testUserLogin.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUserLogin.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(testUserLogin.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(testUserLogin.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testUserLogin.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(testUserLogin.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    @Transactional
    public void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userLoginRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(1L);
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createUserWithExistingLogin() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);
        int databaseSizeBeforeCreate = userLoginRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);// this login should already be used
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail("anothermail@localhost");
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Create the UserLogin
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createUserWithExistingEmail() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);
        int databaseSizeBeforeCreate = userLoginRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin("anotherlogin");
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);// this email should already be used
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Create the UserLogin
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllUsers() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);

        // Get all the users
        restUserMockMvc.perform(get("/api/users?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
            .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGEURL)))
            .andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
    }

    @Test
    @Transactional
    public void getUser() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);

        assertThat(cacheManager.getCache(UserLoginRepository.USERS_BY_LOGIN_CACHE).get(userLogin.getLogin())).isNull();

        // Get the userLogin
        restUserMockMvc.perform(get("/api/users/{login}", userLogin.getLogin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value(userLogin.getLogin()))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGEURL))
            .andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));

        assertThat(cacheManager.getCache(UserLoginRepository.USERS_BY_LOGIN_CACHE).get(userLogin.getLogin())).isNotNull();
    }

    @Test
    @Transactional
    public void getNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateUser() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);
        int databaseSizeBeforeUpdate = userLoginRepository.findAll().size();

        // Update the userLogin
        UserLogin updatedUserLogin = userLoginRepository.findById(userLogin.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUserLogin.getId());
        managedUserVM.setLogin(updatedUserLogin.getLogin());
        managedUserVM.setPassword(UPDATED_PASSWORD);
        managedUserVM.setFirstName(UPDATED_FIRSTNAME);
        managedUserVM.setLastName(UPDATED_LASTNAME);
        managedUserVM.setEmail(UPDATED_EMAIL);
        managedUserVM.setActivated(updatedUserLogin.getActivated());
        managedUserVM.setImageUrl(UPDATED_IMAGEURL);
        managedUserVM.setLangKey(UPDATED_LANGKEY);
        managedUserVM.setCreatedBy(updatedUserLogin.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUserLogin.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUserLogin.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUserLogin.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isOk());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeUpdate);
        UserLogin testUserLogin = userLoginList.get(userLoginList.size() - 1);
        assertThat(testUserLogin.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUserLogin.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUserLogin.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testUserLogin.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        assertThat(testUserLogin.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    @Transactional
    public void updateUserLogin() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);
        int databaseSizeBeforeUpdate = userLoginRepository.findAll().size();

        // Update the userLogin
        UserLogin updatedUserLogin = userLoginRepository.findById(userLogin.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUserLogin.getId());
        managedUserVM.setLogin(UPDATED_LOGIN);
        managedUserVM.setPassword(UPDATED_PASSWORD);
        managedUserVM.setFirstName(UPDATED_FIRSTNAME);
        managedUserVM.setLastName(UPDATED_LASTNAME);
        managedUserVM.setEmail(UPDATED_EMAIL);
        managedUserVM.setActivated(updatedUserLogin.getActivated());
        managedUserVM.setImageUrl(UPDATED_IMAGEURL);
        managedUserVM.setLangKey(UPDATED_LANGKEY);
        managedUserVM.setCreatedBy(updatedUserLogin.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUserLogin.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUserLogin.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUserLogin.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isOk());

        // Validate the UserLogin in the database
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeUpdate);
        UserLogin testUserLogin = userLoginList.get(userLoginList.size() - 1);
        assertThat(testUserLogin.getLogin()).isEqualTo(UPDATED_LOGIN);
        assertThat(testUserLogin.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUserLogin.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUserLogin.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testUserLogin.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        assertThat(testUserLogin.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    @Transactional
    public void updateUserExistingEmail() throws Exception {
        // Initialize the database with 2 users
        userLoginRepository.saveAndFlush(userLogin);

        UserLogin anotherUserLogin = new UserLogin();
        anotherUserLogin.setLogin("jhipster");
        anotherUserLogin.setPassword(RandomStringUtils.random(60));
        anotherUserLogin.setActivated(true);
        anotherUserLogin.setEmail("jhipster@localhost");
        anotherUserLogin.setFirstName("java");
        anotherUserLogin.setLastName("hipster");
        anotherUserLogin.setImageUrl("");
        anotherUserLogin.setLangKey("en");
        userLoginRepository.saveAndFlush(anotherUserLogin);

        // Update the userLogin
        UserLogin updatedUserLogin = userLoginRepository.findById(userLogin.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUserLogin.getId());
        managedUserVM.setLogin(updatedUserLogin.getLogin());
        managedUserVM.setPassword(updatedUserLogin.getPassword());
        managedUserVM.setFirstName(updatedUserLogin.getFirstName());
        managedUserVM.setLastName(updatedUserLogin.getLastName());
        managedUserVM.setEmail("jhipster@localhost");// this email should already be used by anotherUserLogin
        managedUserVM.setActivated(updatedUserLogin.getActivated());
        managedUserVM.setImageUrl(updatedUserLogin.getImageUrl());
        managedUserVM.setLangKey(updatedUserLogin.getLangKey());
        managedUserVM.setCreatedBy(updatedUserLogin.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUserLogin.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUserLogin.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUserLogin.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void updateUserExistingLogin() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);

        UserLogin anotherUserLogin = new UserLogin();
        anotherUserLogin.setLogin("jhipster");
        anotherUserLogin.setPassword(RandomStringUtils.random(60));
        anotherUserLogin.setActivated(true);
        anotherUserLogin.setEmail("jhipster@localhost");
        anotherUserLogin.setFirstName("java");
        anotherUserLogin.setLastName("hipster");
        anotherUserLogin.setImageUrl("");
        anotherUserLogin.setLangKey("en");
        userLoginRepository.saveAndFlush(anotherUserLogin);

        // Update the userLogin
        UserLogin updatedUserLogin = userLoginRepository.findById(userLogin.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUserLogin.getId());
        managedUserVM.setLogin("jhipster");// this login should already be used by anotherUserLogin
        managedUserVM.setPassword(updatedUserLogin.getPassword());
        managedUserVM.setFirstName(updatedUserLogin.getFirstName());
        managedUserVM.setLastName(updatedUserLogin.getLastName());
        managedUserVM.setEmail(updatedUserLogin.getEmail());
        managedUserVM.setActivated(updatedUserLogin.getActivated());
        managedUserVM.setImageUrl(updatedUserLogin.getImageUrl());
        managedUserVM.setLangKey(updatedUserLogin.getLangKey());
        managedUserVM.setCreatedBy(updatedUserLogin.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUserLogin.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUserLogin.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUserLogin.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void deleteUser() throws Exception {
        // Initialize the database
        userLoginRepository.saveAndFlush(userLogin);
        int databaseSizeBeforeDelete = userLoginRepository.findAll().size();

        // Delete the userLogin
        restUserMockMvc.perform(delete("/api/users/{login}", userLogin.getLogin())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        assertThat(cacheManager.getCache(UserLoginRepository.USERS_BY_LOGIN_CACHE).get(userLogin.getLogin())).isNull();

        // Validate the database is empty
        List<UserLogin> userLoginList = userLoginRepository.findAll();
        assertThat(userLoginList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void getAllAuthorities() throws Exception {
        restUserMockMvc.perform(get("/api/users/authorities")
            .accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").value(hasItems(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN)));
    }

    @Test
    @Transactional
    public void testUserEquals() throws Exception {
        TestUtil.equalsVerifier(UserLogin.class);
        UserLogin userLogin1 = new UserLogin();
        userLogin1.setId(1L);
        UserLogin userLogin2 = new UserLogin();
        userLogin2.setId(userLogin1.getId());
        assertThat(userLogin1).isEqualTo(userLogin2);
        userLogin2.setId(2L);
        assertThat(userLogin1).isNotEqualTo(userLogin2);
        userLogin1.setId(null);
        assertThat(userLogin1).isNotEqualTo(userLogin2);
    }

    @Test
    public void testUserDTOtoUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(DEFAULT_ID);
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFirstName(DEFAULT_FIRSTNAME);
        userDTO.setLastName(DEFAULT_LASTNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setImageUrl(DEFAULT_IMAGEURL);
        userDTO.setLangKey(DEFAULT_LANGKEY);
        userDTO.setCreatedBy(DEFAULT_LOGIN);
        userDTO.setLastModifiedBy(DEFAULT_LOGIN);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserLogin userLogin = userMapper.userDTOToUser(userDTO);
        assertThat(userLogin.getId()).isEqualTo(DEFAULT_ID);
        assertThat(userLogin.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userLogin.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userLogin.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userLogin.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userLogin.getActivated()).isEqualTo(true);
        assertThat(userLogin.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(userLogin.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(userLogin.getCreatedBy()).isNull();
        assertThat(userLogin.getCreatedDate()).isNotNull();
        assertThat(userLogin.getLastModifiedBy()).isNull();
        assertThat(userLogin.getLastModifiedDate()).isNotNull();
        assertThat(userLogin.getAuthorities()).extracting("name").containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    public void testUserToUserDTO() {
        userLogin.setId(DEFAULT_ID);
        userLogin.setCreatedBy(DEFAULT_LOGIN);
        userLogin.setCreatedDate(Instant.now());
        userLogin.setLastModifiedBy(DEFAULT_LOGIN);
        userLogin.setLastModifiedDate(Instant.now());
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        userLogin.setAuthorities(authorities);

        UserDTO userDTO = userMapper.userToUserDTO(userLogin);

        assertThat(userDTO.getId()).isEqualTo(DEFAULT_ID);
        assertThat(userDTO.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userDTO.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userDTO.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userDTO.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userDTO.isActivated()).isEqualTo(true);
        assertThat(userDTO.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(userDTO.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(userDTO.getCreatedBy()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userDTO.getCreatedDate()).isEqualTo(userLogin.getCreatedDate());
        assertThat(userDTO.getLastModifiedBy()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userDTO.getLastModifiedDate()).isEqualTo(userLogin.getLastModifiedDate());
        assertThat(userDTO.getAuthorities()).containsExactly(AuthoritiesConstants.USER);
        assertThat(userDTO.toString()).isNotNull();
    }

    @Test
    public void testAuthorityEquals() {
        Authority authorityA = new Authority();
        assertThat(authorityA).isEqualTo(authorityA);
        assertThat(authorityA).isNotEqualTo(null);
        assertThat(authorityA).isNotEqualTo(new Object());
        assertThat(authorityA.hashCode()).isEqualTo(0);
        assertThat(authorityA.toString()).isNotNull();

        Authority authorityB = new Authority();
        assertThat(authorityA).isEqualTo(authorityB);

        authorityB.setName(AuthoritiesConstants.ADMIN);
        assertThat(authorityA).isNotEqualTo(authorityB);

        authorityA.setName(AuthoritiesConstants.USER);
        assertThat(authorityA).isNotEqualTo(authorityB);

        authorityB.setName(AuthoritiesConstants.USER);
        assertThat(authorityA).isEqualTo(authorityB);
        assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode());
    }
}
