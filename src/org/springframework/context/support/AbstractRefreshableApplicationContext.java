
package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	private DefaultListableBeanFactory beanFactory = null;

	public AbstractRefreshableApplicationContext() {
		
	}
	
	public AbstractRefreshableApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	protected final void refreshBeanFactory() throws BeansException {
		// Shut down previous bean factory, if any.
		if (beanFactory != null) {
			beanFactory.destroySingletons();
			beanFactory = null;
		}
		// Initialize fresh bean factory.
		try {
			logger.info("Start to create the instance of 'DefaultListableBeanFactory' ...");
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			loadBeanDefinitions(beanFactory);
			this.beanFactory = beanFactory;
			if (logger.isInfoEnabled()) {
				logger.info("Bean factory for application context [" + getDisplayName() + "] : " + beanFactory);
			}
		} catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing XML document for application context [" + getDisplayName() + "]", ex);
		}
	}

	public final ConfigurableListableBeanFactory getBeanFactory() {
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized - " + "call 'refresh' before accessing beans via the context: " + this);
		}
		return beanFactory;
	}
	
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}
	
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException, BeansException;
}
