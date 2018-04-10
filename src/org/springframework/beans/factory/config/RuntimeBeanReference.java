
package org.springframework.beans.factory.config;

import org.springframework.util.Assert;

public class RuntimeBeanReference {
	
	private final String beanName;
	private final boolean toParent;
	
	public RuntimeBeanReference(String beanName) {
		this(beanName, false);
	}
	
	public RuntimeBeanReference(String beanName, boolean toParent) {
		Assert.hasText(beanName, "Bean name must not be empty");
		this.beanName = beanName;
		this.toParent = toParent;
	}
	
	public String getBeanName() {
		return beanName;
	}
	
	public boolean isToParent() {
		return toParent;
	}
	
	public String toString() {
	   return '<' + getBeanName() + '>';
	}
}
