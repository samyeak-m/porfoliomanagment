package com.stockmanagment.porfoliomanagment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableConfigurationProperties
@EntityScan(basePackages = {
		"com.stockmanagment.porfoliomanagment.model.nepse",
		"com.stockmanagment.porfoliomanagment.model.portfolio"
})
public class PorfoliomanagmentApplication implements CommandLineRunner {
	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(PorfoliomanagmentApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		try {
			// Get the local IP address
			String localhost = InetAddress.getLocalHost().getHostAddress();

			// Get the server port
			String serverPort = environment.getProperty("server.port");

			// Determine whether SSL is enabled
			boolean isSslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);

			// Use "https" if SSL is enabled, otherwise use "http"
			String protocol = isSslEnabled ? "https" : "http";

			// Print the local and external server URLs
			System.out.println();
			System.out.println("\t Local: " + protocol + "://localhost:" + serverPort);
			System.out.println("\t External: " + protocol + "://" + localhost + ":" + serverPort);
			System.out.println();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
