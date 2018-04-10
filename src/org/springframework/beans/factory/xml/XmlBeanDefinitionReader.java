
package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.Resource;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	private boolean validating = true;

	private EntityResolver entityResolver = new BeansDtdResolver();

	@SuppressWarnings("rawtypes")
	private Class parserClass = DefaultXmlBeanDefinitionParser.class;
	
	public XmlBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		super(beanFactory);
	}
	
	public void setValidating(boolean validating) {
		this.validating = validating;
	}
	
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}
	
	@SuppressWarnings("rawtypes")
	public void setParserClass(Class parserClass) {
		if (this.parserClass == null || !XmlBeanDefinitionParser.class.isAssignableFrom(parserClass)) {
			throw new IllegalArgumentException("parserClass must be an XmlBeanDefinitionParser");
		}
		this.parserClass = parserClass;
	}
	
	public int loadBeanDefinitions(Resource resource) throws BeansException {
		if (resource == null) {
			throw new BeanDefinitionStoreException("resource cannot be null: expected an XML file");
		}
		InputStream is = null;
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Loading XML bean definitions from " + resource + "");
			}
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			if (logger.isDebugEnabled()) {
				logger.debug("Using JAXP implementation [" + factory + "]");
			}
			factory.setValidating(validating);
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			docBuilder.setErrorHandler(new BeansErrorHandler());
			if (entityResolver != null) {
				docBuilder.setEntityResolver(entityResolver);
			}
			is = resource.getInputStream();
			Document doc = docBuilder.parse(is);
			return registerBeanDefinitions(doc, resource);
		} catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException("Parser configuration exception parsing XML from " + resource, ex);
		} catch (SAXParseException ex) {
			throw new BeanDefinitionStoreException("Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		} catch (SAXException ex) {
			throw new BeanDefinitionStoreException("XML document from " + resource + " is invalid", ex);
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException("IOException parsing XML document from " + resource, ex);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ex) {
					logger.warn("Could not close InputStream", ex);
				}
			}
		}
	}
	
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeansException {
		XmlBeanDefinitionParser parser = (XmlBeanDefinitionParser)BeanUtils.instantiateClass(parserClass);
		return parser.registerBeanDefinitions(this, doc, resource);
	}
	
	private static class BeansErrorHandler implements ErrorHandler {
		private final static Log logger = LogFactory.getLog(XmlBeanDefinitionReader.class);
		public void error(SAXParseException ex) throws SAXException {
			throw ex;
		}
		public void fatalError(SAXParseException ex) throws SAXException {
			throw ex;
		}
		public void warning(SAXParseException ex) throws SAXException {
			logger.warn("Ignored XML validation warning: " + ex.getMessage(), ex);
		}
	}
}
