package com.securepay.modules.news.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.news.entity.News;
import com.securepay.modules.news.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * NewsController — real CRUD over {@link News} (news), ported from
 * app/Modules/News (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class NewsController extends BaseController {

    private final ResponseFactory response;
    private final NewsService service;
    private final ObjectMapper objectMapper;

    public NewsController(ResponseFactory response, NewsService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // POST news/getNews
    @PostMapping("news/getNews")
    public ResponseEntity<Map<String, Object>> getNews() {
        return response.success("News fetched", service.findAll());
    }

    // POST news/createNews
    @PostMapping("news/createNews")
    public ResponseEntity<Map<String, Object>> createNews() {
        News entity = objectMapper.convertValue(payload(), News.class);
        entity.setId(null);
        return response.success("News created", service.save(entity));
    }

    // POST news/deleteNews
    @PostMapping("news/deleteNews")
    public ResponseEntity<Map<String, Object>> deleteNews() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        boolean ok = service.deleteById(Long.valueOf(idObj.toString()));
        return ok ? response.success("News deleted", "")
                : response.error("News not found", "");
    }
}
