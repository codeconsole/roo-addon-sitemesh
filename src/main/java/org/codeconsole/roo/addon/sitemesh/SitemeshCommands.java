package org.codeconsole.roo.addon.sitemesh;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Sitemesh command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colors on 
 * non-Windows systems).
 * 
 * @since 1.1.1
 */
@Component //use these Apache Felix annotations to register your commands class in the Roo container
@Service
public class SitemeshCommands implements CommandMarker { //all command types must implement the CommandMarker interface
	
	/**
	 * Get hold of a JDK Logger
	 */
	private Logger log = Logger.getLogger(getClass().getName());

	/**
	 * Get a reference to the SitemeshOperations from the underlying OSGi container
	 */
	@Reference private SitemeshOperations operations; 
	
	@CliAvailabilityIndicator("sitemesh setup") public boolean isInstallSitemeshAvailable() {
		return operations.isInstallSitemeshAvailable();
	}

	@CliCommand(value = "sitemesh setup", help = "Install Sitemesh into your project.") 
	public void installSitemesh() {
		operations.installSitemesh();
	}
}