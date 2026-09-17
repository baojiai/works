package com.course.aftersales.probes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionProbeTest {
    private static final long TIMEOUT_SECONDS = 120;

    @TempDir
    Path tempDir;

    @Test
    void customerTransactions() throws Exception {
        runH2Probe("Stage7CustomerTransactionProbe", "CUSTOMER_TX_OK");
    }

    @Test
    void engineerTransactions() throws Exception {
        runH2Probe("Stage7EngineerTransactionProbe", "ENGINEER_TX_OK");
    }

    @Test
    void warehouseTransactions() throws Exception {
        runH2Probe("Stage7WarehouseTransactionProbe", "WAREHOUSE_TX_OK");
    }

    @Test
    void adminTransactions() throws Exception {
        runH2Probe("Stage7AdminTransactionProbe", "ADMIN_TX_OK");
    }

    private void runH2Probe(String mainClass, String successMarker) throws Exception {
        Path base = tempDir.resolve(mainClass);
        String databasePath = base.resolve("data").resolve("after_sales")
                .toAbsolutePath().toString().replace('\\', '/');
        List<String> command = new ArrayList<>(Arrays.asList(
                javaExecutable(), "-cp", System.getProperty("java.class.path"),
                mainClass, base.toAbsolutePath().toString()
        ));
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environment = builder.environment();
        environment.put("APP_DB_URL", "jdbc:h2:file:" + databasePath
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=SYSTEM_USER");
        environment.put("APP_DB_USER", "sa");
        environment.put("APP_DB_PASSWORD", "");
        environment.put("APP_DB_DRIVER", "org.h2.Driver");
        builder.redirectErrorStream(true);

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
