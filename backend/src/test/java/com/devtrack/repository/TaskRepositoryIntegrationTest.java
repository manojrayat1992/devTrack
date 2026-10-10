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
    @DisplayName("Deterministic pagination guarantees zero duplicate or skipped rows and strict ID order when timestamps match")
    void deterministicPaginationWithEqualTimestamps() {
        Instant fixedTimestamp = Instant.parse("2026-10-10T10:00:00Z");

        List<Task> createdTasks = new java.util.ArrayList<>();
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
            createdTasks.add(task);
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

        // Expected order: since createdAt is identical, 'id' DESC strictly dictates the deterministic order (lexical descending in SQL)
        List<UUID> expectedSortedIds = createdTasks.stream()
            .map(Task::getId)
            .sorted(java.util.Comparator.comparing(UUID::toString).reversed())
            .toList();

        assertEquals(expectedSortedIds.subList(0, 2), page0.getContent().stream().map(Task::getId).toList(),
            "Page 0 must match the first 2 tasks in descending ID order");
        assertEquals(expectedSortedIds.subList(2, 4), page1.getContent().stream().map(Task::getId).toList(),
            "Page 1 must match the next 2 tasks in descending ID order");
        assertEquals(expectedSortedIds.subList(4, 5), page2.getContent().stream().map(Task::getId).toList(),
            "Page 2 must match the final task in descending ID order");
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
    @DisplayName("Search filter matches title and description case-insensitively with SQL wildcard escaping")
    void searchFilterMatchesTitleAndDescriptionWithSqlWildcardEscaping() {
        Task taskPercent = new Task(UUID.randomUUID(), "Deploy 100% Complete", "Milestone reached", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId);
        Task taskNoPercent = new Task(UUID.randomUUID(), "Deploy 1000 Items", "Inventory rollout", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId);
        Task taskUnderscore = new Task(UUID.randomUUID(), "Fix task_worker issue", "Worker thread bug", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId);
        Task taskNoUnderscore = new Task(UUID.randomUUID(), "Fix taskAworker issue", "Another thread issue", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId);

        entityManager.persist(taskPercent);
        entityManager.persist(taskNoPercent);
        entityManager.persist(taskUnderscore);
        entityManager.persist(taskNoUnderscore);
        entityManager.flush();

        // 1. Literal '%' search should only match "Deploy 100% Complete", not "Deploy 1000 Items"
        TaskFilterCriteria percentCriteria = new TaskFilterCriteria(null, null, null, null, "100%");
        Page<Task> percentResults = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, percentCriteria),
            PageRequest.of(0, 10)
        );
        assertEquals(1, percentResults.getTotalElements(), "Only literal '%' containing task must match");
        assertEquals(taskPercent.getId(), percentResults.getContent().get(0).getId());

        // 2. Literal '_' search should only match "Fix task_worker issue", not "Fix taskAworker issue"
        TaskFilterCriteria underscoreCriteria = new TaskFilterCriteria(null, null, null, null, "task_worker");
        Page<Task> underscoreResults = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, underscoreCriteria),
            PageRequest.of(0, 10)
        );
        assertEquals(1, underscoreResults.getTotalElements(), "Only literal '_' containing task must match");
        assertEquals(taskUnderscore.getId(), underscoreResults.getContent().get(0).getId());

        // 3. Case-insensitive description match
        TaskFilterCriteria descCriteria = new TaskFilterCriteria(null, null, null, null, "WORKER THREAD");
        Page<Task> descResults = taskRepository.findAll(
            TaskSpecification.withFilters(aliceId, descCriteria),
            PageRequest.of(0, 10)
        );
        assertEquals(1, descResults.getTotalElements());
        assertEquals(taskUnderscore.getId(), descResults.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Aggregated count queries group correctly by status and priority with strict tenant isolation")
    void aggregatedCountsGroupCorrectly() {
        entityManager.persist(new Task(UUID.randomUUID(), "T1", "D", TaskPriority.HIGH, TaskStatus.OPEN, null, aliceId));
        entityManager.persist(new Task(UUID.randomUUID(), "T2", "D", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, null, aliceId));
        entityManager.persist(new Task(UUID.randomUUID(), "T3", "D", TaskPriority.LOW, TaskStatus.OPEN, null, aliceId));

        // Seed Bob's tasks with identical status and priority to prove tenant isolation
        entityManager.persist(new Task(UUID.randomUUID(), "Bob-T1", "D", TaskPriority.HIGH, TaskStatus.OPEN, null, bobId));
        entityManager.persist(new Task(UUID.randomUUID(), "Bob-T2", "D", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, null, bobId));
        entityManager.flush();

        List<StatusCountRecord> statusCounts = taskRepository.countTasksByStatusForOwner(aliceId);
        List<PriorityCountRecord> priorityCounts = taskRepository.countTasksByPriorityForOwner(aliceId);

        long openCount = statusCounts.stream()
            .filter(r -> r.status() == TaskStatus.OPEN)
            .mapToLong(StatusCountRecord::count)
            .findFirst()
            .orElse(0L);
        assertEquals(2, openCount, "Alice's OPEN count must isolate Bob's OPEN tasks");

        long highCount = priorityCounts.stream()
            .filter(r -> r.priority() == TaskPriority.HIGH)
            .mapToLong(PriorityCountRecord::count)
            .findFirst()
            .orElse(0L);
        assertEquals(2, highCount, "Alice's HIGH count must isolate Bob's HIGH tasks");

        // Verify Bob's aggregates are separate
        List<StatusCountRecord> bobStatusCounts = taskRepository.countTasksByStatusForOwner(bobId);
        long bobOpenCount = bobStatusCounts.stream()
            .filter(r -> r.status() == TaskStatus.OPEN)
            .mapToLong(StatusCountRecord::count)
            .findFirst()
            .orElse(0L);
        assertEquals(1, bobOpenCount, "Bob has exactly 1 OPEN task");
    }
}
