
package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;

public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {
	
	void ignoreDependencyType(Class type);
	
	BeanDefinition getBeanDefinition(String beanName) throws BeansException;
	
	void preInstantiateSingletons() throws BeansException;
}
