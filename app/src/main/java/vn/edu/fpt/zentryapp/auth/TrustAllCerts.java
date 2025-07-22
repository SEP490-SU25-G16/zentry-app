package vn.edu.fpt.zentryapp.auth;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for SSL handling
 * WARNING: This is for development only and should not be used in production
 */
public class TrustAllCerts implements X509TrustManager {
    private static final X509Certificate[] ACCEPTED_ISSUERS = new X509Certificate[0];
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new TrustAllCerts()};
    private static final HostnameVerifier TRUST_ALL_HOSTNAMES = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // No check for development
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // No check for development
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return ACCEPTED_ISSUERS;
    }

    /**
     * Get SSL socket factory that trusts all certificates
     * @return SSLSocketFactory instance
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL socket factory", e);
        }
    }

    /**
     * Get trust manager that trusts all certificates
     * @return X509TrustManager instance
     */
    public static X509TrustManager getTrustManager() {
        return new TrustAllCerts();
    }

    /**
     * Get hostname verifier that trusts all hostnames
     * @return HostnameVerifier instance
     */
    public static HostnameVerifier getHostnameVerifier() {
        return TRUST_ALL_HOSTNAMES;
    }
}
