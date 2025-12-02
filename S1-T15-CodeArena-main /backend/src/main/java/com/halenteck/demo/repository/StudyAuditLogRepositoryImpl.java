package com.halenteck.demo.repository;

import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudyAuditLogRepositoryImpl implements StudyAuditLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<StudyAuditLogEntity> searchLogs(StudyEntity study,
                                                StudyAuditAction action,
                                                Long actorId,
                                                LocalDateTime from,
                                                LocalDateTime to,
                                                Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudyAuditLogEntity> query = cb.createQuery(StudyAuditLogEntity.class);
        Root<StudyAuditLogEntity> root = query.from(StudyAuditLogEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("study"), study));

        if (action != null) {
            predicates.add(cb.equal(root.get("action"), action));
        }

        if (actorId != null) {
            Path<Object> actorPath = root.get("actor");
            predicates.add(cb.isNotNull(actorPath));
            predicates.add(cb.equal(actorPath.get("id"), actorId));
        }

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<StudyAuditLogEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        return typedQuery.getResultList();
    }
}

