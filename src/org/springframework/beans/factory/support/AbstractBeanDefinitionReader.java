
package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader {

	protected final Log logger = LogFactory.getLog(getClass());

	private final BeanDefinitionRegistry beanFactory;

	private ClassLoader beanClassLoader = Thread.currentThread().getContextClassLoader();

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		this.beanFactory = beanFactory;
	}

	public BeanDefinitionRegistry getBeanFactory() {
		return beanFactory;
	}
	
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	public ClassLoader getBeanClassLoader() {
		return beanClassLoader;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}
	
	public int loadBeanDefinitions(Resource[] resources) throws BeansException {
		int counter = 0;
		logger.debug("resources.length called from loadBeanDefinitions == " + resources.length);
		for (int i = 0; i < resources.length; i++) {
			counter += loadBeanDefinitions(resources[i]);
		}
		return counter;
	}
}
