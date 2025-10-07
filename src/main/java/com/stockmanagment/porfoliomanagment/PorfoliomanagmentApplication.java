package com.stockmanagment.porfoliomanagment;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
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
			String localhost = "127.0.0.1";
			Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
			while (nics.hasMoreElements()) {
				NetworkInterface nic = nics.nextElement();
				if (nic.isUp() && !nic.isLoopback()) {
					Enumeration<java.net.InetAddress> addrs = nic.getInetAddresses();
					while (addrs.hasMoreElements()) {
						java.net.InetAddress addr = addrs.nextElement();
						if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
							localhost = addr.getHostAddress();
							break;
						}
					}
				}
				if (!localhost.equals("127.0.0.1")) break;
			}

			String serverPort = environment.getProperty("server.port");

			boolean isSslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);

			String protocol = isSslEnabled ? "https" : "http";

			String localUrl = protocol + "://localhost:" + serverPort;
			String externalUrl = protocol + "://" + localhost + ":" + serverPort;

			System.out.println();
			System.out.println("\t Local: " + localUrl);
			System.out.println("\t External: " + externalUrl);
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
