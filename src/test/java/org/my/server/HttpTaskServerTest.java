package org.my.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.my.manager.Managers;
import org.my.manager.TestInputValues;
import org.my.server.message.Decoder;
import org.my.server.message.Encoder;
import org.my.server.message.Message;
import org.my.task.Epic;
import org.my.task.Subtask;
import org.my.task.Task;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpTaskServerTest {
    private static final String ADDRESS = "http://127.0.0.1:8080";
    private HttpTaskServer httpTaskServer;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpTaskServer = new HttpTaskServer(Managers.getDefault());
        httpTaskServer.start();
        httpClient = HttpClient.newBuilder().build();
    }

    @AfterEach
    void tearDown() {
        httpTaskServer.stop();
        httpClient.close();
    }

    @Test
    void getTasks() throws IOException, InterruptedException {
        // get empty payload
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        assertEquals("[]", payload);
        // post task and get payload
        Encoder<Task> encoder = new Encoder<>();
        Task task = new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(), null,
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst());
        Message<Task> message = new Message<>(task);
        String encoded = encoder.encode(message);
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(encoded);
        int status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Task> decoder = new Decoder<>(Task.class);
        Task received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        task.setId(id);
        assertEquals(task, received);
        // get by id
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        received = decoder.decode(payload).getContent().getFirst();
        assertEquals(task, received);
    }

    @Test
    void getEpics() throws IOException, InterruptedException {
        // get empty payload
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        assertEquals("[]", payload);
        // post task and get payload
        Encoder<Epic> encoder = new Encoder<>();
        Epic task = new Epic(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(), null);
        Message<Epic> message = new Message<>(task);
        String encoded = encoder.encode(message);
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(encoded);
        int status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Epic> decoder = new Decoder<>(Epic.class);
        Epic received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        task.setId(id);
        assertEquals(task, received);
        // get by id
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        received = decoder.decode(payload).getContent().getFirst();
        assertEquals(task, received);
    }

    @Test
    void getSubtasks() throws IOException, InterruptedException {
        // get empty payload
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        assertEquals("[]", payload);
        // post task and get payload
        Encoder<Subtask> encoder = new Encoder<>();
        String id = getIdOfCreatedEpic(bodyHandler);
        HttpRequest.BodyPublisher bodyPublisher;
        int status;
        Subtask task = new Subtask(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(), null,
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst(), id);
        Message<Subtask> subtaskMessagemessage = new Message<>(task);
        String subEncoded = encoder.encode(subtaskMessagemessage);
        bodyPublisher = HttpRequest.BodyPublishers.ofString(subEncoded);
        status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Subtask> subtaskDecoder = new Decoder<>(Subtask.class);
        Subtask subtask = subtaskDecoder.decode(payload).getContent().getFirst();
        id = subtask.getId();
        task.setId(id);
        assertEquals(task, subtask);
        // get by id
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        subtask = subtaskDecoder.decode(payload).getContent().getFirst();
        assertEquals(task, subtask);
    }

    private String getIdOfCreatedEpic(HttpResponse.BodyHandler<String> bodyHandler) throws IOException, InterruptedException {
        String payload;
        Encoder<Epic> epicEncoder = new Encoder<>();
        Epic epic = new Epic(TestInputValues.LEVEL_1_NAMES.getLast(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getLast(), null);
        Message<Epic> message = new Message<>(epic);
        String encoded = epicEncoder.encode(message);
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(encoded);
        int status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Epic> decoder = new Decoder<>(Epic.class);
        Epic received = decoder.decode(payload).getContent().getFirst();
        return received.getId();
    }

    @Test
    void postTasks() throws IOException, InterruptedException {
        // post task and get payload
        Encoder<Task> encoder = new Encoder<>();
        Task task = new Task(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(), null,
                TestInputValues.LEVEL_1_DURATION.getFirst(),
                TestInputValues.LEVEL_1_START_DATE_TIMES.getFirst());
        Message<Task> message = new Message<>(task);
        String encoded = encoder.encode(message);
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(encoded);
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        int status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Task> decoder = new Decoder<>(Task.class);
        Task received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        task.setId(id);
        assertEquals(task, received);
    }

    @Test
    void postEpics() throws IOException, InterruptedException {
        // post task and get payload
        Encoder<Epic> encoder = new Encoder<>();
        Epic task = new Epic(TestInputValues.LEVEL_1_NAMES.getFirst(),
                TestInputValues.LEVEL_1_DESCRIPTIONS.getFirst(), null);
        Message<Epic> message = new Message<>(task);
        String encoded = encoder.encode(message);
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(encoded);
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        int status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Epic> decoder = new Decoder<>(Epic.class);
        Epic received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        task.setId(id);
        assertEquals(task, received);
    }

    @Test
    void postSubtasks() throws IOException, InterruptedException {
        // post task and get payload
        Encoder<Subtask> encoder = new Encoder<>();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String id = getIdOfCreatedEpic(bodyHandler);
        HttpRequest.BodyPublisher bodyPublisher;
        int status;
        Subtask task = new Subtask(TestInputValues.LEVEL_2_NAMES.getFirst().getLast(),
                TestInputValues.LEVEL_2_DESCRIPTIONS.getFirst().getLast(), null,
                TestInputValues.LEVEL_2_DURATION.getFirst().getLast(),
                TestInputValues.LEVEL_2_START_DATE_TIMES.getFirst().getLast(), id);
        Message<Subtask> subtaskMessagemessage = new Message<>(task);
        String subEncoded = encoder.encode(subtaskMessagemessage);
        bodyPublisher = HttpRequest.BodyPublishers.ofString(subEncoded);
        status = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .POST(bodyPublisher)
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS_VOID, status);
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Subtask> subtaskDecoder = new Decoder<>(Subtask.class);
        Subtask subtask = subtaskDecoder.decode(payload).getContent().getFirst();
        id = subtask.getId();
        task.setId(id);
        assertEquals(task, subtask);
    }

    @Test
    void deleteTasks() throws IOException, InterruptedException {
        postTasks();
        // get by id
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Task> decoder = new Decoder<>(Task.class);
        Task received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        int statusCode = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .DELETE()
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS, statusCode);
    }

    @Test
    void deleteEpics() throws IOException, InterruptedException {
        postEpics();
        // get by id
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Epic> decoder = new Decoder<>(Epic.class);
        Epic received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        int statusCode = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.EPIC_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .DELETE()
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS, statusCode);
    }

    @Test
    void deleteSubtasks() throws IOException, InterruptedException {
        postSubtasks();
        // get by id
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Subtask> decoder = new Decoder<>(Subtask.class);
        Subtask received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        int statusCode = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.SUBTASK_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .DELETE()
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS, statusCode);
    }

    @Test
    void gethistory() throws IOException, InterruptedException {
        postTasks();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        String payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        Decoder<Task> decoder = new Decoder<>(Task.class);
        Task received = decoder.decode(payload).getContent().getFirst();
        String id = received.getId();
        httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.TASK_PATH + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                        .GET()
                        .build(),
                bodyHandler
        );
        payload = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.HISTORY_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).body();
        received = decoder.decode(payload).getContent().getFirst();
        assertEquals(id, received.getId());
    }

    @Test
    void getPriority() throws IOException, InterruptedException {
        postTasks();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
        int statusCode = httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create(ADDRESS + HttpTaskServer.PRIORITY_PATH))
                        .GET()
                        .build(),
                bodyHandler
        ).statusCode();
        assertEquals(TaskHandlers.SUCCESS, statusCode);
    }
}