package net.wjlafrance.grumble.net;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public @NoArgsConstructor(access = AccessLevel.PRIVATE) class OverlyTrustingSSLContext {

	/**
	 * Get a SSLContext that uses a custom X509TrustManager implementation that will trust any SSL certificate.
	 *
	 * Note, this is an incredibly bad idea from a security standpoint.
	 *
	 * @return An SSLContext that trusts all certificates
	 * @throws NoSuchAlgorithmException If the system's SSLContext doesn't know what SSL is
	 * @throws KeyManagementException
	 */
	public static SSLContext getInstance() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager nopTrustManager = new X509TrustManager() {
			@Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { /* nop */ }
			@Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { /* nop */ }
			@Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
		};

		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[] { nopTrustManager }, new SecureRandom());
		return sslContext;
	}

}
