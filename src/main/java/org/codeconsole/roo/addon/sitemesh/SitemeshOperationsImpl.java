package org.codeconsole.roo.addon.sitemesh;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link SitemeshOperations} interface.
 *
 * @since 1.1.1
 */
@Component
@Service
public class SitemeshOperationsImpl implements SitemeshOperations{

	private static Logger logger = Logger.getLogger(SitemeshOperations.class.getName());

	/**
	 * Get a reference to the FileManager from the underlying OSGi container. Make sure you
	 * are referencing the Roo bundle which contains this service in your add-on pom.xml.
	 * 
	 * Using the Roo file manager instead if java.io.File gives you automatic rollback in case
	 * an Exception is thrown.
	 */
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;

	/**
	 * Get a reference to the PathResolver from the underlying OSGi container. Make sure you
	 * are referencing the Roo bundle which contains this service in your add-on pom.xml.
	 * 
	 * Using the PathResolver allows us reference resources in the project without a need to 
	 * know the project layout (ie Maven project layout)
	 */
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;

	/** {@inheritDoc} */
	public boolean isInstallSitemeshAvailable(boolean debug) {
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			if (debug) {
				logger.info("Please configure a project first. Run 'project'.");
			}
			return false;
		}

		// Do not permit installation unless they have a web project
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			if (debug) {
				logger.info("Please set up a web project. No web.xml has been found. The 'controller' command will do this for you.");
			}
			return false;
		}

		// Only permit installation if they don't already have some version of Sitemesh installed
		if (!(project.getDependenciesExcludingVersion(new Dependency("opensymphony", "sitemesh", "2.4.2")).size() == 0)) {
			if (debug) {
				logger.info("SiteMesh has already been installed.");
			}
			return false;
		}
		return true;
	}
	
	/** {@inheritDoc} */
	public void installSitemesh() {
		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add dependencies to POM
		updateDependencies(configuration);		
		
		// Copy the template across
		String destination = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/decorators.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "decorators.xml"), fileManager.createFile(destination).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "sitemesh.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/sitemesh.xml")).getOutputStream());
				fileManager.createDirectory(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/decorators"));
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "decorators/default.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/decorators/default.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "decorators/public.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/decorators/public.jspx")).getOutputStream());				
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}		

		String webXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		try {
			if (fileManager.exists(webXml)) {
				MutableFile mutableWebXml = fileManager.updateFile(webXml);
				Document webXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableWebXml.getInputStream());
				WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.AFTER, WebMvcOperations.HTTP_METHOD_FILTER_NAME, null, "clearSiteMeshAppliedOnce", "org.codeconsole.sitemesh.filter.ClearSitemeshAppliedOnceFilter", "/*", webXmlDoc, "Needed for Error pages. Sets decorator for all error pages by setting request attribute decorator to public.",
						Arrays.asList(new WebXmlUtils.WebXmlParam("decorator-attribute", "public")), Arrays.asList(WebXmlUtils.Dispatcher.ERROR));
				WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.AFTER, WebMvcOperations.HTTP_METHOD_FILTER_NAME, null, SitemeshOperations.SITEMESH_FILTER_NAME, "com.opensymphony.module.sitemesh.filter.PageFilter", "/*", webXmlDoc, null, null, Arrays.asList(WebXmlUtils.Dispatcher.REQUEST, WebXmlUtils.Dispatcher.ERROR));
				XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
			} else {
				throw new IllegalStateException("Could not acquire " + webXml);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}		
		
		// Update webmvc-config.xml
		updateSpringWebCtx();
	}
	
	private void updateSpringWebCtx() {
		String mvcXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		Assert.isTrue(fileManager.exists(mvcXml), "webmvc-config.xml not found; cannot continue");

		MutableFile mutableMvcXml = null;
		Document mvcXmlDoc;
		try {
			mutableMvcXml = fileManager.updateFile(mvcXml);
			mvcXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableMvcXml.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element rootElement = mvcXmlDoc.getDocumentElement();
		Element urlBasedViewResolver = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.web.servlet.view.UrlBasedViewResolver']", rootElement);
		rootElement.removeChild(urlBasedViewResolver);

		Element tilesConfigurer = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.web.servlet.view.tiles2.TilesConfigurer']", rootElement);
		rootElement.removeChild(tilesConfigurer);		

		Element viewResolver = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.web.servlet.view.InternalResourceViewResolver']", rootElement);
		if (viewResolver != null) {
			rootElement.removeChild(viewResolver);
		}
		rootElement.appendChild(new XmlElementBuilder("bean", mvcXmlDoc).addAttribute("class", "org.springframework.web.servlet.view.InternalResourceViewResolver")
					.addChild(new XmlElementBuilder("property", mvcXmlDoc).addAttribute("name", "viewClass").addAttribute("value", "org.springframework.web.servlet.view.JstlView").build())
					.addChild(new XmlElementBuilder("property", mvcXmlDoc).addAttribute("name", "prefix").addAttribute("value", "/WEB-INF/views/").build())
					.addChild(new XmlElementBuilder("property", mvcXmlDoc).addAttribute("name", "suffix").addAttribute("value", ".jspx").build())
					.build());
		XmlUtils.writeXml(mutableMvcXml.getOutputStream(), mvcXmlDoc);
	}	
	
	private void updateDependencies(Element configuration) {
		List<Element> sitemeshDependencies = XmlUtils.findElements("/configuration/sitemesh/dependencies/dependency", configuration);
		for (Element dependencyElement : sitemeshDependencies) {
			projectOperations.addDependency(new Dependency(dependencyElement));
		}
	}
}
