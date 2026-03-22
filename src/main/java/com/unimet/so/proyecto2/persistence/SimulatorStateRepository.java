package com.unimet.so.proyecto2.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unimet.so.proyecto2.model.ProjectTypes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimulatorStateRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void saveState(Path path, SimulatorState state) throws IOException {
        Files.writeString(path, gson.toJson(state), StandardCharsets.UTF_8);
    }

    public SimulatorState loadState(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return gson.fromJson(content, SimulatorState.class);
    }

    public TestScenario loadScenario(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        TestScenario scenario = new TestScenario();
        scenario.testId = getString(root, "test_id", "manual");
        scenario.initialHead = root.get("initial_head").getAsInt();

        JsonObject systemFilesObject = root.getAsJsonObject("system_files");
        TestScenario.FileSeed[] seeds = new TestScenario.FileSeed[systemFilesObject.entrySet().size()];
        int seedIndex = 0;
        for (var entry : systemFilesObject.entrySet()) {
            JsonObject fileObject = entry.getValue().getAsJsonObject();
            TestScenario.FileSeed seed = new TestScenario.FileSeed();
            seed.position = Integer.parseInt(entry.getKey());
            seed.name = getString(fileObject, "name", "file_" + seed.position);
            seed.blocks = fileObject.get("blocks").getAsInt();
            seeds[seedIndex++] = seed;
        }
        scenario.systemFiles = seeds;

        JsonElement requestElement = root.get("requests");
        TestScenario.Request[] requests = gson.fromJson(requestElement, TestScenario.Request[].class);
        scenario.requests = requests == null ? new TestScenario.Request[0] : requests;
        return scenario;
    }

    private String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null ? fallback : element.getAsString();
    }

    public ProjectTypes.OperationType mapScenarioOperation(String value) {
        return switch (value == null ? "READ" : value.trim().toUpperCase()) {
            case "UPDATE" -> ProjectTypes.OperationType.UPDATE_NAME;
            case "DELETE" -> ProjectTypes.OperationType.DELETE;
            case "CREATE" -> ProjectTypes.OperationType.CREATE_FILE;
            default -> ProjectTypes.OperationType.READ;
        };
    }
}