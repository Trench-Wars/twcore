package twcore.core;

/**
 * If a bot use a configuration file, it should implement this interface
 * The command "!reloadconf <bottype>" will call reload()
 */
public interface Reloadable {

	public void reload();
	
}
