
package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.util.StringUtils;

public class BeanDefinitionReaderUtils {
	
	public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";
	
	public static AbstractBeanDefinition createBeanDefinition(String className, String parent, ConstructorArgumentValues cargs, MutablePropertyValues pvs, ClassLoader classLoader) throws ClassNotFoundException {
		Class beanClass = null;
		if (className != null && classLoader != null) {
			beanClass = Class.forName(className, true, classLoader);
		}
		if (parent == null) {
			if (beanClass != null) {
				return new RootBeanDefinition(beanClass, cargs, pvs);
			} else {
				return new RootBeanDefinition(className, cargs, pvs);
			}
		} else {
			if (beanClass != null) {
				return new ChildBeanDefinition(parent, beanClass, cargs, pvs);
			} else {
				return new ChildBeanDefinition(parent, className, cargs, pvs);
			}
		}
	}
	
	public static String generateBeanName(AbstractBeanDefinition beanDefinition, BeanDefinitionRegistry beanFactory) throws BeanDefinitionStoreException {
		String generatedId = beanDefinition.getBeanClassName();
		if (generatedId == null && beanDefinition instanceof ChildBeanDefinition) {
			generatedId = ((ChildBeanDefinition) beanDefinition).getParentName();
		}
		if (!StringUtils.hasText(generatedId)) {
			throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), "", "Unnamed bean definition specifies neither 'class' nor 'parent' - can't generate name");
		}
		int counter = 1;
		String id = generatedId;
		while (beanFactory.containsBeanDefinition(id)) {
			counter++;
			id = generatedId + GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}
	
	public static void registerBeanDefinition(BeanDefinitionHolder bdHolder, BeanDefinitionRegistry beanFactory) throws BeansException {
		// register bean definition under primary name
		beanFactory.registerBeanDefinition(bdHolder.getBeanName(), bdHolder.getBeanDefinition());
		// register aliases for bean name, if any
		if (bdHolder.getAliases() != null) {
			for (int i = 0; i < bdHolder.getAliases().length; i++) {
				beanFactory.registerAlias(bdHolder.getBeanName(), bdHolder.getAliases()[i]);
			}
		}
	}
}
