
package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

	private boolean allowBeanDefinitionOverriding = true;

	@SuppressWarnings("rawtypes")
	private final Map beanDefinitionMap    = new HashMap();
	@SuppressWarnings("rawtypes")
	private final List beanDefinitionNames = new ArrayList();

	public DefaultListableBeanFactory() {
		super();
	}
	
	public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}
	
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}
	
	// ---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	// ---------------------------------------------------------------------

	public boolean containsBeanDefinition(String beanName) {
		return beanDefinitionMap.containsKey(beanName);
	}

	public int getBeanDefinitionCount() {
		return beanDefinitionMap.size();
	}

	public String[] getBeanDefinitionNames() {
		return (String[])beanDefinitionNames.toArray(new String[beanDefinitionNames.size()]);
	}
	
	public String[] getBeanDefinitionNames(Class type) {
		List matches = new ArrayList();
		Iterator it = this.beanDefinitionNames.iterator();
		while (it.hasNext()) {
			String beanName = (String) it.next();
			if (isBeanDefinitionTypeMatch(beanName, type)) {
				matches.add(beanName);
			}
		}
		return (String[])matches.toArray(new String[matches.size()]);
	}

	public String[] getBeanNamesForType(Class type) {
		List beanNames = doGetBeanNamesForType(type, true, true);
		return (String[])beanNames.toArray(new String[beanNames.size()]);
	}

	public Map getBeansOfType(Class type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
		List beanNames = doGetBeanNamesForType(type, includePrototypes, includeFactoryBeans);
		Map result = CollectionFactory.createLinkedMapIfPossible(beanNames.size());
		for (Iterator it = beanNames.iterator(); it.hasNext();) {
			String beanName = (String) it.next();
			try {
				result.put(beanName, getBean(beanName));
			} catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to currently created bean '" + beanName + "'", ex);
					}
					// Ignore: indicates a circular reference when autowiring constructors. We want to find matches other than the currently created bean itself.
				} else {
					throw ex;
				}
			}
		}
		return result;
	}
	
	protected List doGetBeanNamesForType(Class type, boolean includePrototypes, boolean includeFactoryBeans) {
		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		List result = new ArrayList();
		// Check all bean definitions.
		Iterator it = this.beanDefinitionNames.iterator();
		while (it.hasNext()) {
			String beanName = (String) it.next();
			RootBeanDefinition rbd = getMergedBeanDefinition(beanName, false);
			// Only check bean definition if it is complete.
			if (!rbd.isAbstract() && rbd.hasBeanClass()) {
				// In case of FactoryBean, match object created by FactoryBean.
				if (FactoryBean.class.isAssignableFrom(rbd.getBeanClass()) && !isFactoryType) {
					if (includeFactoryBeans && (includePrototypes || isSingleton(beanName)) && isBeanTypeMatch(beanName, type)) {
						result.add(beanName);
					}
				} else {
					// If type to match is FactoryBean, match FactoryBean itself. Else, match bean instance.
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					if ((includePrototypes || rbd.isSingleton()) && (type == null || type.isAssignableFrom(rbd.getBeanClass()))) {
						result.add(beanName);
					}
				}
			}
		}
		// Check singletons too, to catch manually registered singletons.
		String[] singletonNames = getSingletonNames();
		for (int i = 0; i < singletonNames.length; i++) {
			String beanName = singletonNames[i];
			// Only check if manually registered.
			if (!containsBeanDefinition(beanName)) {
				// In case of FactoryBean, match object created by FactoryBean.
				if (isFactoryBean(beanName) && !isFactoryType) {
					if (includeFactoryBeans && (includePrototypes || isSingleton(beanName)) && isBeanTypeMatch(beanName, type)) {
						result.add(beanName);
					}
				} else {
					// If type to match is FactoryBean, match FactoryBean itself. Else, match bean instance.
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					if (isBeanTypeMatch(beanName, type)) {
						result.add(beanName);
					}
				}
			}
		}
		return result;
	}

	private boolean isBeanTypeMatch(String beanName, Class type) {
		if (type == null) {
			return true;
		}
		Class beanType = getType(beanName);
		return (beanType != null && type.isAssignableFrom(beanType));
	}

	private boolean isBeanDefinitionTypeMatch(String beanName, Class type) {
		if (type == null) {
			return true;
		}
		RootBeanDefinition rbd = getMergedBeanDefinition(beanName, false);
		return (rbd.hasBeanClass() && type.isAssignableFrom(rbd.getBeanClass()));
	}

	// ---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	// ---------------------------------------------------------------------

	public void preInstantiateSingletons() throws BeansException {
		if (logger.isInfoEnabled()) {
			logger.info("Pre-instantiating singletons in factory [" + this + "]");
		}
		try {
			logger.info("Start to try to Pre-instantiating each bean in 'beanDefinitionNames' ...");
			for (Iterator it = beanDefinitionNames.iterator(); it.hasNext(); ) {
				String beanName = (String)it.next();
				logger.debug("'beanName' in the object of 'beanDefinitionNames' == " + beanName);
				if (containsBeanDefinition(beanName)) {
					RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
					if (bd.hasBeanClass() && !bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
						if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
							FactoryBean factory = (FactoryBean)getBean(FACTORY_BEAN_PREFIX + beanName);
							if (factory.isSingleton()) {
								getBean(beanName);
							}
						} else {
							getBean(beanName);
						}
					}
				}
			}
		} catch (BeansException ex) {
			// destroy already created singletons to avoid dangling resources
			try {
				destroySingletons();
			} catch (Throwable ex2) {
				logger.error("Pre-instantiating singletons failed, " + "and couldn't destroy already created singletons", ex2);
			}
			throw ex;
		}
	}

	// ---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry interface
	// ---------------------------------------------------------------------

	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "Bean definition must not be null");
		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition)beanDefinition).validate();
			} catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, "Validation of bean definition with name failed", ex);
			}
		}
		Object oldBeanDefinition = beanDefinitionMap.get(beanName);
		if (oldBeanDefinition != null) {
			if (!allowBeanDefinitionOverriding) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, "Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName + "': there's already [" + oldBeanDefinition + "] bound");
			} else {
				if (logger.isInfoEnabled()) {
					logger.info("Overriding bean definition for bean '" + beanName + "': replacing [" + oldBeanDefinition + "] with [" + beanDefinition + "]");
				}
			}
		} else {
			beanDefinitionNames.add(beanName);
		}
		beanDefinitionMap.put(beanName, beanDefinition);
		logger.debug("SUCCESSFUL register the beanDefinition object - " + beanName + " in the 'beanDefinitionMap' ...");
		// Remove corresponding bean from singleton cache, if any. Shouldn't usually be necessary, rather just meant for overriding a context's default beans (e.g. the default StaticMessageSource in a StaticApplicationContext).
		removeSingleton(beanName);
	}

	public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
		BeanDefinition bd = (BeanDefinition)beanDefinitionMap.get(beanName);
		if (bd == null) {
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
		return bd;
	}

	protected Map findMatchingBeans(Class requiredType) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this, requiredType);
	}

	public String toString() {
		return getClass().getName() + " defining beans [" + StringUtils.arrayToDelimitedString(getBeanDefinitionNames(), ",") + "]; " + ((getParentBeanFactory() == null) ? "root of BeanFactory hierarchy" : "parent: " + getParentBeanFactory());
	}
}
