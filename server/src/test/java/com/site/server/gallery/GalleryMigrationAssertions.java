package com.site.server.gallery;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the same populated V4-to-V5 upgrade checks on H2 and PostgreSQL. */
public final class GalleryMigrationAssertions {
    private GalleryMigrationAssertions() { }

    public static void verify(DataSource dataSource) {
        String table = "\"gallery_upgrade\".gallery_item";
        var jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).schemas("gallery_upgrade").defaultSchema("gallery_upgrade")
                .target("4").load().migrate();
        jdbc.execute("INSERT INTO " + table + """
                 (id, image_url, storage_key, caption, category, sort_order, created_at) VALUES
                 (7, '/uploads/old.png', 'old-key', 'Old caption', 'CLADDING', 4, TIMESTAMP '2026-01-01 12:00:00'),
                 (41, '/uploads/paint.png', 'paint-key', NULL, 'PAINTING', -2, TIMESTAMP '2026-02-01 12:00:00')
                """);
        var before = jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id");
        var flyway = Flyway.configure().dataSource(dataSource).schemas("gallery_upgrade")
                .defaultSchema("gallery_upgrade").target("5").load();
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        assertThat(jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")).isEqualTo(before);
        for (GalleryCategory category : GalleryCategory.values()) {
            jdbc.update("INSERT INTO " + table + " (image_url, storage_key, category, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    "/uploads/new.png", category.name(), category.name());
            assertThat(jdbc.queryForObject("SELECT id FROM " + table + " WHERE storage_key = ?", Long.class, category.name()))
                    .isGreaterThan(41L);
        }
        assertThatThrownBy(() -> jdbc.update("UPDATE " + table + " SET category = 'ROOFING' WHERE id = 7"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE " + table + " SET storage_key = 'old-key' WHERE id = 41"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForList("SELECT id FROM " + table + " ORDER BY sort_order, created_at DESC, id DESC").getFirst())
                .containsValue(41L);
        Boolean hasSortIndex = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
            try (var indexes = connection.getMetaData().getIndexInfo(null, "gallery_upgrade", "gallery_item", false, false)) {
                while (indexes.next()) {
                    if ("gallery_item_sort_idx".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
                }
                return false;
            }
        });
        assertThat(hasSortIndex).isTrue();
    }
}
