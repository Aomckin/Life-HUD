package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class UploadStorageServiceTest {
    @TempDir Path temp;

    @Test void consolidatesExistingDuplicatesAndRewritesJsonReferences() throws Exception {
        ObjectMapper mapper=new ObjectMapper();JsonFileStore files=new JsonFileStore(mapper,temp);
        Path uploads=temp.resolve("uploads");Files.createDirectories(uploads);byte[] content={9,8,7};
        Files.write(uploads.resolve("a.jpg"),content);Files.write(uploads.resolve("b.png"),content);
        ObjectNode state=mapper.createObjectNode().put("cover","/uploads/b.png");files.write("state.json",state);
        new UploadStorageService(files,mapper).consolidateExisting();
        assertThat(Files.list(uploads).toList()).extracting(v->v.getFileName().toString()).containsExactly("a.jpg");
        assertThat(files.read("state.json").path("cover").asText()).isEqualTo("/uploads/a.jpg");
    }
}
