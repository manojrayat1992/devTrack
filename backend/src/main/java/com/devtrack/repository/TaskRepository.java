package com.devtrack.repository;

import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.entity.Task;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Optimized Group-by aggregations to eliminate N+1 round-trips
    @Query("SELECT new com.devtrack.dto.StatusCountRecord(t.status, COUNT(t)) FROM Task t WHERE t.ownerId = :ownerId GROUP BY t.status")
    List<StatusCountRecord> countTasksByStatusForOwner(@Param("ownerId") String ownerId);

    @Query("SELECT new com.devtrack.dto.PriorityCountRecord(t.priority, COUNT(t)) FROM Task t WHERE t.ownerId = :ownerId GROUP BY t.priority")
    List<PriorityCountRecord> countTasksByPriorityForOwner(@Param("ownerId") String ownerId);

    // System-wide administrative aggregation methods
    long countByStatus(TaskStatus status);

    long countByPriority(TaskPriority priority);

    @Query("SELECT new com.devtrack.dto.StatusCountRecord(t.status, COUNT(t)) FROM Task t GROUP BY t.status")
    List<StatusCountRecord> countTasksByStatusSystemWide();

    @Query("SELECT new com.devtrack.dto.PriorityCountRecord(t.priority, COUNT(t)) FROM Task t GROUP BY t.priority")
    List<PriorityCountRecord> countTasksByPrioritySystemWide();

    @Query("SELECT COUNT(DISTINCT t.ownerId) FROM Task t")
    long countDistinctOwners();
}
