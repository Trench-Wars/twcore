package twcore.bots.chatgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import twcore.core.BotAction;

public class ArenaList extends JFrame {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private BotAction m_botAction;
	private JButton go;
	private JList<String> arenaList;
	
	public ArenaList(BotAction botAction, String[] arenas) {
		this.m_botAction = botAction;

		arenaList = new JList<String>(arenas);
		arenaList.setBackground(Color.BLACK);
		arenaList.setForeground(Color.YELLOW);
		arenaList.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent arg0) { }
			public void mousePressed(MouseEvent arg0) { }
			public void mouseExited(MouseEvent arg0) { }
			public void mouseEntered(MouseEvent arg0) { }
			public void mouseClicked(MouseEvent arg0) { 
				if (arg0.getClickCount() == 2) {
					m_botAction.joinArena((String)arenaList.getSelectedValue());
					ArenaChat.playerIndex.clear();
					ArenaChat.reconstructList();
					dispose();
				}
			}
		});
		
		go = new JButton("Go");
		go.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent arg0) { }
			public void mousePressed(MouseEvent arg0) { }
			public void mouseExited(MouseEvent arg0) { }
			public void mouseEntered(MouseEvent arg0) { }
			public void mouseClicked(MouseEvent arg0) { 
				m_botAction.joinArena((String)arenaList.getSelectedValue());
				ArenaChat.clear();
				ArenaChat.reconstructList();
				dispose();
			}
		});
		
		final JScrollPane scrollingArea = new JScrollPane(arenaList);
		scrollingArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollingArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollingArea.setAutoscrolls(true);		
	
		
		setTitle("Arena List");
		setLayout(new BorderLayout());
		add(go, BorderLayout.SOUTH);
		add(scrollingArea, BorderLayout.CENTER);
		setSize(175, 350);
		setVisible(true);
		
	}

}
