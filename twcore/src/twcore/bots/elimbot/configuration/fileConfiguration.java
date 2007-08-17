package twcore.bots.elimbot.configuration;

import twcore.core.util.Tools;

public class fileConfiguration {
	private int id;
	private String name;
	
	private boolean shipsVote;
	private int[] ships = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private int shipsDefault;
	// Let's not use the 1st digit (position 0)
	// 0 = ship disabled
	// 1 = ship enabled
	
	private boolean deathLimitVote;
	private int[] deathLimit = {0,0};
	private int deathLimitDefault;
	private int spawnX,spawnY,spawnRadius;
	private boolean spawnSet;

	public fileConfiguration() {
		super();
	}
	
	public fileConfiguration(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	/**
	 * @return the deathLimitDefault
	 */
	public int getDeathLimitDefault() {
		return deathLimitDefault;
	}

	/**
	 * @param deathLimitDefault the deathLimitDefault to set
	 */
	public void setDeathLimitDefault(int deathLimitDefault) {
		this.deathLimitDefault = deathLimitDefault;
	}

	/**
	 * @return the deathLimit
	 */
	public int[] getDeathLimit() {
		return deathLimit;
	}

	/**
	 * @param deathLimit the deathLimit to set
	 */
	public void setDeathLimit(int[] deathLimit) {
		this.deathLimit = deathLimit;
	}
	
	/**
	 * @param ships the ships to set
	 */
	public void setDeathLimit(String deathLimit) {
		if(deathLimit.startsWith("~")) {
			this.setDeathLimitVote(true);
			deathLimit = deathLimit.substring(1);
		}
		
		String pieces[] = deathLimit.split(",");
		if(pieces.length == 1) {
			this.deathLimit[0] = Integer.parseInt(pieces[0]);
		} else if(pieces.length == 2) {
			this.deathLimit[0] = Integer.parseInt(pieces[0]);
			this.deathLimit[1] = Integer.parseInt(pieces[1]);
		} else {
			Tools.printLog("Error in elimbot configuration #"+this.id+" ("+this.name+"): deathlimit.");
		}
	}

	/**
	 * @return the deathLimitVote
	 */
	public boolean isDeathLimitVote() {
		return deathLimitVote;
	}

	/**
	 * @param deathLimitVote the deathLimitVote to set
	 */
	public void setDeathLimitVote(boolean deathLimitVote) {
		this.deathLimitVote = deathLimitVote;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the ships
	 */
	public int[] getShips() {
		return ships;
	}

	/**
	 * @param ships the ships to set
	 */
	public void setShips(int[] ships) {
		this.ships = ships;
	}
	
	/**
	 * @param ships the ships to set
	 */
	public void setShips(String ships) {
		if(ships.startsWith("~")) {
			this.setShipsVote(true);
			ships = ships.substring(1);
		}
		
		String pieces[] = ships.split(",");
		for( int i = 0; i < pieces.length; i++ ) {
			if(pieces[i] != null && pieces[i].length()==1 ) {
				int ship = Integer.parseInt(pieces[i]);
				this.ships[ship] = 1;
			}  
		}
	}

	/**
	 * @return the shipsDefault
	 */
	public int getShipsDefault() {
		return shipsDefault;
	}

	/**
	 * @param shipsDefault the shipsDefault to set
	 */
	public void setShipsDefault(int shipsDefault) {
		this.shipsDefault = shipsDefault;
	}

	/**
	 * @return the shipsVote
	 */
	public boolean isShipsVote() {
		return shipsVote;
	}

	/**
	 * @param shipsVote the shipsVote to set
	 */
	public void setShipsVote(boolean shipsVote) {
		this.shipsVote = shipsVote;
	}

	/**
	 * @return the spawnRadius
	 */
	public int getSpawnRadius() {
		return spawnRadius;
	}

	/**
	 * @param spawnRadius the spawnRadius to set
	 */
	public void setSpawnRadius(int spawnRadius) {
		this.spawnRadius = spawnRadius;
	}

	/**
	 * @return the spawnX
	 */
	public int getSpawnX() {
		return spawnX;
	}

	/**
	 * @param spawnX the spawnX to set
	 */
	public void setSpawnX(int spawnX) {
		this.spawnX = spawnX;
	}

	/**
	 * @return the spawnY
	 */
	public int getSpawnY() {
		return spawnY;
	}

	/**
	 * @param spawnY the spawnY to set
	 */
	public void setSpawnY(int spawnY) {
		this.spawnY = spawnY;
	}
	
	public void setSpawn(String spawn) {
		String pieces[] = spawn.split(",");
		
		if(pieces.length == 3) {
			this.spawnX = Integer.parseInt(pieces[0]);
			this.spawnY = Integer.parseInt(pieces[1]);
			this.spawnRadius = Integer.parseInt(pieces[2]);
			this.spawnSet = true;
		} else {
			Tools.printLog("Error in elimbot configuration #"+this.id+" ("+this.name+"): spawn.");
		}
	}
	
	public boolean isSpawnset() {
		return this.spawnSet;
	}
}
