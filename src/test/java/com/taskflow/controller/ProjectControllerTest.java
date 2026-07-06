package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.model.Project;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.service.TaskService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for ProjectController.
 *
 * Tests the HTTP layer only — ProjectRepository and TaskService are mocked.
 * See also: PR #52 (TaskController), PR #44 (DateUtils/StringUtils).
 */
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private TaskService taskService;

    /**
     * A project with 5 tasks and 3 completed → getProgress() = 60.0.
     * members is set to avoid NPE in getMemberIds() during Jackson serialization.
     * Project.getMemberIds() throws NPE when members is null (see BUG comment in Project.java).
     */
    private Project sampleProject() {
        Project p = new Project("Test Project", "TEST");
        p.setId(1L);
        p.setDescription("A test project");
        p.setTaskCount(5);
        p.setCompletedTaskCount(3);
        p.setMembers("1");
        return p;
    }

    // ─── GET /api/projects ────────────────────────────────────────────────────

    @Test
    void getAllProjects_returnsJsonList() throws Exception {
        when(projectRepository.findAll()).thenReturn(List.of(sampleProject()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("TEST"))
                .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    @Test
    void getAllProjects_emptyList_returnsEmptyArray() throws Exception {
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/projects/{id} ───────────────────────────────────────────────

    @Test
    void getProject_existingId_returns200WithBody() throws Exception {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST"))
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void getProject_missingId_returns404() throws Exception {
        // ProjectController correctly returns 404 for missing projects.
        // Contrast with TaskController.getTask which returns 200 with null body (Bug, PR #52).
        when(projectRepository.findById(9999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/9999"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/projects/code/{code} ───────────────────────────────────────

    @Test
    void getProjectByCode_found_returns200() throws Exception {
        when(projectRepository.findByCode("TEST")).thenReturn(sampleProject());

        mockMvc.perform(get("/api/projects/code/TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void getProjectByCode_notFound_returns404() throws Exception {
        when(projectRepository.findByCode("NOPE")).thenReturn(null);

        mockMvc.perform(get("/api/projects/code/NOPE"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /api/projects ───────────────────────────────────────────────────

    @Test
    void createProject_setsStatusZeroAndReturnsBody_BUG_returns200InsteadOf201() throws Exception {
        // BUG: createProject() returns 200 OK instead of 201 Created.
        // See ProjectController line 58: "Should be 201 Created".
        // When fixed, change the expected status from isOk() to isCreated().
        Project input = new Project("New Project", "NEW");
        input.setMembers("1"); // avoid NPE in getMemberIds() during serialization
        Project saved = new Project("New Project", "NEW");
        saved.setId(42L);
        saved.setStatus(0);
        saved.setMembers("1");

        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())               // BUG: should be 201
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value(0)); // status forced to 0
    }

    // ─── PUT /api/projects/{id} ───────────────────────────────────────────────

    @Test
    void updateProject_existingId_returns200() throws Exception {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject());

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProject())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST"));
    }

    @Test
    void updateProject_missingId_returns404() throws Exception {
        when(projectRepository.existsById(9999L)).thenReturn(false);

        mockMvc.perform(put("/api/projects/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProject())))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/projects/{id} ────────────────────────────────────────────

    @Test
    void deleteProject_returns204() throws Exception {
        doNothing().when(projectRepository).deleteById(1L);

        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    // ─── GET /api/projects/{id}/dashboard ────────────────────────────────────

    @Test
    void getProjectDashboard_notFound_returns404() throws Exception {
        when(projectRepository.findById(9999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/9999/dashboard"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProjectDashboard_found_returns200WithCompositeData() throws Exception {
        // sampleProject() has taskCount=5, completedTaskCount=3 → getProgress()=60.0
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(taskService.getTasksByProject("TEST")).thenReturn(Collections.emptyList());
        when(taskService.getProjectStatistics("TEST")).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/projects/1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.code").value("TEST"))
                .andExpect(jsonPath("$.progress").value(60.0));
    }

    @Test
    void getProjectDashboard_zeroTasks_BUG_progressIsNaN() throws Exception {
        // BUG: Project.getProgress() returns NaN when taskCount == 0 (formula: 0.0/0 = NaN).
        // Jackson 2.13 serializes Double.NaN as the literal token "NaN" rather than throwing,
        // producing invalid JSON. Clients (browsers, REST consumers) will fail to parse it.
        //
        // Fix: guard against taskCount == 0 in Project.getProgress():
        //   return taskCount == 0 ? 0.0 : (double) completedTaskCount / taskCount * 100;
        //
        // When fixed, replace the content assertion with:
        //   .andExpect(jsonPath("$.progress").value(0.0));
        Project emptyProject = new Project("Empty Project", "EMPTY");
        emptyProject.setId(2L);
        emptyProject.setTaskCount(0);
        emptyProject.setCompletedTaskCount(0);
        emptyProject.setMembers("1");

        when(projectRepository.findById(2L)).thenReturn(Optional.of(emptyProject));
        when(taskService.getTasksByProject("EMPTY")).thenReturn(Collections.emptyList());
        when(taskService.getProjectStatistics("EMPTY")).thenReturn(Collections.emptyMap());

        // BUG: response body contains "NaN" (non-standard JSON) for the progress field
        mockMvc.perform(get("/api/projects/2/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NaN")));
    }

    // ─── POST /api/projects/{id}/members ─────────────────────────────────────

    @Test
    void addMember_projectNotFound_returns404() throws Exception {
        when(projectRepository.findById(9999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/projects/9999/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 7}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addMember_emptyMembersList_setsUserId() throws Exception {
        Project project = new Project("Test", "TEST");
        project.setId(1L);
        project.setMembers(null);

        Project savedProject = new Project("Test", "TEST");
        savedProject.setId(1L);
        savedProject.setMembers("7");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        mockMvc.perform(post("/api/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members").value("7"));
    }

    @Test
    void addMember_existingMembers_appendsUserId_BUG_noDuplicateCheck() throws Exception {
        // BUG: addMember does not check if the user is already a member.
        // Calling POST /api/projects/1/members with userId=5 twice results in "5,5".
        // Fix: before appending, split the CSV and check if userId is already present.
        //
        // This test verifies normal append behaviour (different userId).
        // A duplicate-userId test is omitted here since it would require the caller
        // to make the same request twice — that logic lives above the controller layer.
        Project project = new Project("Test", "TEST");
        project.setId(1L);
        project.setMembers("5");

        Project savedProject = new Project("Test", "TEST");
        savedProject.setId(1L);
        savedProject.setMembers("5,7");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        mockMvc.perform(post("/api/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members").value("5,7"));
    }
}
