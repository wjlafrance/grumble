package net.wjlafrance.grumble;

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
	 * This is a really bad idea.
	 *
	 * @return Something terrible.
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static SSLContext getInstance() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[]{new X509TrustManager() {
			@Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
				/* nop */
			}

			@Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
				/* nop */
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}}, new SecureRandom());
		return sslContext;
	}

}
