package twcore.bots.elimbot;

import java.util.HashMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;

public class elimbotConfiguration {
	
	private BotAction m_botAction;
	private HashMap<String, elimConfiguration> configurations = new HashMap<String, elimConfiguration>();
	private String arena;
	
	public static String shipNames[] = {"", "Warbird", "Javelin", "Spider", "Leviathan", "Terrier", "Weasle", "Lancaster", "Shark"};
	
	private int currentConfig = 1;
	private int playerMin = 3;
	
	public elimbotConfiguration(BotAction m_botAction) {
		this.m_botAction = m_botAction;
		this.initialize();
	}
	
	private void initialize() {
		BotSettings config = m_botAction.getBotSettings();
		
		arena = config.getString("arena");
		playerMin = config.getInt("playermin");
		
		// Load the elim configurations
		int i = 1;
		
		while(config.getString("config"+i) != null) {
			String name = config.getString("config"+i);
			
			elimConfiguration elimConf = new elimConfiguration(i,name);
			elimConf.setShips(config.getString(name+"-ships"));
			elimConf.setDeathLimit(config.getString(name+"-deathlimit"));
			if(config.getString(name+"-spawn") != null) {
				elimConf.setSpawn(config.getString(name+"-spawn"));
			}
			
			configurations.put(String.valueOf(i), elimConf);
			
			i++;
		}
	}
	
	public String getArena() {
		return this.arena;
	}
	
	public int getPlayerMin() {
		return this.playerMin;
	}
	
	/**
	 * Returns the configuration associated with the key
	 * @param key The configuration key, always of the format "config#"
	 * @return elimConfiguration
	 */
	public elimConfiguration getConfig(String key) {
		return configurations.get(key);
	}

	/**
	 * Returns the current configuration
	 * @return elimConfiguration
	 */
	public elimConfiguration getCurrentConfig() {
		return configurations.get(String.valueOf(this.currentConfig));
	}
	
	/**
	 * Advance the current configuration to the next one or to the first configuration
	 */
	public void nextConfiguration() {
		this.currentConfig++;
		
		if(configurations.get(this.currentConfig)==null) {
			this.currentConfig = 1;
		}
	}
}