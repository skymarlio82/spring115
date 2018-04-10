
package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;

public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext  {

	public AbstractXmlApplicationContext() {
		
	}

	public AbstractXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
		logger.info("Start to create the instance of 'XmlBeanDefinitionReader' with 'DefaultListableBeanFactory' ...");
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		beanDefinitionReader.setResourceLoader(this);
		logger.info("Start to create the instance of 'ResourceEntityResolver' with 'AbstractXmlApplicationContext' ...");
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
		
	}

	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (int i = 0; i < configLocations.length; i++) {
				reader.loadBeanDefinitions(getResources(configLocations[i]));
			}
		}
	}

	protected abstract String[] getConfigLocations();
}
