package com.exometric;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

public class CertUtil {

    /**
     * Gera (se necessário) um keystore PKCS12 autoassinado usando o keytool do
     * próprio JDK que roda o servidor, e retorna um SSLContext pronto para uso.
     */
    public static SSLContext getOrCreateSelfSignedContext(File keystoreFile, String password, String hostname) throws Exception {
        if (!keystoreFile.exists()) {
            generateSelfSigned(keystoreFile, password, hostname);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static void generateSelfSigned(File keystoreFile, String password, String hostname) throws Exception {
        if (keystoreFile.getParentFile() != null && !keystoreFile.getParentFile().exists()) {
            keystoreFile.getParentFile().mkdirs();
        }

        String javaHome = System.getProperty("java.home");
        File keytoolFile = new File(javaHome, "bin/keytool" + (isWindows() ? ".exe" : ""));
        String keytoolPath = keytoolFile.exists() ? keytoolFile.getAbsolutePath() : "keytool";

        ProcessBuilder pb = new ProcessBuilder(
                keytoolPath,
                "-genkeypair",
                "-alias", "exometric",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "3650",
                "-storetype", "PKCS12",
                "-keystore", keystoreFile.getAbsolutePath(),
                "-storepass", password,
                "-keypass", password,
                "-dname", "CN=" + hostname + ", OU=ExoMetric, O=ExoMetric, L=Unknown, ST=Unknown, C=US",
                "-ext", "SAN=dns:" + hostname + ",dns:localhost,ip:127.0.0.1"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("keytool não respondeu a tempo ao gerar o certificado autoassinado.");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("keytool falhou ao gerar o certificado autoassinado (exit code " + process.exitValue() + "). Verifique se o keytool está disponível no JDK do servidor.");
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
