package com.site.server.gallery;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class H2GalleryMigrationTest {
    @TempDir Path database;

    @Test
    void upgradesAnExistingFileDatabaseWithoutLosingRowsOrReusingIds() {
        GalleryMigrationAssertions.verify(new DriverManagerDataSource(
                "jdbc:h2:file:" + database.resolve("existing").toAbsolutePath() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", ""));
    }
}
