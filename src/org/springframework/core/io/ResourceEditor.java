
package org.springframework.core.io;

public class ResourceEditor extends AbstractPathResolvingPropertyEditor {

	private final ResourceLoader resourceLoader;

	public ResourceEditor() {
		resourceLoader = new DefaultResourceLoader();
	}
	
	public ResourceEditor(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setAsText(String text) {
		String locationToUse = resolvePath(text).trim();
		setValue(resourceLoader.getResource(locationToUse));
	}
}
