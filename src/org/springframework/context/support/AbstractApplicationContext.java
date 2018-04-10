
package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

	protected final Log logger = LogFactory.getLog(getClass());
	
	public static final String MESSAGE_SOURCE_BEAN_NAME                = "messageSource";
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}
	
	// ---------------------------------------------------------------------
	// Instance data
	// ---------------------------------------------------------------------
	
	private long startupTime = 0L;
	
	private ApplicationContext parent                               = null;
	private ResourcePatternResolver resourcePatternResolver         = null;
	private MessageSource messageSource                             = null;
	private ApplicationEventMulticaster applicationEventMulticaster = null;
	
	@SuppressWarnings("rawtypes")
	private final List beanFactoryPostProcessors = new ArrayList();
	private String displayName = getClass().getName() + ";hashCode=" + hashCode();
	
	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------
	
	public AbstractApplicationContext() {
		this(null);
	}

	public AbstractApplicationContext(ApplicationContext parent) {
		this.parent = parent;
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	// ---------------------------------------------------------------------
	// Implementation of ApplicationContext
	// ---------------------------------------------------------------------
	
	public ApplicationContext getParent() {
		return parent;
	}

	protected void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public long getStartupDate() {
		return startupTime;
	}

	public void publishEvent(ApplicationEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Publishing event in context [" + getDisplayName() + "]: " + event.toString());
		}
		getApplicationEventMulticaster().multicastEvent(event);
		if (this.parent != null) {
			this.parent.publishEvent(event);
		}
	}

	private ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " + "call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}


	// ---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext
	// ---------------------------------------------------------------------

	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
	}

	@SuppressWarnings("rawtypes")
	public List getBeanFactoryPostProcessors() {
		return beanFactoryPostProcessors;
	}

	@SuppressWarnings("rawtypes")
	public void refresh() throws BeansException, IllegalStateException {
		startupTime = System.currentTimeMillis();
		logger.info("'AbstractApplicationContext' starts to refresh time, startupTime == [" + startupTime + "]");
		// Tell subclass to refresh the internal bean factory.
		refreshBeanFactory();
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// Configure the bean factory with context-specific editors.
		beanFactory.registerCustomEditor(Resource.class, new ResourceEditor(this));
		beanFactory.registerCustomEditor(URL.class, new URLEditor(new ResourceEditor(this)));
		beanFactory.registerCustomEditor(InputStream.class, new InputStreamEditor(new ResourceEditor(this)));
		beanFactory.registerCustomEditor(Resource[].class, new ResourceArrayPropertyEditor(resourcePatternResolver));
		// Configure the bean factory with context semantics.
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		beanFactory.ignoreDependencyType(ResourceLoader.class);
		beanFactory.ignoreDependencyType(ApplicationContext.class);
		// Allows post-processing of the bean factory in context subclasses.
		postProcessBeanFactory(beanFactory);
		logger.info("the size of 'beanFactoryPostProcessors' == " + getBeanFactoryPostProcessors().size());
		// Invoke factory processors registered with the context instance.
		for (Iterator it = getBeanFactoryPostProcessors().iterator(); it.hasNext(); ) {
			BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor)it.next();
			factoryProcessor.postProcessBeanFactory(beanFactory);
		}
		if (logger.isInfoEnabled()) {
			if (getBeanDefinitionCount() == 0) {
				logger.info("No beans defined in application context [" + getDisplayName() + "]");
			} else {
				logger.info(getBeanDefinitionCount() + " beans defined in application context [" + getDisplayName() + "]");
			}
		}
		// Invoke factory processors registered as beans in the context.
		invokeBeanFactoryPostProcessors();
		// Register bean processors that intercept bean creation.
		registerBeanPostProcessors();
		// Initialize message source for this context.
		initMessageSource();
		// Initialize event multicaster for this context.
		initApplicationEventMulticaster();
		// Initialize other special beans in specific context subclasses.
		onRefresh();
		// Check for listener beans and register them.
		refreshListeners();
		// iIstantiate singletons this late to allow them to access the message source.
		beanFactory.preInstantiateSingletons();
		// Last step: publish corresponding event.
		publishEvent(new ContextRefreshedEvent(this));
	}

	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}

	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeBeanFactoryPostProcessors() throws BeansException {
		Map factoryProcessorMap = getBeansOfType(BeanFactoryPostProcessor.class, true, false);
		List factoryProcessors = new ArrayList(factoryProcessorMap.values());
		Collections.sort(factoryProcessors, new OrderComparator());
		for (Iterator it = factoryProcessors.iterator(); it.hasNext();) {
			BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor)it.next();
			factoryProcessor.postProcessBeanFactory(getBeanFactory());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerBeanPostProcessors() throws BeansException {
		Map beanProcessorMap = getBeansOfType(BeanPostProcessor.class, true, false);
		List beanProcessors = new ArrayList(beanProcessorMap.values());
		Collections.sort(beanProcessors, new OrderComparator());
		for (Iterator it = beanProcessors.iterator(); it.hasNext();) {
			getBeanFactory().addBeanPostProcessor((BeanPostProcessor) it.next());
		}
	}

	private void initMessageSource() throws BeansException {
		if (containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			messageSource = (MessageSource)getBean(MESSAGE_SOURCE_BEAN_NAME);
			// Make MessageSource aware of parent MessageSource.
			if (parent != null && messageSource instanceof HierarchicalMessageSource) {
				MessageSource parentMessageSource = getInternalParentMessageSource();
				((HierarchicalMessageSource)messageSource).setParentMessageSource(parentMessageSource);
			}
			if (logger.isInfoEnabled()) {
				logger.info("Using MessageSource [" + messageSource + "]");
			}
		} else {
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			messageSource = dms;
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME + "': using default [" + messageSource + "]");
			}
		}
	}

	private void initApplicationEventMulticaster() throws BeansException {
		if (containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			applicationEventMulticaster = (ApplicationEventMulticaster)getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
			if (logger.isInfoEnabled()) {
				logger.info("Using ApplicationEventMulticaster [" + applicationEventMulticaster + "]");
			}
		} else {
			applicationEventMulticaster = new SimpleApplicationEventMulticaster();
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate ApplicationEventMulticaster with name '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "': using default [" + applicationEventMulticaster + "]");
			}
		}
	}

	private boolean containsLocalBean(String beanName) {
		return (containsBeanDefinition(beanName) || getBeanFactory().containsSingleton(beanName));
	}

	protected void onRefresh() throws BeansException {
		// for subclasses: do nothing by default
	}

	@SuppressWarnings("rawtypes")
	private void refreshListeners() throws BeansException {
		logger.debug("Refreshing listeners");
		Collection listeners = getBeansOfType(ApplicationListener.class, true, false).values();
		if (logger.isDebugEnabled()) {
			logger.debug("Found " + listeners.size() + " listeners in bean factory");
		}
		for (Iterator it = listeners.iterator(); it.hasNext(); ) {
			ApplicationListener listener = (ApplicationListener)it.next();
			addListener(listener);
			if (logger.isInfoEnabled()) {
				logger.info("Application listener [" + listener + "] added");
			}
		}
	}

	protected void addListener(ApplicationListener listener) {
		getApplicationEventMulticaster().addApplicationListener(listener);
	}
	
	public void close() {
		if (logger.isInfoEnabled()) {
			logger.info("Closing application context [" + getDisplayName() + "]");
		}
		// publish corresponding event
		publishEvent(new ContextClosedEvent(this));
		// Destroy all cached singletons in this context,
		// invoking DisposableBean.destroy and/or "destroy-method".
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			beanFactory.destroySingletons();
		}
	}

	// ---------------------------------------------------------------------
	// Implementation of BeanFactory
	// ---------------------------------------------------------------------
	
	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	@SuppressWarnings("rawtypes")
	public Object getBean(String name, Class requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	@SuppressWarnings("rawtypes")
	public Class getType(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name);
	}

	public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getAliases(name);
	}

	// ---------------------------------------------------------------------
	// Implementation of ListableBeanFactory
	// ---------------------------------------------------------------------
	
	public boolean containsBeanDefinition(String name) {
		return getBeanFactory().containsBeanDefinition(name);
	}

	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@SuppressWarnings("rawtypes")
	public String[] getBeanDefinitionNames(Class type) {
		return getBeanFactory().getBeanDefinitionNames(type);
	}

	@SuppressWarnings("rawtypes")
	public String[] getBeanNamesForType(Class type) {
		return getBeanFactory().getBeanNamesForType(type);
	}

	@SuppressWarnings("rawtypes")
	public Map getBeansOfType(Class type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	@SuppressWarnings("rawtypes")
	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
		return getBeanFactory().getBeansOfType(type, includePrototypes, includeFactoryBeans);
	}

	// --------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory
	// --------------------------------------------------------------------
	
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	protected BeanFactory getInternalParentBeanFactory() {
		logger.debug("Parent's ApplicationContext is " + getParent());
		return (getParent() instanceof ConfigurableApplicationContext) ? ((ConfigurableApplicationContext)getParent()).getBeanFactory() : (BeanFactory)getParent();
	}

	// ---------------------------------------------------------------------
	// Implementation of MessageSource
	// ---------------------------------------------------------------------
	
	public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " + "call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext) ? ((AbstractApplicationContext)getParent()).messageSource : getParent();
	}

	// ---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver
	// ---------------------------------------------------------------------

	public Resource[] getResources(String locationPattern) throws IOException {
		return resourcePatternResolver.getResources(locationPattern);
	}

	// ---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	// ---------------------------------------------------------------------

	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(": ");
		sb.append("display name [").append(this.displayName).append("]; ");
		sb.append("startup date [").append(new Date(this.startupTime)).append("]; ");
		if (this.parent == null) {
			sb.append("root of context hierarchy");
		} else {
			sb.append("child of [").append(this.parent).append(']');
		}
		return sb.toString();
	}
}
