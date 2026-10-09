package com.devtrack.repository;

import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.entity.Task;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> withFilters(String ownerId, TaskFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly filter by owner ID to guarantee multi-tenant security
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (criteria.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), criteria.priority()));
            }

            if (criteria.dueBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), criteria.dueBefore()));
            }

            if (criteria.dueAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueAfter()));
            }

            if (criteria.search() != null && !criteria.search().trim().isEmpty()) {
                String searchPattern = "%" + criteria.search().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
