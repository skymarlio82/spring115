
package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public abstract class BeanUtils {
	
	public static Object instantiateClass(Class clazz) throws BeansException {
		if (clazz.isInterface()) {
			throw new FatalBeanException("Class [" + clazz.getName() + "] cannot be instantiated: it is an interface");
		}
		try {
			return instantiateClass(clazz.getDeclaredConstructor((Class[])null), null);
		} catch (NoSuchMethodException ex) {
			throw new FatalBeanException("Could not instantiate class [" + clazz.getName() + "]: no default constructor found", ex);
		}
	}
	
	public static Object instantiateClass(Constructor ctor, Object[] args) throws BeansException {
		try {
			if (!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) {
				ctor.setAccessible(true);
			}
			return ctor.newInstance(args);
		} catch (InstantiationException ex) {
			throw new FatalBeanException("Could not instantiate class [" + ctor.getDeclaringClass().getName() + "]: Is it an abstract class?", ex);
		} catch (IllegalAccessException ex) {
			throw new FatalBeanException("Could not instantiate class [" + ctor.getDeclaringClass().getName() + "]: Has the class definition changed? Is the constructor accessible?", ex);
		} catch (IllegalArgumentException ex) {
			throw new FatalBeanException("Could not instantiate class [" + ctor.getDeclaringClass().getName() + "]: illegal args for constructor", ex);
		} catch (InvocationTargetException ex) {
			throw new FatalBeanException("Could not instantiate class [" + ctor.getDeclaringClass().getName() + "]; constructor threw exception", ex.getTargetException());
		}
	}
	
	public static Method findMethod(Class clazz, String methodName, Class[] paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ex) {
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}
	
	public static Method findDeclaredMethod(Class clazz, String methodName, Class[] paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}
	
	public static Method findMethodWithMinimalParameters(Class clazz, String methodName) {
		Method[] methods = clazz.getMethods();
		Method targetMethod = null;
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(methodName)) {
				if (targetMethod == null || methods[i].getParameterTypes().length < targetMethod.getParameterTypes().length) {
					targetMethod = methods[i];
				}
			}
		}
		if (targetMethod != null) {
			return targetMethod;
		} else {
			return findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
	}
	
	public static Method findDeclaredMethodWithMinimalParameters(Class clazz, String methodName) {
		Method[] methods = clazz.getDeclaredMethods();
		Method targetMethod = null;
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(methodName)) {
				if (targetMethod == null || methods[i].getParameterTypes().length < targetMethod.getParameterTypes().length) {
					targetMethod = methods[i];
				}
			}
		}
		if (targetMethod != null) {
			return targetMethod;
		} else {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
			}
			return null;
		}
	}
	
	public static boolean isAssignable(Class type, Object value) {
		return (value != null) ? isAssignable(type, value.getClass()) : !type.isPrimitive();
	}
	
	public static boolean isAssignable(Class targetType, Class valueType) {
		return (targetType.isAssignableFrom(valueType)) || 
			(targetType.equals(boolean.class) && valueType.equals(Boolean.class)) || 
			(targetType.equals(byte.class) && valueType.equals(Byte.class)) || 
			(targetType.equals(char.class) && valueType.equals(Character.class)) || 
			(targetType.equals(short.class) && valueType.equals(Short.class)) || 
			(targetType.equals(int.class) && valueType.equals(Integer.class)) || 
			(targetType.equals(long.class) && valueType.equals(Long.class)) || 
			(targetType.equals(float.class) && valueType.equals(Float.class)) || 
			(targetType.equals(double.class) && valueType.equals(Double.class));
	}
	
	public static boolean isSimpleProperty(Class clazz) {
		return clazz.isPrimitive() || 
			isPrimitiveArray(clazz) || 
			isPrimitiveWrapperArray(clazz) || 
			clazz.equals(String.class) || 
			clazz.equals(String[].class) || 
			clazz.equals(Class.class) || 
			clazz.equals(Class[].class);
	}
	
	public static boolean isPrimitiveArray(Class clazz) {
		return boolean[].class.equals(clazz) || 
			byte[].class.equals(clazz) || 
			char[].class.equals(clazz) || 
			short[].class.equals(clazz) || 
			int[].class.equals(clazz) || 
			long[].class.equals(clazz) || 
			float[].class.equals(clazz) || 
			double[].class.equals(clazz);
	}
	
	public static boolean isPrimitiveWrapperArray(Class clazz) {
		return Boolean[].class.equals(clazz) || 
			Byte[].class.equals(clazz) || 
			Character[].class.equals(clazz) || 
			Short[].class.equals(clazz) || 
			Integer[].class.equals(clazz) || 
			Long[].class.equals(clazz) || 
			Float[].class.equals(clazz) || 
			Double[].class.equals(clazz);
	}
	
	public static void copyProperties(Object source, Object target) throws IllegalArgumentException, BeansException {
		copyProperties(source, target, null);
	}
	
	public static void copyProperties(Object source, Object target, String[] ignoreProperties) throws IllegalArgumentException, BeansException {
		if (source == null || target == null) {
			throw new IllegalArgumentException("Source and target must not be null");
		}
		List ignoreList = (ignoreProperties != null) ? Arrays.asList(ignoreProperties) : null;
		BeanWrapper sourceBw = new BeanWrapperImpl(source);
		BeanWrapper targetBw = new BeanWrapperImpl(target);
		MutablePropertyValues values = new MutablePropertyValues();
		for (int i = 0; i < sourceBw.getPropertyDescriptors().length; i++) {
			PropertyDescriptor sourceDesc = sourceBw.getPropertyDescriptors()[i];
			String name = sourceDesc.getName();
			PropertyDescriptor targetDesc = targetBw.getPropertyDescriptor(name);
			if (targetDesc.getWriteMethod() != null && targetDesc.getReadMethod() != null && (ignoreProperties == null || !ignoreList.contains(name))) {
				values.addPropertyValue(new PropertyValue(name, sourceBw.getPropertyValue(name)));
			}
		}
		targetBw.setPropertyValues(values);
	}
	
	public static PropertyDescriptor[] getPropertyDescriptors(Class clazz) throws BeansException {
		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getBeanInfo().getPropertyDescriptors();
	}
	
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		PropertyDescriptor[] pds = getPropertyDescriptors(method.getDeclaringClass());
		for (int i = 0; i < pds.length; i++) {
			if (method.equals(pds[i].getReadMethod()) || method.equals(pds[i].getWriteMethod())) {
				return pds[i];
			}
		}
		return null;
	}
}
