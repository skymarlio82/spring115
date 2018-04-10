
package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.util.Assert;

public class DefaultResourceLoader implements ResourceLoader {

	private final ClassLoader classLoader;
	
	public DefaultResourceLoader() {
		this(null);
	}
	
	public DefaultResourceLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public Resource getResource(String location) {
		Assert.notNull(location, "location is required");
		if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		} else {
			try {
				// try URL
				URL url = new URL(location);
				return new UrlResource(url);
			} catch (MalformedURLException ex) {
				// no URL -> resolve resource path
				return getResourceByPath(location);
			}
		}
	}
	
	protected Resource getResourceByPath(String path) {
		return new ClassPathResource(path, getClassLoader());
	}
}
