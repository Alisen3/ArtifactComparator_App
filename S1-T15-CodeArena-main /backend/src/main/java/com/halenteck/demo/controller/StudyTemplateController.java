package com.halenteck.demo.controller;

import com.halenteck.demo.dto.CreateStudyTemplateRequest;
import com.halenteck.demo.dto.InstantiateTemplateRequest;
import com.halenteck.demo.dto.StudyTemplateDTO;
import com.halenteck.demo.dto.UpdateStudyTemplateRequest;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyTemplateEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.StudyRepository;
import com.halenteck.demo.repository.StudyTemplateRepository;
import com.halenteck.demo.repository.UserRepository;
import com.halenteck.demo.service.StudyCloneService;
import com.halenteck.demo.service.StudyTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study-templates")
public class StudyTemplateController {

    private final StudyTemplateRepository studyTemplateRepository;
    private final StudyTemplateService studyTemplateService;
    private final StudyCloneService studyCloneService;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public StudyTemplateController(StudyTemplateRepository studyTemplateRepository,
                                   StudyTemplateService studyTemplateService,
                                   StudyCloneService studyCloneService,
                                   StudyRepository studyRepository,
                                   UserRepository userRepository) {
        this.studyTemplateRepository = studyTemplateRepository;
        this.studyTemplateService = studyTemplateService;
        this.studyCloneService = studyCloneService;
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<StudyTemplateDTO>> listTemplates(Principal principal) {
        UserEntity owner = getCurrentUser(principal);
        List<StudyTemplateDTO> templates = studyTemplateService.listTemplates(owner)
                .stream()
                .map(this::convertTemplate)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @PostMapping
    public ResponseEntity<StudyTemplateDTO> createTemplate(@RequestBody CreateStudyTemplateRequest request,
                                                           Principal principal) {
        UserEntity owner = getCurrentUser(principal);
        if (request.studyId() == null) {
            throw new IllegalArgumentException("studyId is required.");
        }
        StudyEntity study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new RuntimeException("Study not found."));
        if (!study.getCreator().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Only study creators can create templates from their studies.");
        }
        StudyTemplateEntity template = studyTemplateService.createTemplate(
                study,
                owner,
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(convertTemplate(template));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<StudyTemplateDTO> updateTemplate(@PathVariable Long templateId,
                                                           @RequestBody UpdateStudyTemplateRequest request,
                                                           Principal principal) {
        UserEntity owner = getCurrentUser(principal);
        StudyTemplateEntity template = studyTemplateRepository.findByIdAndOwner(templateId, owner)
                .orElseThrow(() -> new RuntimeException("Template not found."));
        StudyTemplateEntity updated = studyTemplateService.updateTemplate(template, request.name(), request.description());
        return ResponseEntity.ok(convertTemplate(updated));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long templateId,
                                            Principal principal) {
        UserEntity owner = getCurrentUser(principal);
        StudyTemplateEntity template = studyTemplateRepository.findByIdAndOwner(templateId, owner)
                .orElseThrow(() -> new RuntimeException("Template not found."));
        studyTemplateService.deleteTemplate(template);
        return ResponseEntity.ok(Map.of("message", "Template deleted."));
    }

    @PostMapping("/{templateId}/instantiate")
    public ResponseEntity<?> instantiateTemplate(@PathVariable Long templateId,
                                                 @RequestBody InstantiateTemplateRequest request,
                                                 Principal principal) {
        UserEntity owner = getCurrentUser(principal);
        StudyTemplateEntity template = studyTemplateRepository.findByIdAndOwner(templateId, owner)
                .orElseThrow(() -> new RuntimeException("Template not found."));
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : template.getName() + " Copy";
        StudyEntity created = studyCloneService.createFromTemplate(template, owner, title, request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Study created from template.",
                "studyId", created.getId()
        ));
    }

    private UserEntity getCurrentUser(Principal principal) {
        return userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private StudyTemplateDTO convertTemplate(StudyTemplateEntity template) {
        return new StudyTemplateDTO(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getUpdatedAt(),
                template.getLastUsedAt()
        );
    }
}

