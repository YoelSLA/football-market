package footballmarket.integration;

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @AutoConfigureMockMvc
// @Testcontainers
// @ActiveProfiles("test")
// class LoginIntegrationTest {
//
//  @Autowired private MockMvc mockMvc;
//
//  @Autowired private UserRepository userRepository;
//
//  @Autowired private PasswordEncoder passwordEncoder;
//
//  @Autowired private ObjectMapper objectMapper;
//
////  @BeforeEach
////  void setUp() {
////    userRepository.deleteAll();
////    User user =
////
// User.builder().email("test@test.com").password(passwordEncoder.encode("password")).build();
////    userRepository.save(user);
////  }
//
//  //    @Test
//  //    @Disabled
//  //    void shouldLoginSuccessfully() throws Exception {
//  //        LoginRequest request = new LoginRequest();
//  //        request.setEmail("test@test.com");
//  //        request.setPassword("password");
//  //
//  //        mockMvc.perform(post("/api/auth/login")
//  //                .contentType(MediaType.APPLICATION_JSON)
//  //                .content(objectMapper.writeValueAsString(request)))
//  //                .andExpect(status().isOk())
//  //                .andExpect(jsonPath("$.token").exists());
//  //    }
//  //
//  //    @Test
//  //    @Disabled
//  //    void shouldFailLoginWithWrongPassword() throws Exception {
//  //        LoginRequest request = new LoginRequest();
//  //        request.setEmail("test@test.com");
//  //        request.setPassword("wrongPassword");
//  //
//  //        mockMvc.perform(post("/api/auth/login")
//  //                .contentType(MediaType.APPLICATION_JSON)
//  //                .content(objectMapper.writeValueAsString(request)))
//  //                .andExpect(status().isInternalServerError());
//  //    }
// }
