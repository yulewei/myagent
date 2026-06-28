package ai.myagent.controller;

import ai.myagent.model.vo.*;
import ai.myagent.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpHeaders.EXPIRES;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Slf4j
@RestController
@RequestMapping("api/knowledge")
@Tag(name = "REST API")
public class KnowledgeController {
    @Resource
    private KnowledgeService knowledgeService;

    @GetMapping("{knowledgeId}")
    @Operation(summary = "查询知识库详情")
    public KnowledgeResp queryKnowledge(@PathVariable String knowledgeId) {
        return knowledgeService.queryKnowledge(knowledgeId);
    }

    @PostMapping("new")
    @Operation(summary = "新建知识库")
    public String newKnowledge(@RequestBody @Valid KnowledgeNewReq request) {
        return knowledgeService.newKnowledge(request);
    }

    @PostMapping("update")
    @Operation(summary = "修改知识库")
    public void updateKnowledge(@RequestBody @Valid KnowledgeUpdateReq request) {
        knowledgeService.updateKnowledge(request);
    }

    @PostMapping("{knowledgeId}/delete")
    @Operation(summary = "删除知识库")
    public void deleteKnowledge(@PathVariable String knowledgeId) {
        knowledgeService.deleteKnowledge(knowledgeId);
    }

    @PostMapping("{knowledgeId}/text/upload")
    @Operation(summary = "上传文本到知识库")
    public String uploadDocText(@PathVariable String knowledgeId,
                                @RequestBody KnowledgeDocTextReq req) {
        return knowledgeService.uploadDocText(knowledgeId, req.getContent());
    }

    @SneakyThrows
    @PostMapping(value = "{knowledgeId}/file/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "上传文件到知识库")
    public String uploadDocFile(@PathVariable String knowledgeId,
                                @RequestParam MultipartFile file) {
        return knowledgeService.uploadDocFile(knowledgeId, file.getBytes(), file.getOriginalFilename());
    }

    @GetMapping("{knowledgeId}/doc/list")
    @Operation(summary = "查询知识库文档列表（按创建时间倒序）")
    public List<KnowledgeDocResp> queryDocList(@PathVariable String knowledgeId) {
        return knowledgeService.queryDocList(knowledgeId);
    }

    @GetMapping("{knowledgeId}/doc/{docId}")
    @Operation(summary = "查询知识库文档详情")
    public KnowledgeDocResp queryDoc(@PathVariable String knowledgeId, @PathVariable String docId) {
       return knowledgeService.queryDoc(knowledgeId, docId);
    }

    @PostMapping("{knowledgeId}/doc/embed/{docId}")
    @Operation(summary = "向量化知识库文档（若已经向量化，则重新计算）")
    public void embedDoc(@PathVariable String knowledgeId, @PathVariable String docId) {
        knowledgeService.embedDoc(knowledgeId, docId);
    }

    @PostMapping("{knowledgeId}/doc/delete/{docId}")
    @Operation(summary = "删除知识库文档")
    public void deleteDoc(@PathVariable String knowledgeId, @PathVariable String docId) {
        knowledgeService.deleteDoc(knowledgeId, docId);
    }

    @SneakyThrows
    @GetMapping("file/download/{fileKey}")
    @Operation(summary = "下载知识库文件")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileKey) {
        byte[] bytes = knowledgeService.downloadDocFile(fileKey);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileKey);
        headers.add(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(PRAGMA, "no-cache");
        headers.add(EXPIRES, "0");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
