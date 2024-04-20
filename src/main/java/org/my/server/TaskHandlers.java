package org.my.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.my.manager.TaskManager;
import org.my.server.message.Decoder;
import org.my.server.message.Encoder;
import org.my.server.message.Message;
import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TaskHandlers {
    public static final int SUCCESS = 200;
    public static final int SUCCESS_VOID = 201;
    public static final int NOT_FOUND = 404;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int INTERNAL_ERROR = 500;

    public static HttpHandler of(String path, TaskManager taskManager) {
        return switch (path) {
            case HttpTaskServer.TASK_PATH -> new TaskHandler(taskManager);
            case HttpTaskServer.SUBTASK_PATH -> new SubtaskHandler(taskManager);
            case HttpTaskServer.EPIC_PATH -> new EpicHandler(taskManager);
            case HttpTaskServer.HISTORY_PATH -> new HistoryHandler(taskManager);
            case HttpTaskServer.PRIORITY_PATH -> new PriorityHandler(taskManager);
            case null, default -> null;
        };
    }

    private record TaskHandler(TaskManager taskManager) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Decoder<Task> decoder = new Decoder<>(Task.class);
            Encoder<Task> encoder = new Encoder<>();
            switch (method) {
                case "GET" -> {
                    String[] parts = path.split("/");
                    if (parts.length < 3) {
                        exchange.sendResponseHeaders(SUCCESS, 0);
                        Message<Task> message = new Message<>(taskManager.getAllTasks());
                        String payload = encoder.encode(message);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(payload.getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        Optional<Task> taskById = taskManager.getTaskById(parts[2]);
                        if (taskById.isEmpty()) {
                            exchange.sendResponseHeaders(NOT_FOUND, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                        } else {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            Message<Task> message = new Message<>(taskById.get());
                            String payload = encoder.encode(message);
                            try (OutputStream out = exchange.getResponseBody()) {
                                out.write(payload.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
                case "POST" -> {
                    try (InputStream inputStream = exchange.getRequestBody()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Message<Task> message = decoder.decode(json);
                        Task task = message.getContent().getFirst();
                        String id = task.getId();
                        if (id == null) {
                            if (taskManager.createTask(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        } else {
                            if (taskManager.updateTask(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        }
                    }
                }
                case "DELETE" -> {
                    String[] parts = path.split("/");
                    if (parts.length == 3) {
                        if (taskManager.deleteTaskById(parts[2]) != null) {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                        }
                    }
                }
            }
        }
    }

    private record SubtaskHandler(TaskManager taskManager) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Decoder<Subtask> decoder = new Decoder<>(Subtask.class);
            Encoder<Subtask> encoder = new Encoder<>();
            switch (method) {
                case "GET" -> {
                    String[] parts = path.split("/");
                    if (parts.length < 3) {
                        exchange.sendResponseHeaders(SUCCESS, 0);
                        Message<Subtask> message = new Message<>(taskManager.getAllSubtasks());
                        String payload = encoder.encode(message);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(payload.getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        Optional<Subtask> taskById = taskManager.getSubtaskById(parts[2]);
                        if (taskById.isEmpty()) {
                            exchange.sendResponseHeaders(NOT_FOUND, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                        } else {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            Message<Subtask> message = new Message<>(taskById.get());
                            String payload = encoder.encode(message);
                            try (OutputStream out = exchange.getResponseBody()) {
                                out.write(payload.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
                case "POST" -> {
                    try (InputStream inputStream = exchange.getRequestBody()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Message<Subtask> message = decoder.decode(json);
                        Subtask task = message.getContent().getFirst();
                        if (task.getId() == null) {
                            if (taskManager.createSubtask(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        } else {
                            if (taskManager.updateSubtask(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        }
                    }
                }
                case "DELETE" -> {
                    String[] parts = path.split("/");
                    if (parts.length == 3) {
                        if (taskManager.deleteSubtaskById(parts[2]) != null) {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                        }
                    }
                }
            }
        }
    }

    private record EpicHandler(TaskManager taskManager) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Decoder<Epic> decoder = new Decoder<>(Epic.class);
            Encoder<Epic> encoder = new Encoder<>();
            switch (method) {
                case "GET" -> {
                    String[] parts = path.split("/");
                    int length = parts.length;
                    if (length < 3) {
                        exchange.sendResponseHeaders(SUCCESS, 0);
                        Message<Epic> message = new Message<>(taskManager.getAllEpics());
                        String payload = encoder.encode(message);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(payload.getBytes(StandardCharsets.UTF_8));
                        }

                    } else {
                        Optional<Epic> taskById = taskManager.getEpicById(parts[2]);
                        if (taskById.isEmpty()) {
                            exchange.sendResponseHeaders(NOT_FOUND, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                            return;
                        }
                        if (length < 4) {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            Message<Epic> message = new Message<>(taskById.get());
                            String payload = encoder.encode(message);
                            try (OutputStream out = exchange.getResponseBody()) {
                                out.write(payload.getBytes(StandardCharsets.UTF_8));
                            }
                        } else {
                            if (!parts[3].equals("subtasks")) {
                                return;
                            }
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            Encoder<Subtask> subEncoder = new Encoder<>();
                            Message<Subtask> message = new Message<>(taskManager.getSubtasksOfEpic(taskById.get()));
                            String payload = subEncoder.encode(message);
                            try (OutputStream out = exchange.getResponseBody()) {
                                out.write(payload.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
                case "POST" -> {
                    try (InputStream inputStream = exchange.getRequestBody()) {
                        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        Message<Epic> message = decoder.decode(json);
                        Epic task = message.getContent().getFirst();
                        if (task.getId() == null) {
                            if (taskManager.createEpic(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        } else {
                            if (taskManager.updateEpic(task)) {
                                exchange.sendResponseHeaders(SUCCESS_VOID, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            } else {
                                exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
                                try (OutputStream outputStream = exchange.getResponseBody()) {
                                    outputStream.write(0);
                                }
                            }
                        }
                    }
                }
                case "DELETE" -> {
                    String[] parts = path.split("/");
                    if (parts.length == 3) {
                        if (taskManager.deleteEpicById(parts[2]) != null) {
                            exchange.sendResponseHeaders(SUCCESS, 0);
                            try (OutputStream outputStream = exchange.getResponseBody()) {
                                outputStream.write(0);
                            }
                        }
                    }
                }
            }
        }
    }

    private record HistoryHandler(TaskManager taskManager) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            Encoder<Task> encoder = new Encoder<>();
            if (method.equals("GET")) {
                exchange.sendResponseHeaders(SUCCESS, 0);
                Message<Task> message = new Message<>(taskManager.getHistory());
                String payload = encoder.encode(message);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private record PriorityHandler(TaskManager taskManager) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            Encoder<Task> encoder = new Encoder<>();
            if (method.equals("GET")) {
                exchange.sendResponseHeaders(SUCCESS, 0);
                Message<Task> message = new Message<>(taskManager.getPrioritizedTasks());
                String payload = encoder.encode(message);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
