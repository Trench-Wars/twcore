package twcore.core.game;


/**
 * Interface for use with ShipRestrictor to allow classes to receive
 * notification of a player attempting to change ships and then not being
 * allowed.
 *
 * @author D1st0rt
 * @version 07.01.10
 */
public interface InvalidShipListener
{
	/**
	 * Method called when a player's attempt to change ships is deemed illegal
	 * by the ShipRestrictor with jurisdiction.
	 * @param p the player who tried to change ships
	 * @param ship the ship the player tried to change to
	 * @param t the team object for the player
	 */
	public void changeDenied(Player p, byte ship, Team t);
}
