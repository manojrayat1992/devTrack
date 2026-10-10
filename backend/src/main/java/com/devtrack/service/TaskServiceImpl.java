package com.devtrack.service;

import com.devtrack.dto.PageResponse;
import com.devtrack.dto.TaskCreateRequest;
import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.dto.TaskResponse;
import com.devtrack.dto.TaskStatusUpdateRequest;
import com.devtrack.dto.TaskUpdateRequest;
import com.devtrack.entity.Task;
import com.devtrack.exception.ResourceNotFoundException;
import com.devtrack.exception.TaskOwnershipException;
import com.devtrack.mapper.TaskMapper;
import com.devtrack.repository.TaskRepository;
import com.devtrack.repository.TaskSpecification;
import com.devtrack.security.SecurityUtils;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskStatusTransitionEngine transitionEngine;

    public TaskServiceImpl(TaskRepository taskRepository,
                           TaskMapper taskMapper,
                           TaskStatusTransitionEngine transitionEngine) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.transitionEngine = transitionEngine;
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        String ownerId = SecurityUtils.getCurrentUserId();
        log.info("Creating new task with title '{}' for owner '{}'", request.title(), ownerId);

        Task task = taskMapper.toEntity(request, ownerId);
        Task savedTask = taskRepository.save(task);

        return taskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        Task task = getTaskAndVerifyOwnership(id, currentUserId);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID id, TaskUpdateRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating task '{}' by user '{}'", id, currentUserId);

        Task task = getTaskAndVerifyOwnership(id, currentUserId);
        taskMapper.updateEntity(task, request);
        Task updatedTask = taskRepository.save(task);

        return taskMapper.toResponse(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(UUID id, TaskStatusUpdateRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating status of task '{}' to '{}' by user '{}'", id, request.status(), currentUserId);

        Task task = getTaskAndVerifyOwnership(id, currentUserId);
        transitionEngine.validateTransition(task.getStatus(), request.status());

        task.setStatus(request.status());
        Task updatedTask = taskRepository.save(task);

        return taskMapper.toResponse(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(UUID id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deleting task '{}' by user '{}'", id, currentUserId);

        Task task = getTaskAndVerifyOwnership(id, currentUserId);
        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findTasks(TaskFilterCriteria criteria, Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Fetching paginated tasks for user '{}' with criteria '{}'", currentUserId, criteria);

        Pageable boundedPageable = buildDeterministicPageable(pageable);

        Page<Task> taskPage = taskRepository.findAll(
            TaskSpecification.withFilters(currentUserId, criteria),
            boundedPageable
        );

        return PageResponse.from(taskPage.map(taskMapper::toResponse));
    }

    private Pageable buildDeterministicPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }

        int boundedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort originalSort = pageable.getSort();

        // Enforce deterministic secondary sort by 'id' to guarantee consistency across pagination windows
        Sort deterministicSort;
        boolean hasIdSort = originalSort.stream().anyMatch(order -> order.getProperty().equalsIgnoreCase("id"));
        if (!hasIdSort) {
            deterministicSort = originalSort.and(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            deterministicSort = originalSort;
        }

        return PageRequest.of(pageable.getPageNumber(), boundedSize, deterministicSort);
    }

    private Task getTaskAndVerifyOwnership(UUID id, String currentUserId) {
        return taskRepository.findById(id)
            .map(task -> {
                if (!task.getOwnerId().equals(currentUserId)) {
                    log.warn("User '{}' attempted to access task '{}' owned by '{}'",
                        currentUserId, id, task.getOwnerId());
                    throw new TaskOwnershipException("You are not authorized to access or modify this task");
                }
                return task;
            })
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
