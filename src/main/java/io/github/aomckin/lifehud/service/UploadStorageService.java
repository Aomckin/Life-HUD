package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Content-addressed guard for every file stored in data/uploads. */
@Service
public final class UploadStorageService {
    private static final String PREFIX="/uploads/";
    private final JsonFileStore files;private final ObjectMapper mapper;
    public UploadStorageService(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}

    public synchronized String store(byte[] content,String extension) throws IOException {
        Path directory=directory();String hash=hash(content);
        try(Stream<Path> paths=Files.list(directory)){
            Optional<Path> existing=paths.filter(Files::isRegularFile).filter(v->same(v,content.length,hash)).findFirst();
            if(existing.isPresent())return PREFIX+existing.get().getFileName();
        }
        Path target=directory.resolve(UUID.randomUUID()+extension).normalize();
        if(!target.startsWith(directory))throw new IOException("非法的存储路径");
        Files.write(target,content,StandardOpenOption.CREATE_NEW);return PREFIX+target.getFileName();
    }

    public Path resolvePublic(String path){return directoryUnchecked().resolve(path.substring(PREFIX.length())).normalize();}

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void consolidateExisting(){
        try{
            Path directory=directory();Map<String,Path> kept=new LinkedHashMap<>();Map<String,String> replacements=new LinkedHashMap<>();
            try(Stream<Path> paths=Files.list(directory)){
                for(Path path:paths.filter(Files::isRegularFile).sorted().toList()){
                    String key=Files.size(path)+":"+hash(Files.readAllBytes(path));Path first=kept.putIfAbsent(key,path);
                    if(first!=null)replacements.put(PREFIX+path.getFileName(),PREFIX+first.getFileName());
                }
            }
            if(replacements.isEmpty())return;
            Path root=files.resolve(".");
            try(Stream<Path> paths=Files.list(root)){
                for(Path json:paths.filter(v->Files.isRegularFile(v)&&v.getFileName().toString().endsWith(".json")).toList()){
                    JsonNode tree=mapper.readTree(json.toFile());if(replace(tree,replacements))files.write(json.getFileName().toString(),tree);
                }
            }
            for(String duplicate:replacements.keySet())Files.deleteIfExists(resolvePublic(duplicate));
        }catch(IOException e){throw new IllegalStateException("无法整理 uploads 重复文件",e);}
    }

    private boolean replace(JsonNode node,Map<String,String> replacements){
        boolean changed=false;
        if(node instanceof ObjectNode object){for(var fields=object.fields();fields.hasNext();){var field=fields.next();JsonNode value=field.getValue();if(value.isTextual()&&replacements.containsKey(value.asText())){object.put(field.getKey(),replacements.get(value.asText()));changed=true;}else changed|=replace(value,replacements);}}
        else if(node instanceof ArrayNode array){for(int i=0;i<array.size();i++){JsonNode value=array.get(i);if(value.isTextual()&&replacements.containsKey(value.asText())){array.set(i,mapper.getNodeFactory().textNode(replacements.get(value.asText())));changed=true;}else changed|=replace(value,replacements);}}
        return changed;
    }
    private boolean same(Path path,int size,String hash){try{return Files.size(path)==size&&hash(Files.readAllBytes(path)).equals(hash);}catch(IOException e){return false;}}
    private String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException("SHA-256 不可用",e);}}
    private Path directory() throws IOException{Path value=directoryUnchecked();Files.createDirectories(value);return value;}
    private Path directoryUnchecked(){return files.resolve("uploads").toAbsolutePath().normalize();}
}
