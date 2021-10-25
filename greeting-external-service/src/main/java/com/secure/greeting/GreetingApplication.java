/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.secure.greeting;

//import com.azure.security.keyvault.jca.KeyVaultJcaProvider;
import java.security.KeyStore;
//import java.security.Security;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
//@EnableDiscoveryClient
public class GreetingApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreetingApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() throws Exception {
//		KeyVaultJcaProvider provider = new KeyVaultJcaProvider();
//		Security.addProvider(provider);
		KeyStore ks = KeyStore.getInstance("AzureKeyVault");
		SSLContext sslContext = SSLContexts.custom()
			.loadTrustMaterial(ks, new TrustSelfSignedStrategy())
			.build();

		HostnameVerifier allowAll = (String hostName, SSLSession session) -> true;
		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, allowAll);

		CloseableHttpClient httpClient = HttpClients.custom()
			.setSSLSocketFactory(csf)
			.build();

		HttpComponentsClientHttpRequestFactory requestFactory
			= new HttpComponentsClientHttpRequestFactory();

		requestFactory.setHttpClient(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		return restTemplate;
	}

}
