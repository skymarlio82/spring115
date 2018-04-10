
package org.springframework.beans.factory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

public abstract class BeanFactoryUtils {

	public static boolean isFactoryDereference(String name) {
		return name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX);
	}

	public static String transformedBeanName(String name) {
		Assert.notNull(name, "Name must not be null");
		String beanName = name;
		if (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
		}
		return beanName;
	}

	public static int countBeansIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesIncludingAncestors(lbf).length;
	}
	
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf) {
		Set result = new HashSet();
		result.addAll(Arrays.asList(lbf.getBeanDefinitionNames()));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory)lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesIncludingAncestors((ListableBeanFactory)hbf.getParentBeanFactory());
				result.addAll(Arrays.asList(parentResult));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf, Class type) {
		Set result = new HashSet();
		result.addAll(Arrays.asList(lbf.getBeanDefinitionNames(type)));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory)lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesIncludingAncestors((ListableBeanFactory)hbf.getParentBeanFactory(), type);
				result.addAll(Arrays.asList(parentResult));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class type) {
		Set result = new HashSet();
		result.addAll(Arrays.asList(lbf.getBeanNamesForType(type)));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors((ListableBeanFactory)hbf.getParentBeanFactory(), type);
				result.addAll(Arrays.asList(parentResult));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static Map beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class type) throws BeansException {
		Map result = new HashMap();
		result.putAll(lbf.getBeansOfType(type));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map parentResult = beansOfTypeIncludingAncestors((ListableBeanFactory)hbf.getParentBeanFactory(), type);
				for (Iterator it = parentResult.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry entry = (Map.Entry)it.next();
					if (!result.containsKey(entry.getKey())) {
						result.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		return result;
	}

	public static Map beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
		Map result = new HashMap();
		result.putAll(lbf.getBeansOfType(type, includePrototypes, includeFactoryBeans));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory)lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map parentResult = beansOfTypeIncludingAncestors((ListableBeanFactory) hbf.getParentBeanFactory(), type, includePrototypes, includeFactoryBeans);
				for (Iterator it = parentResult.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry entry = (Map.Entry)it.next();
					if (!result.containsKey(entry.getKey())) {
						result.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		return result;
	}

	public static Object beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class type) throws BeansException {
		Map beansOfType = beansOfTypeIncludingAncestors(lbf, type);
		if (beansOfType.size() == 1) {
			return beansOfType.values().iterator().next();
		} else {
			throw new NoSuchBeanDefinitionException(type, "Expected single bean but found " + beansOfType.size());
		}
	}

	public static Object beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
		Map beansOfType = beansOfTypeIncludingAncestors(lbf, type, includePrototypes, includeFactoryBeans);
		if (beansOfType.size() == 1) {
			return beansOfType.values().iterator().next();
		} else {
			throw new NoSuchBeanDefinitionException(type, "Expected single bean but found " + beansOfType.size());
		}
	}

	public static Object beanOfType(ListableBeanFactory lbf, Class type) throws BeansException {
		Map beansOfType = lbf.getBeansOfType(type);
		if (beansOfType.size() == 1) {
			return beansOfType.values().iterator().next();
		} else {
			throw new NoSuchBeanDefinitionException(type, "Expected single bean but found " + beansOfType.size());
		}
	}

	public static Object beanOfType(ListableBeanFactory lbf, Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
		Map beansOfType = lbf.getBeansOfType(type, includePrototypes, includeFactoryBeans);
		if (beansOfType.size() == 1) {
			return beansOfType.values().iterator().next();
		} else {
			throw new NoSuchBeanDefinitionException(type, "Expected single bean but found " + beansOfType.size());
		}
	}
}
