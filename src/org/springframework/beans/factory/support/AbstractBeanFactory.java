
package org.springframework.beans.factory.support;

import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.Assert;

public abstract class AbstractBeanFactory implements ConfigurableBeanFactory {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private static final Object CURRENTLY_IN_CREATION = new Object();

	private BeanFactory parentBeanFactory = null;
	@SuppressWarnings("rawtypes")
	private Map customEditors             = new HashMap();

	@SuppressWarnings("rawtypes")
	private final List beanPostProcessors = new ArrayList();

	private boolean hasDestructionAwareBeanPostProcessors = false;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final Map aliasMap         = Collections.synchronizedMap(new HashMap());
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final Map singletonCache   = Collections.synchronizedMap(new HashMap());
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final Map disposableBeans  = Collections.synchronizedMap(new HashMap());
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private final Map dependentBeanMap = Collections.synchronizedMap(new HashMap());
	
	public AbstractBeanFactory() {
		
	}
	
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		return getBean(name, null, null);
	}
		
	public Object getBean(String name, Class requiredType) throws BeansException {
		return getBean(name, requiredType, null);
	}
	
	public Object getBean(String name, Object[] args) throws BeansException {
		return getBean(name, null, args);
	}
	
	public Object getBean(String name, Class requiredType, Object[] args) throws BeansException {
		String beanName = transformedBeanName(name);
		logger.debug("After transformedBeanName == " + beanName);
		Object bean = null;
		// Eagerly check singleton cache for manually registered singletons.
		Object sharedInstance = singletonCache.get(beanName);
		if (sharedInstance != null) {
			if (sharedInstance == CURRENTLY_IN_CREATION) {
				throw new BeanCurrentlyInCreationException(beanName);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
			}
			bean = getObjectForSharedInstance(name, sharedInstance);
		} else {
			// Check if bean definition exists in this factory.
			RootBeanDefinition mergedBeanDefinition = null;
			try {
				mergedBeanDefinition = getMergedBeanDefinition(beanName, false);
			} catch (NoSuchBeanDefinitionException ex) {
				// Not found -> check parent.
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					// Delegation to parent with args only possible for AbstractBeanFactory.
					return ((AbstractBeanFactory)parentBeanFactory).getBean(name, requiredType, args);
				} else if (parentBeanFactory != null && args == null) {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(name, requiredType);
				}
				throw ex;
			}
			checkMergedBeanDefinition(mergedBeanDefinition, beanName, requiredType, args);
			// Create bean instance.
			if (mergedBeanDefinition.isSingleton()) {
				synchronized (singletonCache) {
					// re-check singleton cache within synchronized block
					sharedInstance = singletonCache.get(beanName);
					if (sharedInstance == null) {
						if (logger.isInfoEnabled()) {
							logger.info("Creating shared instance of singleton bean '" + beanName + "'");
						}
						singletonCache.put(beanName, CURRENTLY_IN_CREATION);
						try {
							sharedInstance = createBean(beanName, mergedBeanDefinition, args);
							singletonCache.put(beanName, sharedInstance);
						} catch (BeansException ex) {
							singletonCache.remove(beanName);
							throw ex;
						}
					}
				}
				bean = getObjectForSharedInstance(name, sharedInstance);
			} else {
				// It's a prototype -> create a new instance.
				bean = createBean(name, mergedBeanDefinition, args);
			}
		}
		// Check if required type matches the type of the actual bean instance.
		if (requiredType != null && !requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return bean;
	}

	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (this.singletonCache.containsKey(beanName)) {
			return true;
		}
		if (containsBeanDefinition(beanName)) {
			return true;
		}
		else {
			// Not found -> check parent.
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.containsBean(name);
			}
			else {
				return false;
			}
		}
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		try {
			Class beanClass = null;
			boolean singleton = true;

			Object beanInstance = this.singletonCache.get(beanName);
			if (beanInstance == CURRENTLY_IN_CREATION) {
				throw new BeanCurrentlyInCreationException(beanName);
			}
			if (beanInstance != null) {
				beanClass = beanInstance.getClass();
				singleton = true;
			}
			else {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				if (bd.hasBeanClass()) {
					beanClass = bd.getBeanClass();
				}
				singleton = bd.isSingleton();
			}

			// In case of FactoryBean, return singleton status of created object if not a dereference.
			if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass) &&
					!isFactoryDereference(name)) {
				FactoryBean factoryBean = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			}
			return singleton;
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Not found -> check parent.
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.isSingleton(name);
			}
			throw ex;
		}
	}

	public Class getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		try {
			Class beanClass = null;

			// Check manually registered singletons.
			Object beanInstance = this.singletonCache.get(beanName);
			if (beanInstance == CURRENTLY_IN_CREATION) {
				throw new BeanCurrentlyInCreationException(beanName);
			}
			if (beanInstance != null) {
				beanClass = beanInstance.getClass();
			}
			
			else {
				// OK, let's assume it's a bean definition.
				RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition(beanName, false);

				// Return "undeterminable" for beans without class or with factory method.
				if (!mergedBeanDefinition.hasBeanClass() || mergedBeanDefinition.getFactoryMethodName() != null) {
					return null;
				}

				beanClass = mergedBeanDefinition.getBeanClass();
			}

			// Check bean class whether we're dealing with a FactoryBean.
			if (FactoryBean.class.isAssignableFrom(beanClass) && !isFactoryDereference(name)) {
				// If it's a FactoryBean, we want to look at what it creates, not the factory class.
				FactoryBean factoryBean = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.getObjectType();
			}
			return beanClass;
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Not found -> check parent.
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.getType(name);
			}
			throw ex;
		}
		catch (BeanCreationException ex) {
			// Can only happen when checking FactoryBean.
			logger.debug("Ignoring BeanCreationException on FactoryBean type check", ex);
			return null;
		}
	}

	public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		// Check if bean actually exists in this bean factory.
		if (this.singletonCache.containsKey(beanName) || containsBeanDefinition(beanName)) {
			// If found, gather aliases.
			List aliases = new ArrayList();
			synchronized (this.aliasMap) {
				for (Iterator it = this.aliasMap.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					if (entry.getValue().equals(beanName)) {
						aliases.add(entry.getKey());
					}
				}
			}
			return (String[]) aliases.toArray(new String[aliases.size()]);
		}
		else {
			// Not found -> check parent.
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.getAliases(name);
			}
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	//---------------------------------------------------------------------

	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	public void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.notNull(propertyEditor, "PropertyEditor must not be null");
		customEditors.put(requiredType, propertyEditor);
	}
	
	public Map getCustomEditors() {
		return customEditors;
	}

	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		beanPostProcessors.add(beanPostProcessor);
		if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
			hasDestructionAwareBeanPostProcessors = true;
		}
	}
	
	public List getBeanPostProcessors() {
		return beanPostProcessors;
	}
	
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return hasDestructionAwareBeanPostProcessors;
	}
	
	protected BeanWrapper createBeanWrapper(Object beanInstance) {
		return (beanInstance != null) ? new BeanWrapperImpl(beanInstance) : new BeanWrapperImpl();
	}
	
	protected void initBeanWrapper(BeanWrapper bw) {
		for (Iterator it = customEditors.keySet().iterator(); it.hasNext();) {
			Class clazz = (Class)it.next();
			bw.registerCustomEditor(clazz, (PropertyEditor)customEditors.get(clazz));
		}
	}

	public void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException {
		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.hasText(alias, "Alias must not be empty");
		if (logger.isDebugEnabled()) {
			logger.debug("Registering alias '" + alias + "' for bean with name '" + beanName + "'");
		}
		synchronized (aliasMap) {
			Object registeredName = aliasMap.get(alias);
			if (registeredName != null) {
				throw new BeanDefinitionStoreException("Cannot register alias '" + alias + "' for bean name '" + beanName + "': it's already registered for bean name '" + registeredName + "'");
			}
			aliasMap.put(alias, beanName);
		}
	}

	public void registerSingleton(String beanName, Object singletonObject) throws BeanDefinitionStoreException {
		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (singletonCache) {
			Object oldObject = singletonCache.get(beanName);
			if (oldObject != null) {
				throw new BeanDefinitionStoreException("Could not register object [" + singletonObject + "] under bean name '" + beanName + "': there's already object [" + oldObject + " bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}
	
	protected void addSingleton(String beanName, Object singletonObject) {
		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		singletonCache.put(beanName, singletonObject);
	}
	
	protected void removeSingleton(String beanName) {
		Assert.hasText(beanName, "Bean name must not be empty");
		singletonCache.remove(beanName);
		disposableBeans.remove(beanName);
	}
	
	public int getSingletonCount() {
		return singletonCache.size();
	}
	
	public String[] getSingletonNames() {
		return (String[])singletonCache.keySet().toArray(new String[singletonCache.size()]);
	}

	public boolean containsSingleton(String beanName) {
		Assert.hasText(beanName, "Bean name must not be empty");
		return singletonCache.containsKey(beanName);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in factory {" + this + "}");
		}
		singletonCache.clear();
		synchronized (disposableBeans) {
			for (Iterator it = new HashSet(disposableBeans.keySet()).iterator(); it.hasNext(); ) {
				destroyDisposableBean((String)it.next());
			}
		}
	}

	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------
	
	protected boolean isFactoryDereference(String name) {
		return BeanFactoryUtils.isFactoryDereference(name);
	}
	
	protected String transformedBeanName(String name) {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		// handle aliasing
		String canonicalName = (String)aliasMap.get(beanName);
		return canonicalName != null ? canonicalName : beanName;
	}
	
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, boolean includingAncestors) throws BeansException {
		try {
			return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
		} catch (NoSuchBeanDefinitionException ex) {
			if (includingAncestors && getParentBeanFactory() instanceof AbstractBeanFactory) {
				return ((AbstractBeanFactory)getParentBeanFactory()).getMergedBeanDefinition(beanName, true);
			} else {
				throw ex;
			}
		}
	}
	
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) throws BeansException {
		if (bd instanceof RootBeanDefinition) {
			return (RootBeanDefinition)bd;
		} else if (bd instanceof ChildBeanDefinition) {
			ChildBeanDefinition cbd = (ChildBeanDefinition)bd;
			RootBeanDefinition pbd = null;
			if (!beanName.equals(cbd.getParentName())) {
				pbd = getMergedBeanDefinition(cbd.getParentName(), true);
			} else {
				if (getParentBeanFactory() instanceof AbstractBeanFactory) {
					AbstractBeanFactory parentFactory = (AbstractBeanFactory)getParentBeanFactory();
					pbd = parentFactory.getMergedBeanDefinition(cbd.getParentName(), true);
				} else {
					throw new NoSuchBeanDefinitionException(cbd.getParentName(), "Parent name '" + cbd.getParentName() + "' is equal to bean name '" + beanName + "' - cannot be resolved without an AbstractBeanFactory parent");
				}
			}
			// deep copy with overridden values
			RootBeanDefinition rbd = new RootBeanDefinition(pbd);
			rbd.overrideFrom(cbd);
			return rbd;
		} else {
			throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName, "Definition is neither a RootBeanDefinition nor a ChildBeanDefinition");
		}
	}
	
	protected void checkMergedBeanDefinition(RootBeanDefinition mergedBeanDefinition, String beanName, Class requiredType, Object[] args) throws BeansException {
		// check if bean definition is not abstract
		if (mergedBeanDefinition.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
		// Check if required type can match according to the bean definition. This is only possible at this early stage for conventional beans!
		if (mergedBeanDefinition.hasBeanClass()) {
			Class beanClass = mergedBeanDefinition.getBeanClass();
			if (requiredType != null && mergedBeanDefinition.getFactoryMethodName() == null && !FactoryBean.class.isAssignableFrom(beanClass) && !requiredType.isAssignableFrom(beanClass)) {
				throw new BeanNotOfRequiredTypeException(beanName, requiredType, beanClass);
			}
		}
		// Check validity of the usage of the args parameter. This can only be used for prototypes constructed via a factory method.
		if (args != null) {
			if (mergedBeanDefinition.isSingleton()) {
				throw new BeanDefinitionStoreException("Cannot specify arguments in the getBean() method when referring to a singleton bean definition");
			} else if (mergedBeanDefinition.getFactoryMethodName() == null) {
				throw new BeanDefinitionStoreException("Can only specify arguments in the getBean() method in conjunction with a factory method");
			}
		}
	}
	
	protected Object getObjectForSharedInstance(String name, Object beanInstance) throws BeansException {
		String beanName = transformedBeanName(name);
		// Don't let calling code try to dereference the bean factory if the bean isn't a factory.
		if (isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
		}
		// Now we have the bean instance, which may be a normal bean or a FactoryBean. If it's a FactoryBean, we use it to create a bean instance, unless the caller actually wants a reference to the factory.
		if (beanInstance instanceof FactoryBean) {
			if (!isFactoryDereference(name)) {
				// Return bean instance from factory.
				FactoryBean factory = (FactoryBean)beanInstance;
				if (logger.isDebugEnabled()) {
					logger.debug("Bean with name '" + beanName + "' is a factory bean");
				}
				try {
					beanInstance = factory.getObject();
				} catch (Exception ex) {
					throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
				}
				if (beanInstance == null) {
					throw new FactoryBeanNotInitializedException(beanName, "FactoryBean returned null object: " + "probably not fully initialized (maybe due to circular bean reference)");
				}
			} else {
	 			// The user wants the factory itself.
				if (logger.isDebugEnabled()) {
					logger.debug("Calling code asked for FactoryBean instance for name '" + beanName + "'");
				}
			}
		}
		return beanInstance;
	}
	
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		try {
			Object beanInstance = singletonCache.get(beanName);
			if (beanInstance == CURRENTLY_IN_CREATION) {
				throw new BeanCurrentlyInCreationException(beanName);
			}
			if (beanInstance != null) {
				return (beanInstance instanceof FactoryBean);
			} else {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				return (bd.hasBeanClass() && FactoryBean.class.equals(bd.getBeanClass()));
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// Not found -> check parent.
			if (parentBeanFactory != null) {
				return parentBeanFactory.isSingleton(name);
			}
			throw ex;
		}
	}
	
	protected void registerDisposableBeanIfNecessary(final String beanName, final Object bean, final RootBeanDefinition mergedBeanDefinition) {
		if (mergedBeanDefinition.isSingleton()) {
			final boolean isDisposableBean = (bean instanceof DisposableBean);
			final boolean hasDestroyMethod = (mergedBeanDefinition.getDestroyMethodName() != null);
			if (isDisposableBean || hasDestroyMethod || hasDestructionAwareBeanPostProcessors()) {
				// Determine unique key for registration of disposable bean
				int counter = 1;
				String id = beanName;
				while (disposableBeans.containsKey(id)) {
					counter++;
					id = beanName + "#" + counter;
				}
				// Register a DisposableBean implementation that performs all destruction work for the given bean: DestructionAwareBeanPostProcessors, DisposableBean interface, custom destroy method.
				registerDisposableBean(id, new DisposableBean() {
					public void destroy() throws Exception {
						if (hasDestructionAwareBeanPostProcessors()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Applying DestructionAwareBeanPostProcessors to bean with name '" + beanName + "'");
							}
							for (int i = getBeanPostProcessors().size() - 1; i >= 0; i--) {
								Object beanProcessor = getBeanPostProcessors().get(i);
								if (beanProcessor instanceof DestructionAwareBeanPostProcessor) {
									((DestructionAwareBeanPostProcessor)beanProcessor).postProcessBeforeDestruction(bean, beanName);
								}
							}
						}
						if (isDisposableBean) {
							if (logger.isDebugEnabled()) {
								logger.debug("Invoking destroy() on bean with name '" + beanName + "'");
							}
							((DisposableBean) bean).destroy();
						}
						if (hasDestroyMethod) {
							if (logger.isDebugEnabled()) {
								logger.debug("Invoking custom destroy method on bean with name '" + beanName + "'");
							}
							invokeCustomDestroyMethod(beanName, bean, mergedBeanDefinition.getDestroyMethodName());
						}
					}
				});
			}
			// Register bean as dependent on other beans, if necessary, for correct shutdown order.
			String[] dependsOn = mergedBeanDefinition.getDependsOn();
			if (dependsOn != null) {
				for (int i = 0; i < dependsOn.length; i++) {
					registerDependentBean(dependsOn[i], beanName);
				}
			}
		}
	}
	
	protected void registerDisposableBean(String beanName, DisposableBean bean) {
		disposableBeans.put(beanName, bean);
	}
	
	protected void registerDependentBean(String beanName, String dependentBeanName) {
		synchronized (dependentBeanMap) {
			List dependencies = (List)dependentBeanMap.get(beanName);
			if (dependencies == null) {
				dependencies = new LinkedList();
				dependentBeanMap.put(beanName, dependencies);
			}
			dependencies.add(dependentBeanName);
		}
	}
	
	private void destroyDisposableBean(String beanName) {
		Object disposableBean = disposableBeans.remove(beanName);
		if (disposableBean != null) {
			destroyBean(beanName, disposableBean);
		}
	}
	
	protected void destroyBean(String beanName, Object bean) {
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieving dependent beans for bean '" + beanName + "'");
		}
		List dependencies = (List)dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			for (Iterator it = dependencies.iterator(); it.hasNext(); ) {
				String dependentBeanName = (String)it.next();
				destroyDisposableBean(dependentBeanName);
			}
		}
		if (bean instanceof DisposableBean) {
			try {
				((DisposableBean)bean).destroy();
			} catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}
	}
	
	protected void invokeCustomDestroyMethod(String beanName, Object bean, String destroyMethodName) {
		Method destroyMethod = BeanUtils.findDeclaredMethodWithMinimalParameters(bean.getClass(), destroyMethodName);
		if (destroyMethod == null) {
			logger.error("Couldn't find a destroy method named '" + destroyMethodName + "' on bean with name '" + beanName + "'");
		} else {
			Class[] paramTypes = destroyMethod.getParameterTypes();
			if (paramTypes.length > 1) {
				logger.error("Method '" + destroyMethodName + "' of bean '" + beanName + "' has more than one parameter - not supported as destroy method");
			} else if (paramTypes.length == 1 && !paramTypes[0].equals(boolean.class)) {
				logger.error("Method '" + destroyMethodName + "' of bean '" + beanName + "' has a non-boolean parameter - not supported as destroy method");
			} else {
				Object[] args = new Object[paramTypes.length];
				if (paramTypes.length == 1) {
					args[0] = Boolean.TRUE;
				}
				if (!Modifier.isPublic(destroyMethod.getModifiers())) {
					destroyMethod.setAccessible(true);
				}
				try {
					destroyMethod.invoke(bean, args);
				} catch (InvocationTargetException ex) {
					logger.error("Couldn't invoke destroy method '" + destroyMethodName + "' of bean with name '" + beanName + "'", ex.getTargetException());
				} catch (Throwable ex) {
					logger.error("Couldn't invoke destroy method '" + destroyMethodName + "' of bean with name '" + beanName + "'", ex);
				}
			}
		}
	}

	//---------------------------------------------------------------------
	// Abstract methods to be implemented by concrete subclasses
	//---------------------------------------------------------------------
	
	protected abstract boolean containsBeanDefinition(String beanName);

	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	protected abstract Object createBean(String beanName, RootBeanDefinition mergedBeanDefinition, Object[] args) throws BeansException;

}
