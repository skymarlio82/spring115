
package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.StringUtils;

public class SimpleInstantiationStrategy implements InstantiationStrategy {
	
	protected final Log logger = LogFactory.getLog(getClass());

	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
		// don't override the class with CGLIB if no overrides
		if (beanDefinition.getMethodOverrides().isEmpty()) {
			logger.debug("try to start to create the instance of '" + beanDefinition.getBeanClass() + "'");
			return BeanUtils.instantiateClass(beanDefinition.getBeanClass());
		} else {
			// must generate CGLIB subclass
			return instantiateWithMethodInjection(beanDefinition, beanName, owner);
		}
	}
	
	protected Object instantiateWithMethodInjection(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner, Constructor ctor, Object[] args) {
		if (beanDefinition.getMethodOverrides().isEmpty()) {
			return BeanUtils.instantiateClass(ctor, args);
		} else {
			return instantiateWithMethodInjection(beanDefinition, beanName, owner, ctor, args);
		}
	}
	
	protected Object instantiateWithMethodInjection(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner, Constructor ctor, Object[] args) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner, Object factoryBean, Method factoryMethod, Object[] args) {
		try {
			// It's a static method if the target is null.
			return factoryMethod.invoke(factoryBean, args);
		} catch (IllegalArgumentException ex) {
			throw new BeanDefinitionStoreException("Illegal arguments to factory method [" + factoryMethod + "]; " + "args: " + StringUtils.arrayToCommaDelimitedString(args));
		} catch (IllegalAccessException ex) {
			throw new BeanDefinitionStoreException("Cannot access factory method [" + factoryMethod + "]; is it public?");
		} catch (InvocationTargetException ex) {
			String msg = "Factory method [" + factoryMethod + "] threw exception";
			// We want to log this one, as it may be a config error: the method may match, but may have been given incorrect arguments.
			logger.warn(msg, ex.getTargetException());
			throw new BeanDefinitionStoreException(msg, ex.getTargetException());
		}
	}
}
