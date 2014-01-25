package twcore.bots.chatgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import twcore.core.BotAction;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;

public class ArenaChat extends Chat {

	/**
     * This is where the ability to chat comes in.
     */
    private static final long serialVersionUID = 1L;
    static TreeSet<String> playerIndex;
	private static DefaultListModel<String> playerModel;
	private static JList<String> playerList;
	private static ArrayList<String> spec;
	private static ArrayList<String> freq0;
	private static ArrayList<String> freq1;
	private static ArrayList<String> priv;
	private static ArrayList<String> publicFreq;

	public ArenaChat(String name, Client client, BotAction botAction) 
	{
		super(name, client, botAction);
		playerIndex = new TreeSet<String>();
		playerModel = new DefaultListModel<String>();
		playerList = new JList<String>(playerModel);
		spec = new ArrayList<String>();
		freq0 = new ArrayList<String>();
		freq1 = new ArrayList<String>();
		priv = new ArrayList<String>();
		publicFreq = new ArrayList<String>();
		playerList.setFixedCellWidth(175);
		playerList.setFixedCellHeight(20);
		playerList.setBackground(Color.BLACK);
		playerList.setForeground(Color.YELLOW);
		playerList.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent arg0) { }
			public void mousePressed(MouseEvent arg0) { }
			public void mouseExited(MouseEvent arg0) { }
			public void mouseEntered(MouseEvent arg0) { }
			public void mouseClicked(MouseEvent arg0) { 
				if (arg0.getClickCount()==2) {
					input.setText(":" + playerList.getSelectedValue() + ":");
					input.requestFocusInWindow();
				} else if (arg0.getClickCount()==1) {
					input.requestFocusInWindow();
				}}
		
		});
		
		final JScrollPane scrollingArea = new JScrollPane(playerList);
		scrollingArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollingArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollingArea.setAutoscrolls(true);
		
		add(scrollingArea, BorderLayout.EAST);
		
	};
	
	public void handleEvent(Message event) {
		
		String prefix = "";
		Style style = context.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
	    StyleConstants.setSpaceAbove(style, 1);
	    StyleConstants.setSpaceBelow(style, 1);
	    StyleConstants.setFontSize(style, 12);
	    
	    switch (event.getMessageType()) {
	    case Message.ALERT_MESSAGE:
	    	String command = event.getAlertCommandType().toLowerCase();
	    	if( command.equals( "help" )){
                prefix = "help: (" + event.getMessager() + ") ";
            } else if( command.equals( "cheater" )){
                prefix = "cheater: (" + event.getMessager() + ") ";
            } else if( command.equals( "advert" )){
               prefix = "advert (" + event.getMessager() + ") ";
            }
	    	StyleConstants.setForeground(style, CHAT_GREEN);
	    	break;
	    case Message.ARENA_MESSAGE:
	    	StyleConstants.setForeground(style, CHAT_GREEN);
	    	break;
	    case Message.CHAT_MESSAGE:
	    	prefix = event.getChatNumber() + ":" + event.getMessager() + "> ";
	    	StyleConstants.setForeground(style, CHAT_RED);
	    	break;
	    case Message.OPPOSING_TEAM_MESSAGE:
	    	prefix = m_botAction.getPlayerName(event.getPlayerID()) + "> ";
	    	StyleConstants.setForeground(style, CHAT_GREEN);
	    	try {
				document.insertString(document.getLength(), prefix, style);
				prefix = "";
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
	    	StyleConstants.setForeground(style, CHAT_BLUE);
	    	break;
	    case Message.PUBLIC_MACRO_MESSAGE:
	    	prefix = "[MACRO] " + m_botAction.getPlayerName(event.getPlayerID()) + "> ";
	    	StyleConstants.setForeground(style, CHAT_BLUE);
	    	break;
	    case Message.PUBLIC_MESSAGE:
	    	prefix = m_botAction.getPlayerName(event.getPlayerID()) + "> ";
	    	StyleConstants.setForeground(style, CHAT_BLUE);
	    	break;
	    case Message.REMOTE_PRIVATE_MESSAGE:
	    	break;
	    case Message.SERVER_ERROR:
	    	//prefix = "[SERVER ERROR] ";
	    	StyleConstants.setForeground(style, CHAT_WARNING);
	    	break;
	    case Message.TEAM_MESSAGE:
	    	prefix = m_botAction.getPlayerName(event.getPlayerID()) + "> ";
	    	StyleConstants.setForeground(style, CHAT_YELLOW);
	    	break;
	    case Message.WARNING_MESSAGE:
	    	prefix = "[MODERATOR WARNING] " + m_botAction.getPlayerName(event.getPlayerID()) + "> ";
	    	StyleConstants.setForeground(style, CHAT_RED);
	    	StyleConstants.setBold(style, true);
	    	break;
	    default:
	    	StyleConstants.setForeground(style, CHAT_BLUE);
	    	break;
	    }
	    
	    try {
	    	document.insertString(document.getLength(), prefix + event.getMessage() + "\r\n", style);
	    }
	    catch (BadLocationException ex) {
	    	System.err.println(ex);
	    }
	    
	    chatArea.setDocument(document);
	}
	
	public void handleEvent(ArenaJoined event) {
		
		reconstructList();	}
	
	public void handleEvent(FrequencyShipChange event){
		String name = m_botAction.getPlayerName(event.getPlayerID());
			if(event.getFrequency() == 0 && !freq0.contains(name)){
			freq0.add(name);
			if(freq1.contains(name)){
				freq1.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
		} else if(event.getFrequency() == 1 && !freq1.contains(name)){
			freq1.add(name);
			if(freq0.contains(name)){
				freq0.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
			} else if(event.getFrequency() > 100 && event.getFrequency() < 8025 && !priv.contains(name)){
				priv.add(name);
				if(freq1.contains(name)){
					freq1.remove(name);
				} else if(freq0.contains(name)){
					freq0.remove(name);
				} else if(spec.contains(name)){
					spec.remove(name);
				} else if(publicFreq.contains(name)){
					publicFreq.remove(name);
				}
				reconstructList();
			} else if(event.getFrequency() >= 2 && event.getFrequency() <= 99 && !publicFreq.contains(name)){
				publicFreq.add(name);
				if(freq1.contains(name)){
					freq1.remove(name);
				} else if(freq0.contains(name)){
					freq0.remove(name);
				} else if(spec.contains(name)){
					spec.remove(name);
				} else if(priv.contains(name)){
					priv.remove(name);
				} else if(publicFreq.contains(name)){
					publicFreq.remove(name);
				}
				reconstructList();
				} else if(event.getFrequency() >= 8025 && !spec.contains(name)){
					spec.add(name);
					if(freq1.contains(name)){
						freq1.remove(name);
					} else if(priv.contains(name)){
						priv.remove(name);
					} else if(freq0.contains(name)){
						freq0.remove(name);
					} else if(publicFreq.contains(name)){
						publicFreq.remove(name);
					}
					reconstructList();
				
				}
			reconstructList();
		}
	
	public void handleEvent(FrequencyChange event){
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getFrequency() == 0 && !freq0.contains(name)){
			freq0.add(name);
			if(freq1.contains(name)){
				freq1.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
		} else if(event.getFrequency() == 1 && !freq1.contains(name)){
			freq1.add(name);
			if(freq0.contains(name)){
				freq0.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
			} else if(event.getFrequency() > 100 && event.getFrequency() < 8025 && !priv.contains(name)){
				priv.add(name);
				if(freq1.contains(name)){
					freq1.remove(name);
				} else if(freq0.contains(name)){
					freq0.remove(name);
				} else if(spec.contains(name)){
					spec.remove(name);
				} else if(publicFreq.contains(name)){
					publicFreq.remove(name);
				}
				reconstructList();
			} else if(event.getFrequency() >= 2 && event.getFrequency() <= 99 && !publicFreq.contains(name)){
				publicFreq.add(name);
					if(freq1.contains(name)){
						freq1.remove(name);
					} else if(freq0.contains(name)){
						freq0.remove(name);
					} else if(spec.contains(name)){
						spec.remove(name);
					} else if(priv.contains(name)){
						priv.remove(name);
					} else if(publicFreq.contains(name)){
						publicFreq.remove(name);
					}
				reconstructList();
			} else if(event.getFrequency() >= 8025 && !spec.contains(name)){
				spec.add(name);
				if(freq1.contains(name)){
					freq1.remove(name);
				} else if(priv.contains(name)){
					priv.remove(name);
				} else if(freq0.contains(name)){
					freq0.remove(name);
				} else if(publicFreq.contains(name)){
					publicFreq.remove(name);
				}
				reconstructList();
			}
		reconstructList();
			}
	
	
			
		
	

	public void handleEvent(PlayerEntered event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
			if(event.getTeam() == 0){
			freq0.add(name);
			if(freq1.contains(name)){
				freq1.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
		} else if(event.getTeam() == 1){
			freq1.add(name);
			if(freq0.contains(name)){
				freq0.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);
			}
			reconstructList();
		} else if(event.getTeam() >= 8025){
				spec.add(name);
				reconstructList();
				if(freq1.contains(name)){
					freq1.remove(name);
				} else if(priv.contains(name)){
					priv.remove(name);
				} else if(freq0.contains(name)){
					freq0.remove(name);
				} else if(publicFreq.contains(name)){
					publicFreq.remove(name);
				}
				reconstructList();
		} else if(event.getTeam() >= 2 && event.getTeam() <= 99){
			publicFreq.add(name);
			if(freq1.contains(name)){
				freq1.remove(name);
			} else if(freq0.contains(name)){
				freq0.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(priv.contains(name)){
				priv.remove(name);
			}
			reconstructList();
		} else if(event.getTeam() >= 100 && event.getTeam() < 8025){
			priv.add(name);
			if(freq1.contains(name)){
				freq1.remove(name);
			} else if(freq0.contains(name)){
				freq0.remove(name);
			} else if(spec.contains(name)){
				spec.remove(name);
			} else if(publicFreq.contains(name)){
				publicFreq.remove(name);	
			}
				reconstructList();
		}
			reconstructList();
			}
		
	
	public void handleEvent(PlayerLeft event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(spec.contains(name)){
			spec.remove(name);
			reconstructList();
		} else if(freq0.contains(name)){
			freq0.remove(name);
			reconstructList();
		} else if(freq1.contains(name)){
			freq1.remove(name);
			reconstructList();
		} else if(priv.contains(name)){
			priv.remove(name);
			reconstructList();
		} else if(publicFreq.contains(name)){
			publicFreq.remove(name);
			reconstructList();
		}
		reconstructList();
		}
	
	public static void reconstructList() {
		Iterator<String> list = spec.iterator();
		Iterator<String> list0 = freq0.iterator();
		Iterator<String> list1 = freq1.iterator();
		Iterator<String> listpriv = priv.iterator();
		Iterator<String> listpub = publicFreq.iterator();

 
		playerModel = new DefaultListModel<String>();
		Collections.sort(spec, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(freq0, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(freq1, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(priv, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(publicFreq, String.CASE_INSENSITIVE_ORDER);
		
			playerModel.addElement("Spectators: "+spec.size());
			while(list.hasNext()){
			playerModel.addElement(list.next().toString());}
			playerModel.addElement(" ");
			playerModel.addElement("Freq 0: "+freq0.size());
			while(list0.hasNext()){
			playerModel.addElement(list0.next().toString());}
			playerModel.addElement(" ");
			playerModel.addElement("Freq 1: "+freq1.size());
			while(list1.hasNext()){
			playerModel.addElement(list1.next().toString());}
			playerModel.addElement(" ");
			playerModel.addElement("Public Freq(s): "+publicFreq.size());
			while(listpub.hasNext()){
			playerModel.addElement(listpub.next().toString());}
			playerModel.addElement(" ");
			playerModel.addElement("Private Freq(s): "+priv.size());
			while(listpriv.hasNext()){
			playerModel.addElement(listpriv.next().toString());}
	
			
		playerList.setModel(playerModel);
		playerList.repaint();
		playerList.validate();
		
		
	}
	
	@Override
	public String getTitle() {
		return "PM: " + name;
	}
	
	@Override
	public void sendMessage(String message) {
        if ( message.startsWith(":") ) {
            if (message.indexOf(":",1) <= 1) {
                return;
            }
            else {
                int breakSpot = message.indexOf(":",1);
                String sender = message.substring(1, breakSpot);
                message = message.substring(breakSpot + 1); 
                
                m_botAction.sendSmartPrivateMessage(sender, message);
                appendText(m_botAction.getBotName() + "> " + message, Message.PRIVATE_MESSAGE);
            }          
        }
        else if(message.startsWith("//")) {
			String msg = message.substring(2);
			m_botAction.sendTeamMessage(msg);
			appendText(m_botAction.getBotName()+ "> "+ msg, Message.TEAM_MESSAGE);
		}
        else if (message.startsWith("/")) {
        	String receiver = playerList.getSelectedValue().toString();
        	message = message.substring(1);
        	m_botAction.sendSmartPrivateMessage(receiver, message);
        	appendText(m_botAction.getBotName() + "> " + message, Message.PRIVATE_MESSAGE);
        }
        else if (message.startsWith("\"")) {
        	message = message.substring(1);
        	m_botAction.sendOpposingTeamMessageByFrequency(0, message);
        	appendText(m_botAction.getBotName() + "> " + message, Message.OPPOSING_TEAM_MESSAGE);
        }
		else if(message.startsWith("'")) {
			String sub = message.substring(1);
			m_botAction.sendTeamMessage(sub);
			appendText(m_botAction.getBotName()+ "> "+ sub, Message.TEAM_MESSAGE);        	
        }   
		else if(message.startsWith(";2;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(2, chat);
            appendText("2:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.startsWith(";3;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(3,chat);
            appendText("3:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.startsWith(";4;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(4,chat);
            appendText("4:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
        }
		else if(message.startsWith(";5;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(5,chat);
            appendText("5:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
        }
		else if(message.startsWith(";6;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(6,chat);
            appendText("6:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		} 
		else if(message.startsWith(";7;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(7,chat);
            appendText("7:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.startsWith(";;8;")) {
			String chat = message.substring(4);
            m_botAction.sendChatMessage(8,chat);
            appendText("8:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.startsWith(";9;")) {
			String chat = message.substring(3);
            m_botAction.sendChatMessage(9,chat);
            appendText("9:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.startsWith(";;10;")) {
			String chat = message.substring(5);
            m_botAction.sendChatMessage(10,chat);
            appendText("10:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}  
		else if(message.startsWith(";")) {
			String chat = message.substring(1);
            m_botAction.sendChatMessage(chat);
            appendText("1:"+m_botAction.getBotName()+ "> "+ chat, Message.CHAT_MESSAGE);
		}
		else if(message.toLowerCase().equals("?chat")) {
			m_botAction.sendUnfilteredPublicMessage("?chat");
		}
		else if(message.toLowerCase().equalsIgnoreCase("?messages")) {
			m_botAction.sendUnfilteredPublicMessage("?messages");
		} 
		else if(message.toLowerCase().startsWith("?help ")) {
			String help = message.substring(6);
            appendText(m_botAction.getBotName() + "> " + message, Message.PUBLIC_MESSAGE);
            m_botAction.sendHelpMessage(help);
        }
		else if ( message.startsWith(":") && !message.toLowerCase().contains("*warn") ) {			
			String warn = message.substring(6);
			m_botAction.warnPlayer(name, warn);
		}
		else if(message.toLowerCase().startsWith("?cheater ")) {
			String cheater = message.substring(9);
            m_botAction.sendCheaterMessage(cheater);
            appendText(m_botAction.getBotName() + "> " + message, Message.PRIVATE_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?go ")) {
			String arena = message.substring(4);
			clear();
            m_botAction.changeArena(arena);
        }
		else if(message.toLowerCase().startsWith("?blogin ")) {
			String bang = message.substring(7);
            m_botAction.sendUnfilteredPublicMessage("?blogin "+bang, Message.PUBLIC_MESSAGE);
        }
		else if(message.toLowerCase().equals("?go")) {
            //playerIndex.clear();
			clear();
            m_botAction.changeArena("0");
        }
		else if(message.toLowerCase().startsWith("?find ")) {
			String find = message.substring(6);
            m_botAction.sendUnfilteredPublicMessage("?find "+find);
            appendText(m_botAction.getBotName() + "> ?find " + find, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?chat=")) {
			String chat = message.substring(6);
            m_botAction.sendUnfilteredPublicMessage("?chat="+chat);
            appendText(m_botAction.getBotName() + "> ?chat=" + chat, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("*arena ")){
			String arena = message.substring(6);
			m_botAction.sendArenaMessage(arena);
            appendText(m_botAction.getBotName() + "> *arena" + arena, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?ban -e")){
			String ban = message.substring(7);
			m_botAction.sendUnfilteredPublicMessage("?ban -e"+ban);
            appendText(m_botAction.getBotName() + "> ?ban -e" + ban, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?banlist ")){
			String banl = message.substring(9);
			m_botAction.sendUnfilteredPublicMessage("?banlist "+banl);
            appendText(m_botAction.getBotName() + "> ?banlist " + banl, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?listban ")){
			String banl = message.substring(9).trim();
			m_botAction.sendUnfilteredPublicMessage("?listban "+banl);
            appendText(m_botAction.getBotName() + "> ?listban" + banl, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().equals("?banlist")){
			m_botAction.sendUnfilteredPublicMessage("?banlist");
            appendText(m_botAction.getBotName() + "> ?banlist", Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().equals("?listban")){
			m_botAction.sendUnfilteredPublicMessage("?listban");
            appendText(m_botAction.getBotName() + "> ?listban", Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?bancomment ")){
			String banc = message.substring(12).trim();
			m_botAction.sendUnfilteredPublicMessage("?bancomment "+banc);
            appendText(m_botAction.getBotName() + "> ?bancomment " + banc, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?liftban ")){
			String banlift = message.substring(9).trim();
			m_botAction.sendUnfilteredPublicMessage("?liftban "+banlift);
            appendText(m_botAction.getBotName() + "> ?liftban " + banlift, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().startsWith("?changeban ")){
			String banchange = message.substring(10).trim();
			m_botAction.sendUnfilteredPublicMessage("?changeban "+banchange);
            appendText(m_botAction.getBotName() + "> ?changeban " + banchange, Message.PUBLIC_MESSAGE);
		}
		else if(message.toLowerCase().equalsIgnoreCase("?about")) {
			appendText("***												    ", Message.ARENA_MESSAGE);
			appendText("*** Welcome to SSCU Trench Wars Chat Application!   ", Message.ARENA_MESSAGE);
			appendText("***												    ", Message.ARENA_MESSAGE);
			appendText("*** All basic commands are the same as you would do ", Message.ARENA_MESSAGE);
			appendText("*** as if you were in game!                         ", Message.ARENA_MESSAGE);
			appendText("*** Designed and created by Arobas+ And Dezmond!    ", Message.ARENA_MESSAGE);
			appendText("*** Thanks to Flared, SpookedOne, WingZero and Zazu!", Message.ARENA_MESSAGE);
			appendText("*** Special thx to Plucker! (Pure_Luck)			    ", Message.ARENA_MESSAGE);
			appendText("*** If you find any bugs, report to Dezmond         ", Message.ARENA_MESSAGE);
			appendText("*** There is also a Android Trench Wars App!! Yey!  ", Message.ARENA_MESSAGE);
			appendText("*** You may find this on the Android Market Place,  ", Message.ARENA_MESSAGE);
			appendText("*** under the name ....                             ", Message.ARENA_MESSAGE);
			appendText("*** Thanks and Enjoy!                               ", Message.ARENA_MESSAGE);
			appendText("***												    ", Message.ARENA_MESSAGE);
		}
		else {
			m_botAction.sendPublicMessage(message);
			appendText(m_botAction.getBotName() + "> " + message, Message.PUBLIC_MESSAGE);
		}
	}

	static void clear() {
		spec.clear();
		freq0.clear();
		freq1.clear();
		priv.clear();
		publicFreq.clear();
		
	}
}
