
package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	private String[] configLocations = null;

	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation});
	}
	
	public ClassPathXmlApplicationContext(String[] configLocations) throws BeansException {
		this(configLocations, true);
	}

	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this.configLocations = configLocations;
		if (refresh) {
			refresh();
		}
	}
	
	public ClassPathXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}

	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException {
		super(parent);
		this.configLocations = configLocations;
		if (refresh) {
			refresh();
		}
	}

	protected String[] getConfigLocations() {
		return configLocations;
	}
}
