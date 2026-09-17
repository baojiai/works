package com.course.aftersales.probes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "(?i)true")
class MySqlProbeTest {
    private static final long TIMEOUT_SECONDS = 180;

    @Test
    void mysqlConnection() throws Exception {
        runProbe("Stage9MySqlConnectionProbe", "MYSQL_CONNECTION_OK");
    }

    @Test
    void mysqlTransactionsAndConcurrency() throws Exception {
        runProbe("Stage9MySqlTransactionProbe", "STAGE9_MYSQL_TX_OK");
    }

    private void runProbe(String mainClass, String successMarker) throws Exception {
        requireEnvironment("APP_DB_URL");
        requireEnvironment("APP_DB_USER");
        requireEnvironment("APP_DB_PASSWORD");

        List<String> command = Arrays.asList(
                javaExecutable(), "-cp", System.getProperty("java.class.path"), mainClass
        );
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> copy(process.getInputStream(), output), mainClass + "-output");
        reader.start();
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        reader.join(TimeUnit.SECONDS.toMillis(5));
        String text = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertTrue(finished, mainClass + " timed out.\n" + text);
        assertEquals(0, process.exitValue(), mainClass + " failed.\n" + text);
        assertTrue(text.contains(successMarker), mainClass + " did not report success.\n" + text);
    }

    private static void requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " must be set for MySQL integration tests");
        }
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }

    private static void copy(InputStream input, ByteArrayOutputStream output) {
        byte[] buffer = new byte[8192];
        int read;
        try (InputStream stream = input) {
            while ((read = stream.read(buffer)) >= 0) output.write(buffer, 0, read);
        } catch (Exception ignored) {
            // The assertion reports process failures; stream shutdown after timeout is expected.
        }
    }
}
