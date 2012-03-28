package twcore.bots.chatgui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.Session;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.util.Tools;

public class Client extends SubspaceBot {

	private JFrame frame;
	private JPanel mainPanel;
	private JToolBar buttonPanel;
	private JMenuBar menuBar;
	
	private ArenaChat arena;
	private JBlinkableButton arenaButton;
	private HashMap<String,PrivateChat> pms;
	
	public Client(BotAction botAction) {
		super(botAction);
		

		this.pms = new HashMap<String,PrivateChat>();
		
		initializePanel();
		requestEvents();
	}
	
	private void initializePanel() {
		
		frame = new JFrame();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		buttonPanel = new JToolBar();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

		arenaButton = new JBlinkableButton("Logging on..");
		arenaButton.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					arenaButton.stopBlinking();
					mainPanel.removeAll();
					mainPanel.add(arena);
					frame.repaint();
					arena.focusOnInput();
				}
			}
		});
		buttonPanel.add(arenaButton);
		
		menuBar = new JMenuBar();
		JMenu view = new JMenu("View");
		JMenu options = new JMenu("Options");
		
		menuBar.add(view);
		menuBar.add(options);
		
		frame.setTitle("TW Chat - "+Login.userField.getText());
		
		JMenuItem item1 = new JMenuItem("Arena List");
		item1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				m_botAction.requestArenaList();
			}});
		JMenuItem item2 = new JMenuItem("Session Terminate");
		item2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
	            /* Session.m_group.interrupt();
	            Session.m_socket.disconnect();
	            Session.m_socket.close(); */
				frame.dispose();
			}});
			view.add(item1);
			options.add(item2);
		arena = new ArenaChat("0", this, m_botAction);
		mainPanel.add(arena, BorderLayout.CENTER);
		frame.setLayout(new BorderLayout());
		frame.add(buttonPanel, BorderLayout.NORTH);
		frame.add(mainPanel, BorderLayout.CENTER);
		
		frame.setJMenuBar(menuBar);
		frame.setSize(700, 400);
		mainPanel.setPreferredSize(new Dimension(700, 400));
		frame.setLocation(100, 100);
		frame.setVisible(true);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		arena.focusOnInput();

	}
	
	private void requestEvents() {
        EventRequester eventRequester = m_botAction.getEventRequester();
        eventRequester.request(EventRequester.LOGGED_ON);
        eventRequester.request(EventRequester.MESSAGE);
        eventRequester.request(EventRequester.PLAYER_LEFT);
        eventRequester.request(EventRequester.PLAYER_ENTERED);
        eventRequester.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(EventRequester.FREQUENCY_CHANGE);
        eventRequester.request(EventRequester.ARENA_LIST);
        eventRequester.request(EventRequester.ARENA_JOINED);
	}
	
	public void handleEvent(LoggedOn event) {
		m_botAction.joinArena("0");
		String chat = Login.autoField.getText();
		if(chat.startsWith("?chat=")){
		m_botAction.sendUnfilteredPublicMessage(chat);
		} else  {
			m_botAction.sendUnfilteredPublicMessage("?chat="+chat);}
		m_botAction.sendUnfilteredPublicMessage( "*g*misc:alertcommand" );
		

		
	}
	

	
	public void handleEvent(Message event) {
		
		if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
			
			short sender = event.getPlayerID();
			final String name = event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ? event.getMessager() :  m_botAction.getPlayerName(sender);
			//final String name = m_botAction.getPlayerName(event.getPlayerID());
			boolean newChat = !pms.containsKey(name);
			final PrivateChat chat = pms.containsKey(name) ? pms.get(name) : new PrivateChat(name, this, m_botAction);
			if (newChat) {
				pms.put(name,chat);
				
				// Toolbar button
				final JBlinkableButton button = new JBlinkableButton(name);
				buttonPanel.add(button);
				buttonPanel.revalidate();
				buttonPanel.repaint();
				button.addMouseListener(new MouseListener() {
					public void mouseReleased(MouseEvent e) { }
					public void mousePressed(MouseEvent e) { }
					public void mouseExited(MouseEvent e) { }
					public void mouseEntered(MouseEvent e) { }
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							button.stopBlinking();
							mainPanel.removeAll();
							mainPanel.add(chat);
							mainPanel.revalidate();
							frame.repaint();
						} else if (e.getButton() == MouseEvent.BUTTON3) {
							JPopupMenu menu = new JPopupMenu();
							JMenuItem close = new JMenuItem("Close");
							close.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent arg0) {
									pms.remove(name);
									buttonPanel.remove(button);
									buttonPanel.revalidate();
									buttonPanel.repaint();
									//frame.repaint();
								}
							});
							menu.add(close);
					        menu.show(e.getComponent(), e.getX(), e.getY());
						}
					}
				});
			}
			pms.get(name).handleEvent(event);
			if (!pms.get(name).isVisible()) {
				Component[] components = buttonPanel.getComponents();
				for(Component c: components) {
					if (c instanceof JBlinkableButton) {
						if (((JBlinkableButton)c).getText().equals(name)) 
							((JBlinkableButton)c).blink();

					}
				}
			}
			//arena.focusOnInput();
		}
		else {
			if (!event.getMessage().equals("This arena is Continuum-only. Please get Continuum client from http://www.subspace.net to play here")
					&& !event.getMessage().equals("Obscenity block OFF")
					&& !event.getMessage().equals("Showing Energy ON"))
			arena.handleEvent(event);

		}
	}
	
	public void handleEvent(ArenaJoined event) {
		Tools.printLog("Joining arena " + m_botAction.getArenaName());
		arenaButton.setText(m_botAction.getArenaName() +" - "+m_botAction.getArenaSize());
		String auto = Login.startField.getText();
		if(auto.contains("?obscene")){
		    m_botAction.sendUnfilteredPublicMessage("?obscene");
		} else if(auto.contains("?messages")){
		    m_botAction.sendUnfilteredPublicMessage("?messages");
		} else if(auto.contains("?blogin ")){
		    String b = auto.substring(8);
		    m_botAction.sendUnfilteredPublicMessage("?blogin "+b);
		}
		arena.handleEvent(event);
	}
	
	public void handleEvent(ArenaList event) {
		new twcore.bots.chatgui.ArenaList(m_botAction, event.getArenaNames());
	}
	
	public void handleEvent(PlayerEntered event) {
		arenaButton.setText(m_botAction.getArenaName() +" - "+m_botAction.getArenaSize());
		arena.handleEvent(event);
	}
	
	public void handleEvent(PlayerLeft event) {
		arenaButton.setText(m_botAction.getArenaName() +" - "+m_botAction.getArenaSize());
		arena.handleEvent(event);
	}
	public void handleEvent(FrequencyShipChange event) {
		arena.handleEvent(event);
	}
	
	public void handleEvent(FrequencyChange event) {
		arena.handleEvent(event);
	}

	public void handleDisconnect() {
		frame.dispose();
		Tools.printLog("Client Disconnected");
		new Login();
	}
}
