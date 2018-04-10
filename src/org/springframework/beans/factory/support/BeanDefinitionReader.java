
package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public interface BeanDefinitionReader {

	BeanDefinitionRegistry getBeanFactory();

	ClassLoader getBeanClassLoader();

	ResourceLoader getResourceLoader();

	int loadBeanDefinitions(Resource resource) throws BeansException;

	int loadBeanDefinitions(Resource[] resources) throws BeansException;
}
