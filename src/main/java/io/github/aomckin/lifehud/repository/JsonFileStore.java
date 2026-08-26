package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Generic JSON file access. It knows nothing about game domain objects. */
public class JsonFileStore {
    private final ObjectMapper mapper;
    private final Path root;

    public JsonFileStore(ObjectMapper mapper, Path root) {
        this.mapper = mapper;
        this.root = root;
    }

    public JsonNode read(String name) {
        try { return mapper.readTree(resolve(name).toFile()); }
        catch (IOException e) { throw new IllegalStateException("无法读取数据文件: " + name, e); }
    }
    public boolean exists(String name) { return Files.exists(resolve(name)); }
    public Path resolve(String name) { return root.resolve(name).normalize(); }

    public synchronized void write(String name, Object value) {
        try {
            Path target = resolve(name);
            Path temp = Files.createTempFile(root, name + ".", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), value);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { throw new IllegalStateException("无法写入数据文件: " + name, e); }
    }

    public synchronized void appendLine(String name, String line) {
        try {
            Files.writeString(resolve(name), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { throw new IllegalStateException("无法写入文件: " + name, e); }
    }

    public List<String> readLines(String name) {
        if (!exists(name)) return List.of();
        try { return Files.readAllLines(resolve(name), StandardCharsets.UTF_8); }
        catch (IOException e) { throw new IllegalStateException("无法读取文件: " + name, e); }
    }

    protected ObjectMapper mapper() { return mapper; }
    protected Path root() { return root; }
}
