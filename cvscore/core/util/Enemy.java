package twcore.core.util;

class Enemy {
	
	int totalEnergy = 0;
	int currentEnergy = 0;
	int damageTaken = 0;
	
	public Enemy( int d ) {
		totalEnergy = d;
		currentEnergy = d;
	}
	
	public Enemy( int d, int d2 ) {
		damageTaken = d2;
	}
	
	public void addDamage( int d ) { 
		totalEnergy += d;
		currentEnergy += d;
	}
	
	public int getCurrentEnergy() {
		return currentEnergy;
	}
	
	public void addDamageDealt( int d ) {
		damageTaken += d;
	}
	
	public int getDamageDealt() { return damageTaken; }
	public int getDamageTaken() { return totalEnergy; }
	
	public void zeroDamage() {
		currentEnergy = 0;
	}
}