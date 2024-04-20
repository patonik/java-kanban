package org.my.server;

import com.sun.net.httpserver.HttpServer;
import org.my.manager.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    private final static int PORT = 8080;
    public static final String TASK_PATH = "/tasks";
    public static final String SUBTASK_PATH = "/subtasks";
    public static final String EPIC_PATH = "/epics";
    public static final String HISTORY_PATH = "/history";
    public static final String PRIORITY_PATH = "/prioritized";
    private HttpServer httpServer;
    private final TaskManager taskManager;

    public HttpTaskServer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void start() {
        this.setServer();
        this.setHandlers();
        this.httpServer.start();
    }

    public void stop() {
        this.httpServer.stop(0);
    }

    private void setHandlers() {
        this.httpServer.createContext(TASK_PATH, TaskHandlers.of(TASK_PATH, taskManager));
        this.httpServer.createContext(SUBTASK_PATH, TaskHandlers.of(SUBTASK_PATH, taskManager));
        this.httpServer.createContext(EPIC_PATH, TaskHandlers.of(EPIC_PATH, taskManager));
        this.httpServer.createContext(HISTORY_PATH, TaskHandlers.of(HISTORY_PATH, taskManager));
        this.httpServer.createContext(PRIORITY_PATH, TaskHandlers.of(PRIORITY_PATH, taskManager));
    }

    private void setServer() {
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
