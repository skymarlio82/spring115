
package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

public class ClassPathResource extends AbstractResource {

	private final String path;

	private ClassLoader classLoader = null;
	private Class clazz             = null;
	
	public ClassPathResource(String path) {
		this(path, (ClassLoader)null);
	}
	
	public ClassPathResource(String path, ClassLoader classLoader) {
		Assert.notNull(path, "path is required");
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		this.path = path;
		this.classLoader = classLoader;
	}
	
	public ClassPathResource(String path, Class clazz) {
		Assert.notNull(path, "path is required");
		this.path = path;
		this.clazz = clazz;
	}
	
	protected ClassPathResource(String path, ClassLoader classLoader, Class clazz) {
		Assert.notNull(path, "path is required");
		this.path = path;
		this.classLoader = classLoader;
		this.clazz = clazz;
	}

	public InputStream getInputStream() throws IOException {
		InputStream is = null;
		if (clazz != null) {
			is = clazz.getResourceAsStream(path);
		} else {
			ClassLoader cl = classLoader;
			if (cl == null) {
				// no class loader specified -> use thread context class loader
				cl = Thread.currentThread().getContextClassLoader();
			}
			is = cl.getResourceAsStream(path);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	public URL getURL() throws IOException {
		URL url = null;
		if (clazz != null) {
			url = clazz.getResource(this.path);
		} else {
			ClassLoader cl = classLoader;
			if (cl == null) {
				// no class loader specified -> use thread context class loader
				cl = Thread.currentThread().getContextClassLoader();
			}
			url = cl.getResource(path);
		}
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	public File getFile() throws IOException {
		return ResourceUtils.getFile(getURL(), getDescription());
	}

	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(path, relativePath);
		return new ClassPathResource(pathToUse, classLoader, clazz);
	}

	public String getFilename() {
		return StringUtils.getFilename(path);
	}

	public String getDescription() {
		return "class path resource [" + path + "]";
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ClassPathResource) {
			ClassPathResource otherRes = (ClassPathResource)obj;
			return (path.equals(otherRes.path) && ObjectUtils.nullSafeEquals(clazz, otherRes.clazz));
		}
		return false;
	}

	public int hashCode() {
		return path.hashCode();
	}
}
