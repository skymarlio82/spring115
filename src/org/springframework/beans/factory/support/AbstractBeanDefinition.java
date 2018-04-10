
package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.util.ClassUtils;

public abstract class AbstractBeanDefinition implements BeanDefinition {

	public static final int AUTOWIRE_NO = 0;

	public static final int AUTOWIRE_BY_NAME     = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
	public static final int AUTOWIRE_BY_TYPE     = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
	public static final int AUTOWIRE_AUTODETECT  = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	public static final int DEPENDENCY_CHECK_NONE    = 0;
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;
	public static final int DEPENDENCY_CHECK_SIMPLE  = 2;
	public static final int DEPENDENCY_CHECK_ALL     = 3;


	private Object beanClass = null;

	private boolean abstractFlag = false;
	private boolean singleton    = true;
	private boolean lazyInit     = false;

	private ConstructorArgumentValues constructorArgumentValues = null;
	private MutablePropertyValues propertyValues                = null;
	private MethodOverrides methodOverrides                     = new MethodOverrides();

	private String initMethodName    = null;
	private String destroyMethodName = null;
	private String factoryMethodName = null;
	private String factoryBeanName   = null;

	private int autowireMode    = AUTOWIRE_NO;
	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	private String[] dependsOn         = null;
	private String resourceDescription = null;

	protected AbstractBeanDefinition() {
		this(null, null);
	}

	protected AbstractBeanDefinition(ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		setConstructorArgumentValues(cargs);
		setPropertyValues(pvs);
	}

	protected AbstractBeanDefinition(AbstractBeanDefinition original) {
		this.beanClass = original.beanClass;
		setAbstract(original.isAbstract());
		setSingleton(original.isSingleton());
		setLazyInit(original.isLazyInit());
		setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
		setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
		setMethodOverrides(new MethodOverrides(original.getMethodOverrides()));
		setInitMethodName(original.getInitMethodName());
		setDestroyMethodName(original.getDestroyMethodName());
		setFactoryMethodName(original.getFactoryMethodName());
		setFactoryBeanName(original.getFactoryBeanName());
		setDependsOn(original.getDependsOn());
		setAutowireMode(original.getAutowireMode());
		setDependencyCheck(original.getDependencyCheck());
		setResourceDescription(original.getResourceDescription());
	}

	public void overrideFrom(AbstractBeanDefinition other) {
		if (other.beanClass != null) {
			this.beanClass = other.beanClass;
		}
		setAbstract(other.isAbstract());
		setSingleton(other.isSingleton());
		setLazyInit(other.isLazyInit());
		getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
		getPropertyValues().addPropertyValues(other.getPropertyValues());
		getMethodOverrides().addOverrides(other.getMethodOverrides());
		if (other.getInitMethodName() != null) {
			setInitMethodName(other.getInitMethodName());
		}
		if (other.getDestroyMethodName() != null) {
			setDestroyMethodName(other.getDestroyMethodName());
		}
		if (other.getFactoryMethodName() != null) {
			setFactoryMethodName(other.getFactoryMethodName());
		}
		if (other.getFactoryBeanName() != null) {
			setFactoryBeanName(other.getFactoryBeanName());
		}
		setDependsOn(other.getDependsOn());
		setAutowireMode(other.getAutowireMode());
		setDependencyCheck(other.getDependencyCheck());
		setResourceDescription(other.getResourceDescription());
	}

	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	public void setBeanClass(Class beanClass) {
		this.beanClass = beanClass;
	}

	public Class getBeanClass() throws IllegalStateException {
		if (!(beanClass instanceof Class)) {
			throw new IllegalStateException("Bean definition does not carry a resolved bean class");
		}
		return (Class)beanClass;
	}

	public void setBeanClassName(String beanClassName) {
		this.beanClass = beanClassName;
	}

	public String getBeanClassName() {
		if (beanClass instanceof Class) {
			return ((Class)beanClass).getName();
		} else {
			return (String)beanClass;
		}
	}

	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	public boolean isAbstract() {
		return abstractFlag;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	public boolean isLazyInit() {
		return lazyInit;
	}

	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = (constructorArgumentValues != null) ? constructorArgumentValues : new ConstructorArgumentValues();
	}
	
	public ConstructorArgumentValues getConstructorArgumentValues() {
		return constructorArgumentValues;
	}
	
	public boolean hasConstructorArgumentValues() {
		return (constructorArgumentValues != null && !constructorArgumentValues.isEmpty());
	}
	
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = (propertyValues != null) ? propertyValues : new MutablePropertyValues();
	}
	
	public MutablePropertyValues getPropertyValues() {
		return propertyValues;
	}

	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = (methodOverrides != null) ? methodOverrides : new MethodOverrides();
	}
	
	public MethodOverrides getMethodOverrides() {
		return methodOverrides;
	}

	public void setInitMethodName(String initMethodName) {
		this.initMethodName = initMethodName;
	}

	public String getInitMethodName() {
		return initMethodName;
	}

	public void setDestroyMethodName(String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	public String getDestroyMethodName() {
		return destroyMethodName;
	}

	public void setFactoryMethodName(String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	public String getFactoryMethodName() {
		return factoryMethodName;
	}

	public void setFactoryBeanName(String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	public String getFactoryBeanName() {
		return factoryBeanName;
	}

	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	public int getAutowireMode() {
		return autowireMode;
	}

	public int getResolvedAutowireMode() {
		if (autowireMode == AUTOWIRE_AUTODETECT) {
			// Work out whether to apply setter autowiring or constructor autowiring. If it has a no-arg constructor it's deemed to be setter autowiring, otherwise we'll try constructor autowiring.
			Constructor[] constructors = getBeanClass().getConstructors();
			for (int i = 0; i < constructors.length; i++) {
				if (constructors[i].getParameterTypes().length == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		} else {
			return autowireMode;
		}
	}

	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	public int getDependencyCheck() {
		return dependencyCheck;
	}

	public void setDependsOn(String[] dependsOn) {
		this.dependsOn = dependsOn;
	}

	public String[] getDependsOn() {
		return dependsOn;
	}

	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}

	public String getResourceDescription() {
		return resourceDescription;
	}

	public void validate() throws BeanDefinitionValidationException {
		if (lazyInit && !singleton) {
			throw new BeanDefinitionValidationException("Lazy initialization is applicable only to singleton beans");
		}
		if (!getMethodOverrides().isEmpty() && getFactoryMethodName() != null) {
			throw new  BeanDefinitionValidationException("Cannot combine static factory method with method overrides: the static factory method must create the instance");
		}
		if (hasBeanClass()) {
			// Check that lookup methods exists
			for (Iterator itr = getMethodOverrides().getOverrides().iterator(); itr.hasNext(); ) {
				MethodOverride mo = (MethodOverride) itr.next();
				validateMethodOverride(mo);
			}
		}
	}

	protected void validateMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		if (!ClassUtils.hasAtLeastOneMethodWithName(getBeanClass(), mo.getMethodName())) {
			throw new BeanDefinitionValidationException("Invalid method override: no method with name '" + mo.getMethodName() + "' on class [" + getBeanClassName() + "]");
		}
	}
}
