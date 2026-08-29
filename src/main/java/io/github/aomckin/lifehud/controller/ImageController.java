package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.service.ImageStorageService;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** The single upload endpoint for every module's images. */
@RestController @RequestMapping("/api/images")
public final class ImageController {
    private final ImageStorageService images;
    public ImageController(ImageStorageService images){this.images=images;}
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@RequestParam("file") MultipartFile file){
        String path=images.store(file);
        return Map.of("path",path,"url",path);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String,String> tooLarge(){return Map.of("message","文件太大：单张图片不能超过 64MB");}
}
