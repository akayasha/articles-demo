package com.example.articlesdemo.controller;

import com.example.articlesdemo.dto.request.ArticleRequest;
import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.exception.GlobalExceptionHandler;
import com.example.articlesdemo.kafka.ArticleProducer;
import com.example.articlesdemo.service.ArticleService;
import com.example.articlesdemo.service.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleProducer articleProducer;

    @InjectMocks
    private ArticleController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getByTitleReturnsArticle() throws Exception {

        Article article = Article.builder()
                .id(1L)
                .title("hello")
                .description("world")
                .build();

        when(articleService.getByTitle("hello")).thenReturn(article);

        mockMvc.perform(get("/api/articles")
                        .param("title", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("hello"));
    }

    @Test
    void getByTitleNotFoundReturns404() throws Exception {

        when(articleService.getByTitle("missing"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/articles")
                        .param("title", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postQueuesRequest() throws Exception {

        ArticleRequest request = ArticleRequest.builder()
                .title("t")
                .description("d")
                .build();

        // IMPORTANT: mock duplicate check
        when(articleProducer.isDuplicateTitle("t")).thenReturn(false);

        doNothing().when(articleProducer).publish(any(ArticleRequest.class));

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Article queued for processing"));
    }

    @Test
    void postDuplicateReturns409() throws Exception {

        ArticleRequest request = ArticleRequest.builder()
                .title("t")
                .description("d")
                .build();

        when(articleProducer.isDuplicateTitle("t")).thenReturn(true);

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}
