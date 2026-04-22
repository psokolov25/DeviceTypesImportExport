package ru.aritmos.dtt.demo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что в директории исходного Java-кода demo-service отсутствуют
 * бинарные и временные файлы, не предназначенные для хранения в VCS.
 */
class SourceTreeHygieneTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    @Test
    void sourceTreeMustNotContainBinaryOrTemporaryArtifacts() throws IOException {
        if (!Files.exists(SOURCE_ROOT)) {
            return;
        }

        final List<String> forbiddenPaths;
        try (Stream<Path> pathStream = Files.walk(SOURCE_ROOT)) {
            forbiddenPaths = pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isForbiddenArtifact)
                    .map(SOURCE_ROOT::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertThat(forbiddenPaths)
                .as("В src/main/java не должно быть бинарных/временных файлов")
                .isEmpty();
    }

    private boolean isForbiddenArtifact(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".class")
                || fileName.endsWith(".tmp")
                || fileName.endsWith(".bak")
                || fileName.equals("tmpfile.txt");
    }
}
