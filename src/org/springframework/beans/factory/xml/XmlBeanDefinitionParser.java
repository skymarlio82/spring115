
package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.Resource;

public interface XmlBeanDefinitionParser {

	int registerBeanDefinitions(BeanDefinitionReader reader, Document doc, Resource resource) throws BeansException;
	
}
