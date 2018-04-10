
package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.MethodOverrides;
import org.springframework.beans.factory.support.ReplaceOverride;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

public class DefaultXmlBeanDefinitionParser implements XmlBeanDefinitionParser {

	public static final String BEAN_NAME_DELIMITERS = ",; ";
	
	public static final String TRUE_VALUE          = "true";
	public static final String DEFAULT_VALUE       = "default";
	public static final String DESCRIPTION_ELEMENT = "description";

	public static final String AUTOWIRE_BY_NAME_VALUE     = "byName";
	public static final String AUTOWIRE_BY_TYPE_VALUE     = "byType";
	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";
	public static final String AUTOWIRE_AUTODETECT_VALUE  = "autodetect";

	public static final String DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE     = "all";
	public static final String DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE  = "simple";
	public static final String DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE = "objects";

	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE        = "default-lazy-init";
	public static final String DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE = "default-dependency-check";
	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE         = "default-autowire";

	public static final String IMPORT_ELEMENT     = "import";
	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String BEAN_ELEMENT     = "bean";
	public static final String ID_ATTRIBUTE     = "id";
	public static final String NAME_ATTRIBUTE   = "name";
	public static final String PARENT_ATTRIBUTE = "parent";

	public static final String CLASS_ATTRIBUTE            = "class";
	public static final String ABSTRACT_ATTRIBUTE         = "abstract";
	public static final String SINGLETON_ATTRIBUTE        = "singleton";
	public static final String LAZY_INIT_ATTRIBUTE        = "lazy-init";
	public static final String AUTOWIRE_ATTRIBUTE         = "autowire";
	public static final String DEPENDENCY_CHECK_ATTRIBUTE = "dependency-check";
	public static final String DEPENDS_ON_ATTRIBUTE       = "depends-on";
	public static final String INIT_METHOD_ATTRIBUTE      = "init-method";
	public static final String DESTROY_METHOD_ATTRIBUTE   = "destroy-method";
	public static final String FACTORY_METHOD_ATTRIBUTE   = "factory-method";
	public static final String FACTORY_BEAN_ATTRIBUTE     = "factory-bean";

	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
	public static final String INDEX_ATTRIBUTE         = "index";
	public static final String TYPE_ATTRIBUTE          = "type";
	public static final String PROPERTY_ELEMENT        = "property";
	public static final String LOOKUP_METHOD_ELEMENT   = "lookup-method";

	public static final String REPLACED_METHOD_ELEMENT  = "replaced-method";
	public static final String REPLACER_ATTRIBUTE       = "replacer";
	public static final String ARG_TYPE_ELEMENT         = "arg-type";
	public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

	public static final String REF_ELEMENT          = "ref";
	public static final String IDREF_ELEMENT        = "idref";
	public static final String BEAN_REF_ATTRIBUTE   = "bean";
	public static final String LOCAL_REF_ATTRIBUTE  = "local";
	public static final String PARENT_REF_ATTRIBUTE = "parent";

	public static final String LIST_ELEMENT  = "list";
	public static final String SET_ELEMENT   = "set";
	public static final String MAP_ELEMENT   = "map";
	public static final String ENTRY_ELEMENT = "entry";
	public static final String KEY_ATTRIBUTE = "key";
	public static final String PROPS_ELEMENT = "props";
	public static final String PROP_ELEMENT  = "prop";
	public static final String VALUE_ELEMENT = "value";
	public static final String NULL_ELEMENT  = "null";

	protected final Log logger = LogFactory.getLog(getClass());

	private BeanDefinitionReader beanDefinitionReader = null;
	
	private Resource resource = null;
	
	private String defaultLazyInit        = null;
	private String defaultDependencyCheck = null;
	private String defaultAutowire        = null;

