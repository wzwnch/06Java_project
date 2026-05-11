package com.shortlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortlink.common.result.R;
import com.shortlink.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(classes = ShortLinkApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String TOKEN_HEADER = "Authorization";
    protected static final String TOKEN_PREFIX = "Bearer ";

    protected String generateTestToken(Long userId, String username) {
        return JwtUtils.generateToken(userId, username);
    }

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T parseResponse(MvcResult result, Class<T> clazz) {
        try {
            String content = result.getResponse().getContentAsString();
            R<T> response = objectMapper.readValue(content, 
                objectMapper.getTypeFactory().constructParametricType(R.class, clazz));
            return response.getData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andDo(print());
    }

    protected ResultActions performGetWithToken(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token))
                .andDo(print());
    }

    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType("application/json")
                .content(asJsonString(body)))
                .andDo(print());
    }

    protected ResultActions performPostWithToken(String url, Object body, String token) throws Exception {
        return mockMvc.perform(post(url)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                .contentType("application/json")
                .content(asJsonString(body)))
                .andDo(print());
    }

    protected ResultActions performPutWithToken(String url, Object body, String token) throws Exception {
        return mockMvc.perform(put(url)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                .contentType("application/json")
                .content(asJsonString(body)))
                .andDo(print());
    }

    protected ResultActions performDeleteWithToken(String url, String token) throws Exception {
        return mockMvc.perform(delete(url)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token))
                .andDo(print());
    }
}
