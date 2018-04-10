
package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

public class RootBeanDefinition extends AbstractBeanDefinition {
	
	public RootBeanDefinition(Class beanClass) {
		super();
		setBeanClass(beanClass);
	}
	
	public RootBeanDefinition(Class beanClass, boolean singleton) {
		super();
		setBeanClass(beanClass);
		setSingleton(singleton);
	}
	
	public RootBeanDefinition(Class beanClass, int autowireMode) {
		super();
		setBeanClass(beanClass);
		setAutowireMode(autowireMode);
	}
	
	public RootBeanDefinition(Class beanClass, int autowireMode, boolean dependencyCheck) {
		super();
		setBeanClass(beanClass);
		setAutowireMode(autowireMode);
		if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
			setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		}
	}
	
	public RootBeanDefinition(Class beanClass, MutablePropertyValues pvs) {
		super(null, pvs);
		setBeanClass(beanClass);
	}
	
	public RootBeanDefinition(Class beanClass, MutablePropertyValues pvs, boolean singleton) {
		super(null, pvs);
		setBeanClass(beanClass);
		setSingleton(singleton);
	}
	
	public RootBeanDefinition(Class beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClass(beanClass);
	}
	
	public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClassName(beanClassName);
	}
	
	public RootBeanDefinition(RootBeanDefinition original) {
		super(original);
	}

	public void validate() throws BeanDefinitionValidationException {
		super.validate();				
		if (hasBeanClass()) {
			if (FactoryBean.class.isAssignableFrom(getBeanClass()) && !isSingleton()) {
				throw new BeanDefinitionValidationException("FactoryBean must be defined as singleton - " + "FactoryBeans themselves are not allowed to be prototypes");
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("Root bean with class [");
		sb.append(getBeanClassName()).append(']');
		if (getResourceDescription() != null) {
			sb.append(" defined in ").append(getResourceDescription());
		}
		return sb.toString();
	}
}
