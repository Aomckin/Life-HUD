package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.*;
import java.util.*;
import java.util.function.Predicate;
import org.springframework.stereotype.Repository;

@Repository
public final class MediaRepository {
    private final JsonFileStore files; private final ObjectMapper mapper;
    public MediaRepository(JsonFileStore files, ObjectMapper mapper) { this.files=files; this.mapper=mapper; }
    public synchronized List<Anime> anime(){return read("media-anime.json",new TypeReference<>(){});}
    public synchronized List<AnimeWatchSession> animeSessions(){return read("media-anime-sessions.json",new TypeReference<>(){});}
    public synchronized List<MediaGame> games(){return read("media-games.json",new TypeReference<>(){});}
    public synchronized List<MediaGameSession> gameSessions(){return read("media-game-sessions.json",new TypeReference<>(){});}
    public synchronized List<MediaItem> items(){return read("media-items.json",new TypeReference<>(){});}
    public synchronized void saveAnime(Anime v){save("media-anime.json",anime(),v,Anime::id);}
    public synchronized void saveAnimeSession(AnimeWatchSession v){save("media-anime-sessions.json",animeSessions(),v,AnimeWatchSession::id);}
    public synchronized void saveGame(MediaGame v){save("media-games.json",games(),v,MediaGame::id);}
    public synchronized void saveGameSession(MediaGameSession v){save("media-game-sessions.json",gameSessions(),v,MediaGameSession::id);}
    public synchronized void saveItem(MediaItem v){save("media-items.json",items(),v,MediaItem::id);}
    public synchronized boolean deleteAnime(String id){return delete("media-anime.json",anime(),v->v.id().equals(id));}
    public synchronized boolean deleteAnimeSession(String id){return delete("media-anime-sessions.json",animeSessions(),v->v.id().equals(id));}
    public synchronized boolean deleteGame(String id){return delete("media-games.json",games(),v->v.id().equals(id));}
    public synchronized boolean deleteGameSession(String id){return delete("media-game-sessions.json",gameSessions(),v->v.id().equals(id));}
    public synchronized boolean deleteItem(String id){return delete("media-items.json",items(),v->v.id().equals(id));}
    private <T> List<T> read(String file,TypeReference<List<T>> type){if(!files.exists(file))return List.of();try{return mapper.convertValue(files.read(file),type);}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取媒体档案: "+file,e);}}
    private <T> void save(String file,List<T> values,T value,java.util.function.Function<T,String> id){List<T> copy=new ArrayList<>(values);copy.removeIf(v->id.apply(v).equals(id.apply(value)));copy.add(value);files.write(file,copy);}
    private <T> boolean delete(String file,List<T> values,Predicate<T> test){List<T> copy=new ArrayList<>(values);boolean removed=copy.removeIf(test);if(removed)files.write(file,copy);return removed;}
}
