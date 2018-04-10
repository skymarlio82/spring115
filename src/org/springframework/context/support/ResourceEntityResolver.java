
package org.springframework.context.support;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.xml.sax.InputSource;

import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

public class ResourceEntityResolver extends BeansDtdResolver {

	private final ApplicationContext applicationContext;

	public ResourceEntityResolver(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@SuppressWarnings("deprecation")
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		InputSource source = super.resolveEntity(publicId, systemId);
		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String decodedSystemId = URLDecoder.decode(systemId);
				String givenUrl = new URL(decodedSystemId).toString();
				String systemRootUrl = new File("").toURL().toString();
				// try relative to resource base if currently in system root
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			} catch (MalformedURLException ex) {
				// no URL -> try relative to resource base
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Trying to locate entity [" + systemId + "] as application context resource [" + resourcePath + "]");
				}
				Resource resource = applicationContext.getResource(resourcePath);
				if (logger.isInfoEnabled()) {
					logger.info("Found entity [" + systemId + "] as application context resource [" + resourcePath + "]");
				}
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			}
		}
		return source;
	}
}
