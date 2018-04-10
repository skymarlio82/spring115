
package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

	private final DefaultListableBeanFactory beanFactory;

	private ResourceLoader resourceLoader;

	private boolean refreshed = false;


	/**
	 * Create a new GenericApplicationContext.
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	/**
	 * Create a new GenericApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Create a new GenericApplicationContext with the given parent.
	 * @param parent the parent application context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(ApplicationContext parent) {
		super(parent);
		this.beanFactory = new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * Create a new GenericApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @param parent the parent application context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
		super(parent);
		this.beanFactory = beanFactory;
	}


	/**
	 * Set a ResourceLoader to use for this context. If set, the context
	 * will delegate all resource loading to the given ResourceLoader.
	 * If not set, default resource loading will apply.
	 * <p>The main reason to specify a custom ResourceLoader is to resolve
	 * resource paths (withour URL prefix) in a specific fashion.
	 * The default behavior is to resolve such paths as class path locations.
	 * To resolve resource paths as file system locations, specify a
	 * FileSystemResourceLoader here.
	 * @see #getResource
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.core.io.FileSystemResourceLoader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * This implementation delegates to this context's ResourceLoader if set,
	 * falling back to the default superclass behavior else.
	 * @see #setResourceLoader
	 */
	public Resource getResource(String location) {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getResource(location);
		}
		return super.getResource(location);
	}


	//---------------------------------------------------------------------
	// Implementations of AbstractApplicationContext's template methods
	//---------------------------------------------------------------------

	/**
	 * Do nothing: We hold a single internal BeanFactory and rely on callers
	 * to register beans through our public methods (or the BeanFactory's).
	 * @see #registerBeanDefinition
	 */
	protected void refreshBeanFactory() throws IllegalStateException {
		if (this.refreshed) {
			throw new IllegalStateException("Multiple refreshs not supported - just call 'refresh' once");
		}
		this.refreshed = true;
	}

	/**
	 * Return the single internal BeanFactory held by this context.
	 */
	public ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Return the underlying bean factory of this context,
	 * available for registering bean definitions.
	 * <p><b>NOTE:</b> You need to call <code>refresh</code> to initialize the
	 * bean factory and its contained beans with application context semantics
	 * (auto-detecting BeanFactoryPostProcessors, etc)
	 * @see #refresh
	 */
	public DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
		return this.beanFactory.getBeanDefinition(beanName);
	}

	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeansException {
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}

	public void registerAlias(String beanName, String alias) throws BeansException {
		this.beanFactory.registerAlias(beanName, alias);
	}

}
