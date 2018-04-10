
package org.springframework.core.io.support;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ResourceLoader resourceLoader;

	private ClassLoader classLoader = null;

	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}
	
	public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
		this.classLoader = classLoader;
	}
	
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
	
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader, ClassLoader classLoader) {
		this.resourceLoader = resourceLoader;
		this.classLoader = classLoader;
	}
	
	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}
	
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
	public Resource getResource(String location) {
		return this.resourceLoader.getResource(location);
	}
	
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "locationPattern is required");
		logger.debug("locationPattern which be input == " + locationPattern);
		if (locationPattern.startsWith(CLASSPATH_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			if (PathMatcher.isPattern(locationPattern.substring(CLASSPATH_URL_PREFIX.length()))) {
				// a class path resource pattern
				return findPathMatchingResources(locationPattern);
			} else {
				// all class path resources with the given name
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_URL_PREFIX.length()));
			}
		} else {
			if (PathMatcher.isPattern(locationPattern)) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			} else {
				// a single resource with the given name
				return new Resource[] {resourceLoader.getResource(locationPattern)};
			}
		}
	}
	
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = location;
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		ClassLoader cl = this.classLoader;
		if (cl == null) {
			// no class loader specified -> use thread context class loader
			cl = Thread.currentThread().getContextClassLoader();
		}
		Enumeration resourceUrls = cl.getResources(path);
		List result = new ArrayList();
		while (resourceUrls.hasMoreElements()) {
			URL url = (URL)resourceUrls.nextElement();
			result.add(new UrlResource(url));
		}
		return (Resource[])result.toArray(new Resource[result.size()]);
	}
	
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		logger.debug("rootDirPath == " + rootDirPath);
		String subPattern = locationPattern.substring(rootDirPath.length());
		logger.debug("subPattern == " + subPattern);
		Resource[] rootDirResources = getResources(rootDirPath);
		List result = new ArrayList();
		for (int i = 0; i < rootDirResources.length; i++) {
			Resource rootDirResource = rootDirResources[i];
			if ("jar".equals(rootDirResource.getURL().getProtocol())) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, subPattern));
			} else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return (Resource[])result.toArray(new Resource[result.size()]);
	}
	
	protected String determineRootDir(String location) {
		int patternStart = location.length();
		int prefixEnd = location.indexOf(":");
		int asteriskIndex = location.indexOf('*', prefixEnd);
		int questionMarkIndex = location.indexOf('?', prefixEnd);
		if (asteriskIndex != -1 || questionMarkIndex != -1) {
			patternStart = (asteriskIndex > questionMarkIndex ? asteriskIndex : questionMarkIndex);
		}
		int rootDirEnd = location.lastIndexOf('/', patternStart);
		if (rootDirEnd == -1) {
			rootDirEnd = location.lastIndexOf(":", patternStart) + 1;
		}
		return (rootDirEnd != -1) ? location.substring(0, rootDirEnd) : "";
	}
	
	protected List doFindPathMatchingJarResources(Resource rootDirResource, String subPattern) throws IOException {
		URLConnection con = rootDirResource.getURL().openConnection();
		if (!(con instanceof JarURLConnection)) {
			throw new IOException("Cannot perform jar file search for [" + rootDirResource + "]: did not return java.net.JarURLConnection; connection was [" + con + "]");
		}
		JarURLConnection jarCon = (JarURLConnection) con;
		JarFile jarFile = jarCon.getJarFile();
		URL jarFileUrl = jarCon.getJarFileURL();
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
		}
		String rootEntryPath = jarCon.getJarEntry().getName();
		String jarFileUrlPrefix = "jar:" + jarFileUrl.toExternalForm() + "!/";
		List result = new LinkedList();
		for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();) {
			JarEntry entry = (JarEntry) entries.nextElement();
			String entryPath = entry.getName();
			if (entryPath.startsWith(rootEntryPath) && PathMatcher.match(subPattern, entryPath.substring(rootEntryPath.length()))) {
				result.add(new UrlResource(new URL(jarFileUrlPrefix + entryPath)));
			}
		}
		return result;
	}
	
	protected List doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {
		File rootDir = rootDirResource.getFile().getAbsoluteFile();
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for matching resources in directory tree [" + rootDir.getPath() + "], subPattern == " + subPattern);
		}
		List matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
		List result = new ArrayList(matchingFiles.size());
		for (Iterator it = matchingFiles.iterator(); it.hasNext();) {
			File file = (File) it.next();
			result.add(new FileSystemResource(file));
		}
		return result;
	}
	
	protected List retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
		if (!rootDir.isDirectory()) {
			throw new IllegalArgumentException("'rootDir' parameter [" + rootDir + "] does not denote a directory");
		}
		String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
		if (!pattern.startsWith("/")) {
			fullPattern += "/";
		}
		fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
		List result = new LinkedList();
		doRetrieveMatchingFiles(fullPattern, rootDir, result);
		return result;
	}
	
	protected void doRetrieveMatchingFiles(String fullPattern, File dir, List result) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching directory [" + dir.getAbsolutePath() + "] for files matching pattern [" + fullPattern + "]");
		}
		File[] dirContents = dir.listFiles();
		if (dirContents == null) {
			throw new IOException("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
		}
		boolean dirDepthNotFixed = (fullPattern.indexOf("**") != -1);
		for (int i = 0; i < dirContents.length; i++) {
			String currPath = StringUtils.replace(dirContents[i].getAbsolutePath(), File.separator, "/");
			if (dirContents[i].isDirectory() && (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") < StringUtils.countOccurrencesOf(fullPattern, "/"))) {
				doRetrieveMatchingFiles(fullPattern, dirContents[i], result);
			}
			if (PathMatcher.match(fullPattern, currPath)) {
				result.add(dirContents[i]);
			}
		}
	}
}
