package com.devtrack.repository;

import com.devtrack.entity.Task;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndOwnerId(UUID id, String ownerId);

    boolean existsByIdAndOwnerId(UUID id, String ownerId);

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, TaskStatus status);

    long countByOwnerIdAndPriority(String ownerId, TaskPriority priority);

    long countByOwnerIdAndDueDate(String ownerId, LocalDate dueDate);

    long countByOwnerIdAndDueDateBeforeAndStatusNot(String ownerId, LocalDate date, TaskStatus status);

    // System-wide administrative aggregation methods
    long countByStatus(TaskStatus status);

    long countByPriority(TaskPriority priority);

    @Query("SELECT COUNT(DISTINCT t.ownerId) FROM Task t")
    long countDistinctOwners();
}
