
package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;

public interface ConfigurableBeanFactory extends HierarchicalBeanFactory {

	void setParentBeanFactory(BeanFactory parentBeanFactory);

	void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor);

	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	void registerAlias(String beanName, String alias) throws BeansException;

	void registerSingleton(String beanName, Object singletonObject) throws BeansException;

	boolean containsSingleton(String beanName);

	void destroySingletons();

}
