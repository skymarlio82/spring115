
package org.springframework.beans.factory.config;

public class BeanDefinitionHolder {

	private final BeanDefinition beanDefinition;

	private final String beanName;

	private final String[] aliases;
	
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
		this(beanDefinition, beanName, null);
	}
	
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String[] aliases) {
		this.beanDefinition = beanDefinition;
		this.beanName = beanName;
		this.aliases = aliases;
	}

	public BeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	public String getBeanName() {
		return beanName;
	}

	public String[] getAliases() {
		return aliases;
	}

	public String toString() {
		return "Bean definition with name '" + this.beanName + "': " + this.beanDefinition;
	}
}
