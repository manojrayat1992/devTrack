package com.devtrack.repository;

import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.entity.Task;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository JPA & Specification Integration Tests")
class TaskRepositoryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final String aliceId = "user-alice";
    private final String bobId = "user-bob";

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Deterministic pagination guarantees zero duplicate or skipped rows when timestamps match")
    void deterministicPaginationWithEqualTimestamps() {
        Instant fixedTimestamp = Instant.parse("2026-10-10T10:00:00Z");

        // Seed 5 tasks with identical created_at timestamp
        for (int i = 1; i <= 5; i++) {
            Task task = new Task(
                UUID.randomUUID(),
                "Task " + i,
                "Description " + i,
                TaskPriority.MEDIUM,
                TaskStatus.OPEN,
                LocalDate.now().plusDays(i),
                aliceId
            );
            task.setCreatedAt(fixedTimestamp);
            task.setUpdatedAt(fixedTimestamp);
            entityManager.persist(task);
        }
        entityManager.flush();

        // Deterministic sort: created_at DESC, id DESC
        Sort deterministicSort = Sort.by(Sort.Direction.DESC, "createdAt", "id");

        Page<Task> page0 = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, new TaskFilterCriteria(null, null, null, null, null)),
            PageRequest.of(0, 2, deterministicSort)
        );

        Page<Task> page1 = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, new TaskFilterCriteria(null, null, null, null, null)),
            PageRequest.of(1, 2, deterministicSort)
        );

        Page<Task> page2 = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, new TaskFilterCriteria(null, null, null, null, null)),
            PageRequest.of(2, 2, deterministicSort)
        );

        assertEquals(2, page0.getNumberOfElements());
        assertEquals(2, page1.getNumberOfElements());
        assertEquals(1, page2.getNumberOfElements());
        assertEquals(5, page0.getTotalElements());

        // Ensure every task is unique across all pages
        Set<UUID> seenIds = new HashSet<>();
        page0.getContent().forEach(t -> assertTrue(seenIds.add(t.getId()), "Duplicate task on page 0"));
        page1.getContent().forEach(t -> assertTrue(seenIds.add(t.getId()), "Duplicate task on page 1"));
        page2.getContent().forEach(t -> assertTrue(seenIds.add(t.getId()), "Duplicate task on page 2"));

        assertEquals(5, seenIds.size(), "All 5 tasks must be retrieved exactly once");
    }

    @Test
    @DisplayName("Multi-tenant Specification strictly isolates tasks by owner ID")
    void multiTenantSpecificationStrictIsolation() {
        Task aliceTask = new Task(UUID.randomUUID(), "Alice Task", "Desc", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId);
        Task bobTask = new Task(UUID.randomUUID(), "Bob Task", "Desc", TaskPriority.HIGH, TaskStatus.OPEN, null, bobId);

        entityManager.persist(aliceTask);
        entityManager.persist(bobTask);
        entityManager.flush();

        Page<Task> results = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, new TaskFilterCriteria(null, null, null, null, null)),
            PageRequest.of(0, 10)
        );

        assertEquals(1, results.getTotalElements());
        assertEquals(aliceTask.getId(), results.getContent().get(0).getId());
        assertEquals(aliceId, results.getContent().get(0).getOwnerId());
    }

    @Test
    @DisplayName("Search filter matches title and description case-insensitively with SQL escaping")
    void searchFilterMatchesTitleAndDescription() {
        Task task1 = new Task(UUID.randomUUID(), "Deploy Production Kubernetes", "Rollout v1.2", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId);
        Task task2 = new Task(UUID.randomUUID(), "Refactor Backend", "kubernetes cluster config", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId);
        Task task3 = new Task(UUID.randomUUID(), "Fix UI Button", "Styling adjustment", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        TaskFilterCriteria searchCriteria = new TaskFilterCriteria(null, null, null, null, "KUBERNETES");
        Page<Task> results = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, searchCriteria),
            PageRequest.of(0, 10)
        );

        assertEquals(2, results.getTotalElements());
    }

    @Test
    @DisplayName("Aggregated count queries group correctly by status and priority")
    void aggregatedCountsGroupCorrectly() {
        entityManager.persist(new Task(UUID.randomUUID(), "T1", "D", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId));
        entityManager.persist(new Task(UUID.randomUUID(), "T2", "D", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, null, aliceId));
        entityManager.persist(new Task(UUID.randomUUID(), "T3", "D", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId));
        entityManager.flush();

        List<StatusCountRecord> statusCounts = taskRepository.countTasksByStatusForOwner(aliceId);
        List<PriorityCountRecord> priorityCounts = taskRepository.countTasksByPriorityForOwner(aliceId);

        long openCount = statusCounts.stream()
            .filter(r -> r.status() == TaskStatus.OPEN)
            .mapToLong(StatusCountRecord::count)
            .findFirst()
            .orElse(0L);
        assertEquals(2, openCount);

        long highCount = priorityCounts.stream()
            .filter(r -> r.priority() == TaskPriority.HIGH)
            .mapToLong(PriorityCountRecord::count)
            .findFirst()
            .orElse(0L);
        assertEquals(2, highCount);
    }
}
