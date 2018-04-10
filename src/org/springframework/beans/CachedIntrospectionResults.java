
package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

final class CachedIntrospectionResults {

	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

	private static final Map classCache = Collections.synchronizedMap(new WeakHashMap());
	
	static CachedIntrospectionResults forClass(Class clazz) throws BeansException {
		CachedIntrospectionResults results = null;
		Object value = classCache.get(clazz);
		if (value instanceof Reference) {
			Reference ref = (Reference)value;
			results = (CachedIntrospectionResults)ref.get();
		} else {
			results = (CachedIntrospectionResults)value;
		}
		if (results == null) {
			// can throw BeansException
			results = new CachedIntrospectionResults(clazz);
			boolean cacheSafe = isCacheSafe(clazz);
			if (logger.isDebugEnabled()) {
				logger.debug("Class [" + clazz.getName() + "] is " + (!cacheSafe ? "not " : "") + "cache-safe");
			}
			if (cacheSafe) {
				classCache.put(clazz, results);
			} else {
				classCache.put(clazz, new WeakReference(results));
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Using cached introspection results for class [" + clazz.getName() + "]");
			}
		}
		return results;
	}
	
	private static boolean isCacheSafe(Class clazz) {
		ClassLoader cur = CachedIntrospectionResults.class.getClassLoader();
		ClassLoader target = clazz.getClassLoader();
		if (target == null || cur == target) {
			return true;
		}
		while (cur != null) {
			cur = cur.getParent();
			if (cur == target) {
				return true;
			}
		}
		return false;
	}

	private final BeanInfo beanInfo;
	private final Map propertyDescriptorCache;
	
	private CachedIntrospectionResults(Class clazz) throws BeansException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Getting BeanInfo for class [" + clazz.getName() + "]");
			}
			beanInfo = Introspector.getBeanInfo(clazz);
			// Immediately remove class from Introspector cache, to allow for proper garbage collection on class loader shutdown - we cache it here anyway, in a GC-friendly manner. In contrast to CachedIntrospectionResults, Introspector does not use WeakReferences as values of its WeakHashMap!
			Class classToFlush = clazz;
			do {
				Introspector.flushFromCaches(classToFlush);
				classToFlush = classToFlush.getSuperclass();
			} while (classToFlush != null);
			if (logger.isDebugEnabled()) {
				logger.debug("Caching PropertyDescriptors for class [" + clazz.getName() + "]");
			}
			propertyDescriptorCache = new HashMap();
			// This call is slow so we do it once.
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (int i = 0; i < pds.length; i++) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found property '" + pds[i].getName() + "'" + (pds[i].getPropertyType() != null ? " of type [" + pds[i].getPropertyType().getName() + "]" : "") + (pds[i].getPropertyEditorClass() != null ? "; editor [" + pds[i].getPropertyEditorClass().getName() + "]" : ""));
				}
				// Set methods accessible if declaring class is not public, for example in case of package-protected base classes that define bean properties.
				Method readMethod = pds[i].getReadMethod();
				if (readMethod != null && !Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
					readMethod.setAccessible(true);
				}
				Method writeMethod = pds[i].getWriteMethod();
				if (writeMethod != null && !Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
					writeMethod.setAccessible(true);
				}
				propertyDescriptorCache.put(pds[i].getName(), pds[i]);
			}
		} catch (IntrospectionException ex) {
			throw new FatalBeanException("Cannot get BeanInfo for object of class [" + clazz.getName() + "]", ex);
		}
	}

	BeanInfo getBeanInfo() {
		return beanInfo;
	}

	Class getBeanClass() {
		return beanInfo.getBeanDescriptor().getBeanClass();
	}

	PropertyDescriptor getPropertyDescriptor(String propertyName) {
		return (PropertyDescriptor)propertyDescriptorCache.get(propertyName);
	}
}
