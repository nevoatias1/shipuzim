package com.site.server.gallery;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GalleryController {
    private final GalleryService service;

    public GalleryController(GalleryService service) {
        this.service = service;
    }

    @GetMapping("/api/gallery")
    public List<GalleryItemResponse> list() { return service.list(); }
}
