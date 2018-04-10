
package org.springframework.beans.factory.config;

import org.springframework.beans.MutablePropertyValues;

public interface BeanDefinition {
	
	Class getBeanClass();
	
	boolean isAbstract();
	
	boolean isSingleton();
	
	boolean isLazyInit();
	
	MutablePropertyValues getPropertyValues();
	
	ConstructorArgumentValues getConstructorArgumentValues();
	
	String getResourceDescription();

}
