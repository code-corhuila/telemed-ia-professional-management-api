package co.edu.corhuila.telemedia.professionalmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Numeral 5.3.3 (Anexo C): the domain and the application layer import no web, persistence or
 * logging framework; the composition root wires them.
 */
class FrameworkFreeCoreTest {

    private static final Path SOURCES = Path.of("src/main/java/co/edu/corhuila/telemedia/professionalmanagement");
    private static final Pattern FRAMEWORK = Pattern.compile(
            "^import (static )?(org\\.springframework|jakarta\\.|javax\\.persistence|org\\.hibernate|org\\.slf4j)",
            Pattern.MULTILINE);

    @ParameterizedTest
    @ValueSource(strings = {"domain", "application"})
    void importsNoFramework(String layer) throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.walk(SOURCES.resolve(layer))) {
            sources = files.filter(file -> file.toString().endsWith(".java")).toList();
        }

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source ->
                assertThat(FRAMEWORK.matcher(Files.readString(source)).find())
                        .as("%s imports a framework", source.getFileName())
                        .isFalse());
    }
}
