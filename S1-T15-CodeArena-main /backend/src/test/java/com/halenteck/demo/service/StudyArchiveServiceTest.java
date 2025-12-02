package com.halenteck.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.StudyVersionEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.StudyAuditLogRepository;
import com.halenteck.demo.repository.StudyVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyArchiveServiceTest {

    @Mock
    private StudyVersionRepository studyVersionRepository;
    @Mock
    private StudyAuditLogRepository studyAuditLogRepository;

    @InjectMocks
    private StudyArchiveService studyArchiveService;

    private StudyEntity study;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        UserEntity owner = new UserEntity("Owner", "owner@example.com", "pw", null);
        setId(owner, 1L);
        study = new StudyEntity("Archive Study", "Desc", false, owner);
        setStudyId(study, 10L);
        study.setStatus(StudyStatus.PUBLISHED);
    }

    @Test
    void buildArchiveIncludesManifestAndFiles() throws Exception {
        StudyVersionEntity version = new StudyVersionEntity(study, 1, "{\"study\":{\"title\":\"Archive Study\"}}", LocalDateTime.now());
        setId(version, 200L);
        setVersionCreatedAt(version);
        StudyAuditLogEntity audit = new StudyAuditLogEntity(study, null, StudyAuditAction.STUDY_CREATED, "{\"hello\":\"world\"}");
        setAuditId(audit, 300L);

        when(studyVersionRepository.findAllByStudyOrderByVersionNumberDesc(study)).thenReturn(List.of(version));
        when(studyAuditLogRepository.findAllByStudyOrderByCreatedAtDesc(study)).thenReturn(List.of(audit));

        byte[] archive = studyArchiveService.buildArchive(study);
        Map<String, byte[]> contents = unzip(archive);

        assertTrue(contents.containsKey("manifest.json"));
        assertTrue(contents.containsKey("versions.json"));
        assertTrue(contents.containsKey("audit-log.json"));

        JsonNode manifest = objectMapper.readTree(contents.get("manifest.json"));
        assertEquals(study.getId().longValue(), manifest.get("study").get("id").asLong());
        assertEquals("versions.json", manifest.get("files").fieldNames().next());
    }

    private Map<String, byte[]> unzip(byte[] archive) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                files.put(entry.getName(), zis.readAllBytes());
            }
        }
        return files;
    }

    private void setId(StudyVersionEntity entity, Long id) throws Exception {
        Field field = StudyVersionEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private void setAuditId(StudyAuditLogEntity entity, Long id) throws Exception {
        Field field = StudyAuditLogEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        Field createdField = StudyAuditLogEntity.class.getDeclaredField("createdAt");
        createdField.setAccessible(true);
        createdField.set(entity, LocalDateTime.now());
    }

    private void setVersionCreatedAt(StudyVersionEntity entity) throws Exception {
        Field field = StudyVersionEntity.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(entity, LocalDateTime.now());
    }

    private void setStudyId(StudyEntity entity, Long id) throws Exception {
        Field field = StudyEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private void setId(UserEntity user, Long id) throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}

