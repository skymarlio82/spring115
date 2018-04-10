
package org.springframework.beans.factory;

import java.util.Map;

import org.springframework.beans.BeansException;

public interface ListableBeanFactory extends BeanFactory {
	
	boolean containsBeanDefinition(String beanName);

	int getBeanDefinitionCount();

	String[] getBeanDefinitionNames();
	
	String[] getBeanDefinitionNames(Class type);

	String[] getBeanNamesForType(Class type);

	Map getBeansOfType(Class type) throws BeansException;

	Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException;
}
