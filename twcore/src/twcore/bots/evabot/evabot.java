
package twcore.bots.evabot;
import java.util.List;
import java.util.Random;
import twcore.core.*;
import twcore.core.events.*;
import twcore.core.game.*;
public class evabot extends SubspaceBot{
	public evabot(BotAction botAction) {
		super(botAction);
		oplist = ba.getOperatorList();
		oplist.addOperator("PoisonIvy", OperatorList.OWNER_LEVEL);
		ba=botAction;
		requestEvents();
		
	}
	public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.LOGGED_ON);
        BotSettings config = m_botAction.getBotSettings();
	}
	public void handleEvent(LoggedOn event){
		BotSettings config = m_botAction.getBotSettings();
		String initial = config.getString("Arena");
        m_botAction.joinArena(initial);
	}
	OperatorList oplist=null;
	private final int earthx=512,earthy=511,radius=35;
	public void handleEvent(Message message){
		
		
		String msg=message.getMessage();
		
		int PID=message.getPlayerID();
		Player p = ba.getPlayer(PID);
		
		if(p!=null){
			
			
		if(oplist.isER(p.getPlayerName())&&msg.contains("!start")&&message.getMessageType()==Message.PRIVATE_MESSAGE){
			List<Player> players = ba.getPlayingPlayers();
			if(players==null)
				return;
			ba.sendArenaMessage("GO GO GO GO",104);
			
			for(double i=0;i<players.size();i++){
				Player player=players.get((int) i);
				double prgs = (i/(players.size()))*360D;
				setShip(player.getPlayerID(),1,4);
				
				ba.warpTo(player.getPlayerID(),(int) (earthx+Math.sin(Math.toRadians(prgs))*radius),(int)(earthy+Math.cos(Math.toRadians(prgs))*radius));
				
			}
			
			Random r = new Random();
			Player alien = players.get(r.nextInt(players.size()));
			setShip(alien.getPlayerID(), 5, 8);
		}
		}
	}
	public void setShip(int PID,int rs,int re){
		Random r = new Random();
		int ship = rs+r.nextInt(re-rs)+1;
		ba.setShip(PID,ship);
		if(ship>=1&&ship<=4){
			ba.setFreq(PID, 0);
		}else if(ship>=5&&ship<=8){
			ba.setFreq(PID, 1);
		}
	}
	public String[] infections = {
			" dun goofed",
			" got dismembered",
			" became the monster",
			" has no limbs!",
			" ran out of cool deaths to say",
			" went 7 rounds with an alien, he lost",
			" thought aliens were cool",
			" got infected",
			" got high, on alien!!!",
			" though it had a costume",
			" went to the wrong event",
			" thought we were playing catch",
			" just well, died"
	};
	public void handleEvent(PlayerDeath event){
		setShip(event.getKilleeID(), 5, 8);
		Player p = ba.getPlayer(event.getKilleeID());
		if(p.getFrequency()==0){
			Random r  = new Random();
			ba.sendArenaMessage(p.getPlayerName()+infections[r.nextInt(infections.length)]);
		}
		if(ba.getPlayingFrequencySize(0)==2&&p.getFrequency()==0){
			List<Player> ps = ba.getPlayingPlayers();
			for(int i=0;i<ps.size();i++){
				Player sp =ps.get(i);
				if(sp.getFrequency()==0&&sp.getPlayerName()!=p.getPlayerName()){
					ba.sendArenaMessage(sp.getPlayerName()+" is the last human!");
				}
			}
			
		}
	}
}

