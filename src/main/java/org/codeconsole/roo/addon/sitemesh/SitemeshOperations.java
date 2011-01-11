package org.codeconsole.roo.addon.sitemesh;

/**
 * Interface of commands that are available via the Roo shell.
 *
 * @since 1.1.1
 */
public interface SitemeshOperations {
	
	String SITEMESH_FILTER_NAME = "sitemeshFilter";

	/**
	 * Indicate of the install sitemesh command should be available
	 * 
	 * @return true if it should be available, otherwise false
	 */	
	boolean isInstallSitemeshAvailable();
	
	/**
	 * Install sitemesh used for MVC scaffolded apps into the target project.
	 */	
	void installSitemesh();
}