	public int registerBeanDefinitions(BeanDefinitionReader reader, Document doc, Resource resource) throws BeansException {
		beanDefinitionReader = reader;
		this.resource = resource;
		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();
		defaultLazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		defaultDependencyCheck = root.getAttribute(DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE);
		defaultAutowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		if (logger.isDebugEnabled()) {
			logger.debug("Default lazy init '" + defaultLazyInit + "'");
			logger.debug("Default dependency check '" + defaultDependencyCheck + "'");
			logger.debug("Default autowire '" + defaultAutowire + "'");
		}
		NodeList nl = root.getChildNodes();
		logger.debug("The length of NodeList from XML document root == " + nl.getLength());
		int beanDefinitionCounter = 0;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element ele = (Element)node;
				if (IMPORT_ELEMENT.equals(node.getNodeName())) {
					importBeanDefinitionResource(ele);
				} else if (BEAN_ELEMENT.equals(node.getNodeName())) {
					beanDefinitionCounter++;
					BeanDefinitionHolder bdHolder = parseBeanDefinition(ele);
					BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, beanDefinitionReader.getBeanFactory());
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Found " + beanDefinitionCounter + " <" + BEAN_ELEMENT + "> elements defining beans");
		}
		return beanDefinitionCounter;
	}

	protected BeanDefinitionReader getBeanDefinitionReader() {
		return beanDefinitionReader;
	}

	protected String getDefaultLazyInit() {
		return defaultLazyInit;
	}

	protected String getDefaultDependencyCheck() {
		return defaultDependencyCheck;
	}

	protected String getDefaultAutowire() {
		return defaultAutowire;
	}

	protected Resource getResource() {
		return resource;
	}
	
	protected void importBeanDefinitionResource(Element ele) {
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		Resource relativeResource = null;
		if (ResourceUtils.isUrl(location)) {
			ResourceLoader resourceLoader = beanDefinitionReader.getResourceLoader();
			if (resourceLoader == null) {
				throw new BeanDefinitionStoreException("Cannot import bean definitions from location [" + location + "]: no resource loader available");
			}
			relativeResource = resourceLoader.getResource(location);
		} else {
			try {
				relativeResource = resource.createRelative(location);
			} catch (IOException ex) {
				throw new BeanDefinitionStoreException("Invalid relative resource location [" + location + "] to import bean definitions from", ex);
			}
		}
		beanDefinitionReader.loadBeanDefinitions(relativeResource);
	}
	
	protected BeanDefinitionHolder parseBeanDefinition(Element ele) {
		String id = ele.getAttribute(ID_ATTRIBUTE);
		System.out.println("XML Element - bean.id = " + id);
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		System.out.println("XML Element - bean.name = " + nameAttr);
		List aliases = new ArrayList();
		if (StringUtils.hasLength(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, BEAN_NAME_DELIMITERS);
			aliases.addAll(Arrays.asList(nameArr));
		}
		String beanName = id;
		if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
			beanName = (String)aliases.remove(0);
			if (logger.isDebugEnabled()) {
				logger.debug("No XML 'id' specified - using '" + beanName + "' as bean name and " + aliases + " as aliases");
			}
		}
		BeanDefinition beanDefinition = parseBeanDefinition(ele, beanName);
		if (!StringUtils.hasText(beanName) && beanDefinition instanceof AbstractBeanDefinition) {
			beanName = BeanDefinitionReaderUtils.generateBeanName((AbstractBeanDefinition)beanDefinition, beanDefinitionReader.getBeanFactory());
			if (logger.isDebugEnabled()) {
				logger.debug("Neither XML 'id' nor 'name' specified - " + "using generated bean name [" + beanName + "]");
			}
		}
		String[] aliasesArray = (String[])aliases.toArray(new String[aliases.size()]);
		return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
	}
	
	protected BeanDefinition parseBeanDefinition(Element ele, String beanName) {
		String className = null;
		if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
			className = ele.getAttribute(CLASS_ATTRIBUTE);
			System.out.println("XML Element - bean.class = " + className);
		}
		String parent = null;
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
			System.out.println("XML Element - bean.parent = " + parent);
		}
		try {
			ConstructorArgumentValues cargs = getConstructorArgSubElements(ele, beanName);
			MutablePropertyValues pvs = getPropertyValueSubElements(ele, beanName);
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(className, parent, cargs, pvs, beanDefinitionReader.getBeanClassLoader());
			if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
				String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
				bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, BEAN_NAME_DELIMITERS));
			}
			if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
				bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
			}
			if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
				bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
			}
			String dependencyCheck = ele.getAttribute(DEPENDENCY_CHECK_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(dependencyCheck)) {
				dependencyCheck = this.defaultDependencyCheck;
			}
			bd.setDependencyCheck(getDependencyCheck(dependencyCheck));
			String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(autowire)) {
				autowire = this.defaultAutowire;
			}
			bd.setAutowireMode(getAutowireMode(autowire));
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			if (!initMethodName.equals("")) {
				bd.setInitMethodName(initMethodName);
			}
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			if (!destroyMethodName.equals("")) {
				bd.setDestroyMethodName(destroyMethodName);
			}
			getLookupOverrideSubElements(ele, beanName, bd.getMethodOverrides());
			getReplacedMethodSubElements(ele, beanName, bd.getMethodOverrides());
			bd.setResourceDescription(this.resource.getDescription());
			if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
				bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
			}
			if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
				bd.setSingleton(TRUE_VALUE.equals(ele.getAttribute(SINGLETON_ATTRIBUTE)));
			}
			String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(lazyInit) && bd.isSingleton()) {
				// just apply default to singletons, as lazy-init has no meaning for prototypes
				lazyInit = this.defaultLazyInit;
			}
			bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
			return bd;
		} catch (ClassNotFoundException ex) {
			throw new BeanDefinitionStoreException(this.resource, beanName, "Bean class [" + className + "] not found", ex);
		} catch (NoClassDefFoundError err) {
			throw new BeanDefinitionStoreException(this.resource, beanName, "Class that bean class [" + className + "] depends on not found", err);
		}
	}
	
	protected ConstructorArgumentValues getConstructorArgSubElements(Element beanEle, String beanName) throws ClassNotFoundException {
		NodeList nl = beanEle.getChildNodes();
		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && CONSTRUCTOR_ARG_ELEMENT.equals(node.getNodeName())) {
				parseConstructorArgElement((Element)node, beanName, cargs);
			}
		}
		return cargs;
	}
	
	protected MutablePropertyValues getPropertyValueSubElements(Element beanEle, String beanName) {
		NodeList nl = beanEle.getChildNodes();
		MutablePropertyValues pvs = new MutablePropertyValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && PROPERTY_ELEMENT.equals(node.getNodeName())) {
				parsePropertyElement((Element)node, beanName, pvs);
			}
		}
		return pvs;
	}
	
	protected void getLookupOverrideSubElements(Element beanEle, String beanName, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && LOOKUP_METHOD_ELEMENT.equals(node.getNodeName())) {
				Element ele = (Element) node;
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				overrides.addOverride(new LookupOverride(methodName, beanRef));
			}
		}
	}

	protected void getReplacedMethodSubElements(Element beanEle, String beanName, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && REPLACED_METHOD_ELEMENT.equals(node.getNodeName())) {
				Element replacedMethodEle = (Element) node;
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
				// Look for arg-type match elements
				NodeList argTypeNodes = replacedMethodEle.getElementsByTagName(ARG_TYPE_ELEMENT);
				for (int j = 0; j < argTypeNodes.getLength(); j++) {
					Element argTypeEle = (Element) argTypeNodes.item(j);
					replaceOverride.addTypeIdentifier(argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE));
				}
				overrides.addOverride(replaceOverride);
			}
		}
	}
	
	protected void parseConstructorArgElement(Element ele, String beanName, ConstructorArgumentValues cargs) throws DOMException, ClassNotFoundException {
		Object val = getPropertyValue(ele, beanName, null);
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		System.out.println("XML Element - bean.constructor-arg.index = " + indexAttr);
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		System.out.println("XML Element - bean.constructor-arg.type = " + typeAttr);
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					throw new BeanDefinitionStoreException(this.resource, beanName, "'index' cannot be lower than 0");
				}
				if (StringUtils.hasLength(typeAttr)) {
					cargs.addIndexedArgumentValue(index, val, typeAttr);
				} else {
					cargs.addIndexedArgumentValue(index, val);
				}
			} catch (NumberFormatException ex) {
				throw new BeanDefinitionStoreException(this.resource, beanName, "Attribute 'index' of tag 'constructor-arg' must be an integer");
			}
		} else {
			if (StringUtils.hasLength(typeAttr)) {
				cargs.addGenericArgumentValue(val, typeAttr);
			} else {
				cargs.addGenericArgumentValue(val);
			}
		}
	}
	
	protected void parsePropertyElement(Element ele, String beanName, MutablePropertyValues pvs) {
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		System.out.println("XML Element - bean.property.name = " + propertyName);
		if (!StringUtils.hasLength(propertyName)) {
			throw new BeanDefinitionStoreException(resource, beanName, "Tag 'property' must have a 'name' attribute");
		}
		if (pvs.contains(propertyName)) {
			throw new BeanDefinitionStoreException(resource, beanName, "Multiple 'property' definitions for property '" + propertyName + "'");
		}
		Object val = getPropertyValue(ele, beanName, propertyName);
		pvs.addPropertyValue(propertyName, val);
	}
	
	protected Object getPropertyValue(Element ele, String beanName, String propertyName) {
		// <property> or <constructor-arg> should only have one element child: <value>, <ref>, <collection>
		NodeList nl = ele.getChildNodes();
		Element valueRefOrCollectionElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element candidateEle = (Element)nl.item(i);
				if (DESCRIPTION_ELEMENT.equals(candidateEle.getTagName())) {
					// keep going: we don't use this value for now
				} else {
					// child element is what we're looking for
					valueRefOrCollectionElement = candidateEle;
				}
			}
		}
		if (valueRefOrCollectionElement == null) {
			String elementName = (propertyName != null) ? "<property> element for property '" + propertyName + "'" : "<constructor-arg> element";
			throw new BeanDefinitionStoreException(resource, beanName, elementName + " must have a subelement like <value>, <ref> of <collection> ...");
		}
		return parsePropertySubelement(valueRefOrCollectionElement, beanName);
	}
	
	protected Object parsePropertySubelement(Element ele, String beanName) {
		if (ele.getTagName().equals(BEAN_ELEMENT)) {
			return parseBeanDefinition(ele);
		} else if (ele.getTagName().equals(REF_ELEMENT)) {
			// a generic reference to any name of any bean
			String beanRef = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			System.out.println("<ref> element, bean = " + beanRef);
			if (!StringUtils.hasLength(beanRef)) {
				// a reference to the id of another bean in the same XML file
				beanRef = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
				if (!StringUtils.hasLength(beanRef)) {
					// a reference to the id of another bean in the same XML file
					beanRef = ele.getAttribute(PARENT_REF_ATTRIBUTE);
					if (!StringUtils.hasLength(beanRef)) {
						throw new BeanDefinitionStoreException(resource, beanName, "'bean', 'local' or 'parent' is required for a reference");
					}
					return new RuntimeBeanReference(beanRef, true);
				}
			}
			return new RuntimeBeanReference(beanRef);
		} else if (ele.getTagName().equals(IDREF_ELEMENT)) {
			// a generic reference to any name of any bean
			String beanRef = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			if (!StringUtils.hasLength(beanRef)) {
				// a reference to the id of another bean in the same XML file
				beanRef = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
				if (!StringUtils.hasLength(beanRef)) {
					throw new BeanDefinitionStoreException(resource, beanName, "Either 'bean' or 'local' is required for an idref");
				}
			}
			return beanRef;
		} else if (ele.getTagName().equals(LIST_ELEMENT)) {
			return getList(ele, beanName);
		} else if (ele.getTagName().equals(SET_ELEMENT)) {
			return getSet(ele, beanName);
		} else if (ele.getTagName().equals(MAP_ELEMENT)) {
			return getMap(ele, beanName);
		} else if (ele.getTagName().equals(PROPS_ELEMENT)) {
			return getProps(ele, beanName);
		} else if (ele.getTagName().equals(VALUE_ELEMENT)) {
			// it's a literal value
			return getTextValue(ele, beanName);
		} else if (ele.getTagName().equals(NULL_ELEMENT)) {
			// it's a distinguished null value
			return null;
		}
		throw new BeanDefinitionStoreException(resource, beanName, "Unknown subelement of <property>: <" + ele.getTagName() + ">");
	}

	protected List getList(Element collectionEle, String beanName) {
		NodeList nl = collectionEle.getChildNodes();
		ManagedList list = new ManagedList(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element ele = (Element) nl.item(i);
				list.add(parsePropertySubelement(ele, beanName));
			}
		}
		return list;
	}

	protected Set getSet(Element collectionEle, String beanName) {
		NodeList nl = collectionEle.getChildNodes();
		ManagedSet set = new ManagedSet(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element ele = (Element) nl.item(i);
				set.add(parsePropertySubelement(ele, beanName));
			}
		}
		return set;
	}

	protected Map getMap(Element mapEle, String beanName) {
		List list = getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		Map map = new ManagedMap(list.size());
		for (int i = 0; i < list.size(); i++) {
			Element entryEle = (Element) list.get(i);
			String key = entryEle.getAttribute(KEY_ATTRIBUTE);
			NodeList subEles = entryEle.getElementsByTagName("*");
			map.put(key, parsePropertySubelement((Element) subEles.item(0), beanName));
		}
		return map;
	}
	
	protected List getChildElementsByTagName(Element mapEle, String elementName) {
		NodeList nl = mapEle.getChildNodes();
		List nodes = new ArrayList();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element && elementName.equals(n.getNodeName())) {
				nodes.add(n);
			}
		}
		return nodes;
	}

	protected Properties getProps(Element propsEle, String beanName) {
		Properties props = new Properties();
		NodeList nl = propsEle.getElementsByTagName(PROP_ELEMENT);
		for (int i = 0; i < nl.getLength(); i++) {
			Element propEle = (Element) nl.item(i);
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			// trim the text value to avoid unwanted whitespace caused by typical XML formatting
			String value = getTextValue(propEle, beanName).trim();
			props.setProperty(key, value);
		}
		return props;
	}
	
	protected String getTextValue(Element ele, String beanName) {
		StringBuffer value = new StringBuffer();
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item instanceof org.w3c.dom.CharacterData) {
				if (!(item instanceof Comment)) {
					value.append(item.getNodeValue());
				}
			} else {
				throw new BeanDefinitionStoreException(resource, beanName, "<value> element is just allowed to have text and comment nodes, not: " + item.getClass().getName());
			}
		}
		System.out.println("<value> element, TextValue = " + value);
		return value.toString();
	}

	protected int getDependencyCheck(String att) {
		int dependencyCheckCode = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;
		if (DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = AbstractBeanDefinition.DEPENDENCY_CHECK_ALL;
		} else if (DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE;
		} else if (DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS;
		}
		// else leave default value
		return dependencyCheckCode;
	}

	protected int getAutowireMode(String att) {
		int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
		} else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
		} else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		} else if (AUTOWIRE_AUTODETECT_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
		}
		// else leave default value
		return autowire;
	}
}
