package com.halenteck.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyVersionEntity;
import com.halenteck.demo.repository.StudyAuditLogRepository;
import com.halenteck.demo.repository.StudyVersionRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StudyArchiveService {

    private final StudyVersionRepository studyVersionRepository;
    private final StudyAuditLogRepository studyAuditLogRepository;
    private final ObjectMapper objectMapper;

    public StudyArchiveService(StudyVersionRepository studyVersionRepository,
                               StudyAuditLogRepository studyAuditLogRepository) {
        this.studyVersionRepository = studyVersionRepository;
        this.studyAuditLogRepository = studyAuditLogRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public byte[] buildArchive(StudyEntity study) {
        try {
            List<StudyVersionEntity> versions = studyVersionRepository.findAllByStudyOrderByVersionNumberDesc(study);
            List<StudyAuditLogEntity> auditLogs = studyAuditLogRepository.findAllByStudyOrderByCreatedAtDesc(study);

            byte[] versionsBytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(versions.stream().map(this::convertVersion).toList());
            byte[] auditBytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(auditLogs.stream().map(this::convertAuditLog).toList());
            byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(buildManifest(study, versionsBytes, auditBytes));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                addEntry(zos, "versions.json", versionsBytes);
                addEntry(zos, "audit-log.json", auditBytes);
                addEntry(zos, "manifest.json", manifestBytes);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build study archive", e);
        }
    }

    private Map<String, Object> convertVersion(StudyVersionEntity version) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("versionNumber", version.getVersionNumber());
        payload.put("createdAt", version.getCreatedAt() != null ? version.getCreatedAt().toString() : null);
        payload.put("publishedAt", version.getPublishedAt() != null ? version.getPublishedAt().toString() : null);
        payload.put("config", parseJson(version.getConfigJson()));
        return payload;
    }

    private Map<String, Object> convertAuditLog(StudyAuditLogEntity log) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", log.getId());
        payload.put("action", log.getAction());
        payload.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        if (log.getActor() != null) {
            payload.put("actorId", log.getActor().getId());
            payload.put("actorName", log.getActor().getName());
        }
        payload.put("details", parseJson(log.getDetailsJson()));
        return payload;
    }

    private Map<String, Object> buildManifest(StudyEntity study, byte[] versionsBytes, byte[] auditBytes) {
        ObjectNode files = objectMapper.createObjectNode();
        files.set("versions.json", buildFileNode(versionsBytes));
        files.set("audit-log.json", buildFileNode(auditBytes));

        Map<String, Object> studyMetadata = new HashMap<>();
        studyMetadata.put("id", study.getId());
        studyMetadata.put("title", study.getTitle());
        studyMetadata.put("description", study.getDescription());
        studyMetadata.put("status", study.getStatus());
        studyMetadata.put("latestPublishedVersion", study.getLatestPublishedVersionNumber());

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("study", studyMetadata);
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("files", parseJson(files.toString()));
        return manifest;
    }

    private ObjectNode buildFileNode(byte[] content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("bytes", content.length);
        node.put("sha256", sha256(content));
        return node;
    }

    private void addEntry(ZipOutputStream zos, String path, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private JsonNode parseJson(String json) {
        if (json == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            return objectMapper.createObjectNode().put("unparseable", true);
        }
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to compute SHA-256", e);
        }
    }
}

