package com.site.server.gallery;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageSanitizer {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_SIDE = 2000;
    private static final long MAX_SOURCE_PIXELS = 100_000_000;
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    public byte[] sanitize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalidImage();
        if (file.getSize() > MAX_BYTES) throw tooLarge();

        try (InputStream stream = file.getInputStream()) {
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw tooLarge();
            String format = detectFormat(bytes);
            try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalidImage();
                ImageReader reader = readers.next();
                try {
                    if (!reader.getFormatName().toLowerCase(Locale.ROOT).equals(format)) throw invalidImage();
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_SOURCE_PIXELS) {
                        throw new ImageUploadException(HttpStatus.BAD_REQUEST, "IMAGE_DIMENSIONS_TOO_LARGE",
                                "Image dimensions must not exceed 100 million pixels.");
                    }
                    ImageReadParam parameters = reader.getDefaultReadParam();
                    int sample = Math.max(1, Math.max(width, height) / MAX_SIDE);
                    parameters.setSourceSubsampling(sample, sample, 0, 0);
                    BufferedImage decoded = reader.read(0, parameters);
                    if (decoded == null) throw invalidImage();
                    return encodePixels(decoded);
                } finally {
                    reader.dispose();
                }
            }
        } catch (ImageUploadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalidImage();
        }
    }

    private byte[] encodePixels(BufferedImage source) throws IOException {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        // A fresh raster and a fresh PNG writer copy pixels only, never source metadata.
        BufferedImage clean = new BufferedImage(width, height,
                source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = clean.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
            source.flush();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(clean, "png", output)) throw new IOException("PNG encoding unavailable");
            return output.toByteArray();
        } finally {
            clean.flush();
        }
    }

    private String detectFormat(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                && bytes[2] == (byte) 0xff) return "jpeg";
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), PNG_SIGNATURE)) return "png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "webp";
        throw invalidImage();
    }

    private ImageUploadException invalidImage() {
        return new ImageUploadException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE",
                "Upload a valid JPEG, PNG, or WebP image.");
    }

    private ImageUploadException tooLarge() {
        return new ImageUploadException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                "Images must be at most 10 MB.");
    }
}
