package cn.lineai.mvp.agent;

import cn.lineai.model.AgentTaskRecord;
import cn.lineai.model.AgentTaskState;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AgentRuntimeStateTest {
    @Test
    public void stateMachineSeparatesRecoverableAndTerminalStates() {
        assertTrue(AgentTaskState.QUEUED.canStart());
        assertTrue(AgentTaskState.RECOVERABLE.canStart());
        assertTrue(AgentTaskState.PAUSED.canStart());
        assertTrue(AgentTaskState.RUNNING.isInFlight());
        assertFalse(AgentTaskState.RUNNING.isTerminal());
        assertTrue(AgentTaskState.SUCCEEDED.isTerminal());
        assertEquals(AgentTaskState.FAILED, AgentTaskState.fromStorage("unknown"));
    }

    @Test
    public void taskRecordKeepsCheckpointAndBudgetMetadata() {
        AgentTaskRecord task = new AgentTaskRecord(
                "task-1", "conversation-1", "", "/workspace",
                "message-1", "inspect", "", "model-1",
                AgentTaskState.RUNNING, 42, 7, 20,
                "{\"phase\":\"tools\"}", "", 1,
                1L, 2L, 1L, 0L, 100L
        );
        assertEquals("task-1", task.getId());
        assertEquals(AgentTaskState.RUNNING, task.getState());
        assertEquals(42, task.getGenerationId());
        assertEquals(7, task.getToolCallCount());
        assertEquals(20, task.getMaxToolCalls());
        assertEquals("{\"phase\":\"tools\"}", task.getCheckpointJson());
        assertEquals(100L, task.getDeadlineAt());
    }
}
