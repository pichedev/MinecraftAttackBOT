package io.github.cesarsdevs.minebot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyManager {

	private final List<Proxy> proxies = new ArrayList<>();

	private final AtomicInteger currentPosition = new AtomicInteger(0);

	public ProxyManager() {
		proxies.add(Proxy.NO_PROXY);

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("socks4_proxies.txt")));
		} catch (FileNotFoundException e) {
			System.out.println("No found socks4_proxies.txt file");
			return;
		}

		reader.lines().forEach(str -> {
			final String[] attribute = str.split(":");

			final String host = attribute[0];
			final int port = Integer.parseInt(attribute[1]);

			Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

			proxies.add(proxy);
		});
	}

	public synchronized Proxy nextProxy() {
		final Proxy proxy = proxies.get(this.currentPosition.getAndIncrement());

		if (this.currentPosition.get() >= proxies.size()) {
			this.currentPosition.set(0);
		}

		return proxy;
	}
}