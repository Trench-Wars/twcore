package twcore.bots.chatgui;

import java.awt.Color;

import javax.swing.JButton;

public class JBlinkableButton extends JButton {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private boolean blinking = false;
	
	public JBlinkableButton(String name) {
		super(name);
	}

	public void blink() {
		blinking = true;
		Thread t = new Thread() {
			public void run() {
				while (blinking) {
					setForeground(Color.red);
					try { Thread.sleep(500); } catch(Exception e){}
					setForeground(Color.black);
					try { Thread.sleep(500); } catch(Exception e){}
				}
			}
		};
		t.start();
	}
	
	public void stopBlinking() {
		blinking = false;
	}
	
}
