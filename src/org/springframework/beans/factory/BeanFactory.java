
package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

public interface BeanFactory {

	String FACTORY_BEAN_PREFIX = "&";

	Object getBean(String name) throws BeansException;

	Object getBean(String name, Class requiredType) throws BeansException;

	boolean containsBean(String name);

	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	Class getType(String name) throws NoSuchBeanDefinitionException;

	String[] getAliases(String name) throws NoSuchBeanDefinitionException;
}
