
package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public interface Resource extends InputStreamSource {
	
	boolean exists();
	
	boolean isOpen();
	
	URL getURL() throws IOException;

	File getFile() throws IOException;

	Resource createRelative(String relativePath) throws IOException;
	
	String getFilename();

	String getDescription();
}
