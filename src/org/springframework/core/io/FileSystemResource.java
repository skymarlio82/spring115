
package org.springframework.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

public class FileSystemResource extends AbstractResource {

	private final File file;
	private final String path;
	
	public FileSystemResource(File file) {
		Assert.notNull(file, "file is required");
		this.file = file;
		this.path = StringUtils.cleanPath(file.getPath());
	}
	
	public FileSystemResource(String path) {
		Assert.notNull(path, "path is required");
		this.file = new File(path);
		this.path = path;
	}

	public boolean exists() {
		return file.exists();
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}

	public URL getURL() throws IOException {
		return new URL(ResourceUtils.URL_PROTOCOL_FILE + ":" + file.getAbsolutePath());
	}

	public File getFile() {
		return file;
	}

	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(path, relativePath);
		return new FileSystemResource(pathToUse);
	}

	public String getFilename() {
		return file.getName();
	}

	public String getDescription() {
		return "file [" + file.getAbsolutePath() + "]";
	}

	public boolean equals(Object obj) {
		return (obj == this || (obj instanceof FileSystemResource && file.equals(((FileSystemResource)obj).file)));
	}

	public int hashCode() {
		return this.file.hashCode();
	}
}
