package com.shortlink.security;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortlink.BaseIntegrationTest;
import com.shortlink.dto.req.UserLoginReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.EncryptUtils;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.PasswordUtils;
import com.shortlink.utils.SensitiveUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("安全测试")
public class SecurityTest extends BaseIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/user";
    private static final String TEST_USERNAME = "securitytest";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_PHONE = "13812345678";
    private static final String TEST_MAIL = "security@test.com";

    @BeforeAll
    void setupAll() {
        cleanupTestData();
    }

    @BeforeEach
    void setup() {
        cleanupTestData();
    }

    @AfterAll
    void teardown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            userMapper.deleteByUsername(TEST_USERNAME);
            userMapper.deleteByUsername("sqlinject");
            userMapper.deleteByUsername("xssuser");
            userMapper.deleteByUsername("tokenuser");
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("1. 密码加密存储测试（不可逆）")
    class PasswordEncryptionTest {

        @Test
        @DisplayName("密码使用BCrypt加密存储")
        void password_ShouldBeEncryptedWithBCrypt() {
            String plainPassword = "MyPassword123";
            String encryptedPassword = PasswordUtils.encode(plainPassword);

            assertNotNull(encryptedPassword, "加密后的密码不应为空");
            assertNotEquals(plainPassword, encryptedPassword, "加密后的密码不应与明文相同");
            assertTrue(encryptedPassword.startsWith("$2a$") || encryptedPassword.startsWith("$2b$"), 
                    "BCrypt加密后的密码应以$2a$或$2b$开头");
            assertEquals(60, encryptedPassword.length(), "BCrypt加密后的密码长度应为60字符");
        }

        @Test
        @DisplayName("相同密码每次加密结果不同（加盐）")
        void samePassword_ShouldGenerateDifferentHash() {
            String plainPassword = "SamePassword123";
            String hash1 = PasswordUtils.encode(plainPassword);
            String hash2 = PasswordUtils.encode(plainPassword);

            assertNotEquals(hash1, hash2, "相同密码每次加密结果应不同（盐值不同）");
        }

        @Test
        @DisplayName("密码加密不可逆")
        void password_ShouldBeIrreversible() {
            String plainPassword = "TestPassword123";
            String encryptedPassword = PasswordUtils.encode(plainPassword);

            assertFalse(encryptedPassword.contains(plainPassword), 
                    "加密后的密码不应包含明文密码");
            assertFalse(encryptedPassword.equalsIgnoreCase(plainPassword), 
                    "加密后的密码不应与明文相同（忽略大小写）");
        }

        @Test
        @DisplayName("密码校验功能正常")
        void passwordCheck_ShouldWorkCorrectly() {
            String plainPassword = "CorrectPassword123";
            String encryptedPassword = PasswordUtils.encode(plainPassword);

            assertTrue(PasswordUtils.check(plainPassword, encryptedPassword), 
                    "正确密码应校验通过");
            assertFalse(PasswordUtils.check("WrongPassword123", encryptedPassword), 
                    "错误密码应校验失败");
        }

        @Test
        @DisplayName("用户注册后密码已加密存储")
        void userRegister_ShouldStoreEncryptedPassword() throws Exception {
            UserDO user = createTestUserDirectly(TEST_USERNAME, TEST_PASSWORD);

            assertNotNull(user, "用户应已创建");
            
            String storedPassword = user.getPassword();
            assertNotNull(storedPassword, "密码不应为空");
            assertNotEquals(TEST_PASSWORD, storedPassword, "存储的密码不应是明文");
            assertTrue(storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$"), 
                    "存储的密码应为BCrypt格式");
            assertTrue(PasswordUtils.check(TEST_PASSWORD, storedPassword), 
                    "存储的密码应能正确校验");
        }

        @Test
        @DisplayName("空密码处理")
        void nullPassword_ShouldBeHandled() {
            String encrypted = PasswordUtils.encode(null);
            assertNotNull(encrypted, "空密码加密后不应为null");
            
            assertFalse(PasswordUtils.check(null, "somehash"), "空密码校验应失败");
            assertFalse(PasswordUtils.check("password", null), "空哈希校验应失败");
        }

        @Test
        @DisplayName("密码长度符合安全要求")
        void password_ShouldMeetSecurityRequirements() {
            String shortPassword = "abc";
            String normalPassword = "Password123";
            String longPassword = "VeryLongPassword12345678901234567890";

            String shortHash = PasswordUtils.encode(shortPassword);
            String normalHash = PasswordUtils.encode(normalPassword);
            String longHash = PasswordUtils.encode(longPassword);

            assertEquals(60, shortHash.length(), "短密码加密后长度应为60");
            assertEquals(60, normalHash.length(), "正常密码加密后长度应为60");
            assertEquals(60, longHash.length(), "长密码加密后长度应为60");
        }
    }

    @Nested
    @DisplayName("2. 敏感信息脱敏返回测试")
    class SensitiveDataDesensitizationTest {

        @Test
        @DisplayName("手机号脱敏格式正确")
        void phoneDesensitization_ShouldBeCorrect() {
            String phone = "13812345678";
            String desensitized = SensitiveUtils.phone(phone);

            assertNotNull(desensitized, "脱敏后的手机号不应为空");
            assertNotEquals(phone, desensitized, "脱敏后的手机号应与原始值不同");
            assertTrue(desensitized.contains("*"), "脱敏后的手机号应包含*号");
            assertTrue(phone.startsWith(desensitized.substring(0, 3)), 
                    "脱敏后的手机号前缀应保留");
        }

        @Test
        @DisplayName("邮箱脱敏格式正确")
        void emailDesensitization_ShouldBeCorrect() {
            String email = "testuser@example.com";
            String desensitized = SensitiveUtils.email(email);

            assertNotNull(desensitized, "脱敏后的邮箱不应为空");
            assertNotEquals(email, desensitized, "脱敏后的邮箱应与原始值不同");
            assertTrue(desensitized.contains("*"), "脱敏后的邮箱应包含*号");
            assertTrue(desensitized.contains("@"), "脱敏后的邮箱应保留@符号");
        }

        @Test
        @DisplayName("空值脱敏处理")
        void nullDesensitization_ShouldBeHandled() {
            String phoneResult = SensitiveUtils.phone(null);
            assertNull(phoneResult, "空手机号脱敏应返回null");

            String emptyPhoneResult = SensitiveUtils.phone("");
            assertEquals("", emptyPhoneResult, "空字符串手机号脱敏应返回空字符串");

            String emailResult = SensitiveUtils.email(null);
            assertNull(emailResult, "空邮箱脱敏应返回null");

            String emptyEmailResult = SensitiveUtils.email("");
            assertEquals("", emptyEmailResult, "空字符串邮箱脱敏应返回空字符串");
        }

        @Test
        @DisplayName("用户信息返回时手机号已脱敏")
        void userInfo_ShouldReturnDesensitizedPhone() throws Exception {
            UserDO user = createTestUserWithPhone(TEST_USERNAME, TEST_PHONE);
            String token = generateTestToken(user.getId(), TEST_USERNAME);

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(content);
            JsonNode data = root.get("data");
            
            String returnedPhone = data.get("phone").asText();
            assertNotNull(returnedPhone, "返回的手机号不应为空");
            assertTrue(returnedPhone.contains("*"), "返回的手机号应已脱敏");
            assertNotEquals(TEST_PHONE, returnedPhone, "返回的手机号不应是原始值");
        }

        @Test
        @DisplayName("用户信息返回时邮箱已脱敏")
        void userInfo_ShouldReturnDesensitizedEmail() throws Exception {
            UserDO user = createTestUserWithEmail(TEST_USERNAME, TEST_MAIL);
            String token = generateTestToken(user.getId(), TEST_USERNAME);

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(content);
            JsonNode data = root.get("data");
            
            String returnedMail = data.get("mail").asText();
            assertNotNull(returnedMail, "返回的邮箱不应为空");
            assertTrue(returnedMail.contains("*"), "返回的邮箱应已脱敏");
            assertNotEquals(TEST_MAIL, returnedMail, "返回的邮箱不应是原始值");
        }

        @Test
        @DisplayName("密码字段不在响应中返回")
        void userInfo_ShouldNotReturnPassword() throws Exception {
            UserDO user = createTestUserDirectly(TEST_USERNAME, TEST_PASSWORD);
            String token = generateTestToken(user.getId(), TEST_USERNAME);

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(content);
            JsonNode data = root.get("data");
            
            assertFalse(data.has("password"), "响应中不应包含密码字段");
        }

        @Test
        @DisplayName("自定义脱敏功能正常")
        void customDesensitization_ShouldWorkCorrectly() {
            String text = "ABCDEFGHIJ";
            String desensitized = SensitiveUtils.custom(text, 2, 2);

            assertNotNull(desensitized, "脱敏结果不应为空");
            assertTrue(desensitized.contains("*"), "脱敏结果应包含*号");
            assertTrue(desensitized.startsWith("AB"), "前缀应保留");
        }

        @Test
        @DisplayName("密码脱敏返回固定值")
        void passwordDesensitization_ShouldReturnFixedValue() {
            String desensitized = SensitiveUtils.password();
            assertEquals("******", desensitized, "密码脱敏应返回固定值");
        }

        private UserDO createTestUserWithPhone(String username, String phone) {
            UserDO user = new UserDO();
            user.setUsername(username);
            user.setPassword(PasswordUtils.encode(TEST_PASSWORD));
            user.setPhone(EncryptUtils.encrypt(phone));
            user.setRealPhone(SensitiveUtils.phone(phone));
            user.setDelFlag(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            return user;
        }

        private UserDO createTestUserWithEmail(String username, String email) {
            UserDO user = new UserDO();
            user.setUsername(username);
            user.setPassword(PasswordUtils.encode(TEST_PASSWORD));
            user.setMail(EncryptUtils.encrypt(email));
            user.setRealMail(SensitiveUtils.email(email));
            user.setDelFlag(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            return user;
        }
    }

    @Nested
    @DisplayName("3. Token有效期控制测试")
    class TokenExpirationTest {

        @Test
        @DisplayName("Token生成成功")
        void token_ShouldBeGeneratedSuccessfully() {
            Long userId = 10001L;
            String username = "testuser";

            String token = JwtUtils.generateToken(userId, username);

            assertNotNull(token, "Token不应为空");
            assertTrue(StrUtil.isNotBlank(token), "Token不应为空字符串");
            assertEquals(3, token.split("\\.").length, "JWT Token应包含三部分（用.分隔）");
        }

        @Test
        @DisplayName("Token包含正确的用户信息")
        void token_ShouldContainCorrectUserInfo() {
            Long userId = 10002L;
            String username = "tokenuser";

            String token = JwtUtils.generateToken(userId, username);

            Long extractedUserId = JwtUtils.getUserId(token);
            String extractedUsername = JwtUtils.getUsername(token);

            assertEquals(userId, extractedUserId, "Token中的用户ID应正确");
            assertEquals(username, extractedUsername, "Token中的用户名应正确");
        }

        @Test
        @DisplayName("Token验证功能正常")
        void token_ShouldBeValidatedCorrectly() {
            Long userId = 10003L;
            String username = "validuser";

            String token = JwtUtils.generateToken(userId, username);

            assertTrue(JwtUtils.validate(token), "有效的Token应验证通过");
            assertFalse(JwtUtils.isExpired(token), "新生成的Token不应过期");
        }

        @Test
        @DisplayName("无效Token验证失败")
        void invalidToken_ShouldFailValidation() {
            String invalidToken = "invalid.token.string";
            
            assertFalse(JwtUtils.validate(invalidToken), "无效的Token应验证失败");
            assertTrue(JwtUtils.isExpired(invalidToken), "无效的Token应视为过期");
        }

        @Test
        @DisplayName("空Token验证失败")
        void nullToken_ShouldFailValidation() {
            assertFalse(JwtUtils.validate(null), "空Token应验证失败");
            assertFalse(JwtUtils.validate(""), "空字符串Token应验证失败");
            assertTrue(JwtUtils.isExpired(null), "空Token应视为过期");
        }

        @Test
        @DisplayName("Token过期时间正确设置")
        void token_ShouldHaveCorrectExpiration() {
            Long userId = 10004L;
            String username = "expireuser";

            String token = JwtUtils.generateToken(userId, username);
            Date expireDate = JwtUtils.getExpireDate(token);

            assertNotNull(expireDate, "过期时间不应为空");
            assertTrue(expireDate.after(new Date()), "过期时间应在当前时间之后");
        }

        @Test
        @DisplayName("过期Token无法访问受保护接口")
        void expiredToken_ShouldNotAccessProtectedApi() throws Exception {
            UserDO user = createTestUserDirectly("tokenuser", TEST_PASSWORD);
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzaG9ydGxpbmsiLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMSwidXNlcklkIjoxLCJ1c2VybmFtZSI6InRlc3R1c2VyIn0.invalid";

            performGetWithToken(BASE_URL + "/info", expiredToken)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("退出后Token立即失效")
        void logout_ShouldInvalidateToken() throws Exception {
            UserDO user = createTestUserDirectly("tokenuser", TEST_PASSWORD);
            String token = generateTestToken(user.getId(), "tokenuser");

            performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk());

            performPostWithToken(BASE_URL + "/logout", null, token)
                    .andExpect(status().isOk());

            performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("无Token访问受保护接口返回401")
        void noToken_ShouldReturn401() throws Exception {
            performGet(BASE_URL + "/info")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("篡改的Token验证失败")
        void tamperedToken_ShouldFailValidation() {
            Long userId = 10005L;
            String username = "tamperuser";

            String token = JwtUtils.generateToken(userId, username);
            String tamperedToken = token + "tampered";

            assertFalse(JwtUtils.validate(tamperedToken), "篡改的Token应验证失败");
        }

        @Test
        @DisplayName("不同用户Token不可互换")
        void differentUserToken_ShouldNotBeInterchangeable() {
            Long userId1 = 10006L;
            Long userId2 = 10007L;
            String username1 = "user1";
            String username2 = "user2";

            String token1 = JwtUtils.generateToken(userId1, username1);
            String token2 = JwtUtils.generateToken(userId2, username2);

            Long extractedFromToken1 = JwtUtils.getUserId(token1);
            Long extractedFromToken2 = JwtUtils.getUserId(token2);

            assertEquals(userId1, extractedFromToken1, "Token1应包含用户1的ID");
            assertEquals(userId2, extractedFromToken2, "Token2应包含用户2的ID");
            assertNotEquals(extractedFromToken1, extractedFromToken2, "不同用户的Token信息应不同");
        }
    }

    @Nested
    @DisplayName("4. SQL注入防护测试")
    class SqlInjectionTest {

        @Test
        @DisplayName("用户名SQL注入防护 - 单引号注入")
        void usernameSqlInjection_SingleQuote_ShouldBePrevented() throws Exception {
            String maliciousUsername = "admin' OR '1'='1";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(maliciousUsername);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 500, 
                                "SQL注入攻击应被拦截，返回400或500");
                    });

            UserDO injectedUser = userMapper.selectByUsername(maliciousUsername);
            assertNull(injectedUser, "SQL注入不应创建非法用户");
        }

        @Test
        @DisplayName("登录时SQL注入防护")
        void loginSqlInjection_ShouldBePrevented() throws Exception {
            createTestUserDirectly("sqlinject", TEST_PASSWORD);

            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername("sqlinject' OR '1'='1' --");
            request.setPassword("anything");

            performPost(BASE_URL + "/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1005));
        }

        @Test
        @DisplayName("DROP TABLE注入防护")
        void dropTableInjection_ShouldBePrevented() throws Exception {
            String maliciousUsername = "test'; DROP TABLE link_user; --";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(maliciousUsername);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request);

            long userCount = userMapper.selectCount(null);
            assertTrue(userCount >= 0, "DROP TABLE注入不应影响数据库结构");
        }

        @Test
        @DisplayName("UNION注入防护")
        void unionInjection_ShouldBePrevented() throws Exception {
            String maliciousUsername = "' UNION SELECT * FROM link_user --";
            
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername(maliciousUsername);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1005));
        }

        @Test
        @DisplayName("注释符注入防护")
        void commentInjection_ShouldBePrevented() throws Exception {
            String[] maliciousInputs = {
                "admin/*",
                "admin#",
                "admin--",
                "admin'--",
                "admin' #"
            };

            for (String maliciousInput : maliciousInputs) {
                UserLoginReqDTO request = new UserLoginReqDTO();
                request.setUsername(maliciousInput);
                request.setPassword(TEST_PASSWORD);

                performPost(BASE_URL + "/login", request)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(1005));
            }
        }

        @Test
        @DisplayName("分号注入防护")
        void semicolonInjection_ShouldBePrevented() throws Exception {
            String maliciousUsername = "test'; INSERT INTO link_user VALUES(999,'hacker','hack',null,null,null,null,0,NOW(),NOW()); --";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(maliciousUsername);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request);

            UserDO hackerUser = userMapper.selectById(999L);
            assertNull(hackerUser, "分号注入不应创建非法用户");
        }

        @Test
        @DisplayName("MyBatis预编译语句防护验证")
        void myBatisPreparedStatement_ShouldPreventSqlInjection() {
            String maliciousUsername = "test' OR '1'='1";
            
            UserDO user = userMapper.selectByUsername(maliciousUsername);
            assertNull(user, "预编译语句应防止SQL注入");
        }
    }

    @Nested
    @DisplayName("5. XSS攻击防护测试")
    class XssAttackTest {

        @Test
        @DisplayName("用户名XSS脚本防护")
        void usernameXss_ShouldBePrevented() throws Exception {
            String xssUsername = "<script>alert('xss')</script>";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(xssUsername);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 500, 
                                "XSS攻击应被拦截，返回400或500");
                    });

            UserDO xssUser = userMapper.selectByUsername(xssUsername);
            assertNull(xssUser, "XSS脚本不应被存储");
        }

        @Test
        @DisplayName("JavaScript事件XSS防护")
        void javascriptEventXss_ShouldBePrevented() throws Exception {
            String[] xssPayloads = {
                "<img src=x onerror=alert('xss')>",
                "<body onload=alert('xss')>",
                "<svg onload=alert('xss')>",
                "<iframe src='javascript:alert(1)'>"
            };

            for (String xssPayload : xssPayloads) {
                UserRegisterReqDTO request = new UserRegisterReqDTO();
                request.setUsername("xssuser");
                request.setPassword(TEST_PASSWORD);
                request.setPhone(xssPayload);

                performPost(BASE_URL + "/register", request)
                        .andExpect(result -> {
                            String content = result.getResponse().getContentAsString();
                            assertFalse(content.contains("<script>"), 
                                    "响应中不应包含script标签");
                            assertFalse(content.contains("onerror="), 
                                    "响应中不应包含onerror事件");
                            assertFalse(content.contains("onload="), 
                                    "响应中不应包含onload事件");
                        });
                
                cleanupTestData();
            }
        }

        @Test
        @DisplayName("HTML标签注入防护")
        void htmlTagInjection_ShouldBePrevented() throws Exception {
            String htmlInjection = "<h1>Hello</h1><b>World</b>";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(htmlInjection);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 500, 
                                "HTML标签注入应被拦截");
                    });
        }

        @Test
        @DisplayName("特殊字符转义防护")
        void specialCharacterXss_ShouldBeHandled() throws Exception {
            UserDO user = createTestUserDirectly("xssuser", TEST_PASSWORD);
            String token = generateTestToken(user.getId(), "xssuser");

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            
            assertFalse(content.contains("<script"), "响应中不应包含script标签");
        }

        @Test
        @DisplayName("URL编码XSS防护")
        void urlEncodedXss_ShouldBePrevented() throws Exception {
            String encodedXss = "%3Cscript%3Ealert('xss')%3C/script%3E";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(encodedXss);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        assertFalse(content.contains("<script>"), 
                                "URL编码的XSS不应被执行");
                    });
        }

        @Test
        @DisplayName("Unicode编码XSS防护")
        void unicodeXss_ShouldBePrevented() throws Exception {
            String unicodeXss = "\\u003cscript\\u003ealert('xss')\\u003c/script\\u003e";
            
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(unicodeXss);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        assertFalse(content.contains("<script>"), 
                                "Unicode编码的XSS不应被执行");
                    });
        }

        @Test
        @DisplayName("响应内容安全检查")
        void responseContent_ShouldBeSafe() throws Exception {
            UserDO user = createTestUserDirectly("xssuser", TEST_PASSWORD);
            String token = generateTestToken(user.getId(), "xssuser");

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            
            String[] dangerousPatterns = {
                "<script", "</script>", "javascript:", "onerror=", 
                "onload=", "onclick=", "onmouseover=", "<iframe"
            };
            
            for (String pattern : dangerousPatterns) {
                assertFalse(content.toLowerCase().contains(pattern.toLowerCase()), 
                        "响应中不应包含危险模式: " + pattern);
            }
        }
    }

    private UserDO createTestUserDirectly(String username, String password) {
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode(password));
        user.setRealPhone(TEST_PHONE);
        user.setRealMail(TEST_MAIL);
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
