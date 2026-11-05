package com.site.server.serviceimage;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ServiceImageController {
    private final ServiceImageService service;

    public ServiceImageController(ServiceImageService service) { this.service = service; }

    @GetMapping("/api/service-images")
    public Map<ServiceKey, String> list() { return service.list(); }

    @PutMapping(value = "/admin/service-images/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> put(@PathVariable String key,
                                   @RequestParam(required = false) MultipartFile file) {
        return service.put(parse(key), file);
    }

    @DeleteMapping("/admin/service-images/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        service.delete(parse(key));
        return ResponseEntity.noContent().build();
    }

    private ServiceKey parse(String key) {
        try {
            return ServiceKey.valueOf(key);
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
