package twcore.bots.chatgui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

import twcore.core.BotAction;
import twcore.core.CoreData;
import twcore.core.Session;
import twcore.core.SubspaceBot;
import twcore.core.util.Tools;


public class Login extends JFrame {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    static JTextField userField;
	JPasswordField passField;
	public static JTextField autoField;
	public static JTextField startField;
	public static Preferences preference;
	public static String STORAGE_USERNAME = " ";
	BotAction m_botAction;
    //public ArrayList<String> file = new ArrayList<String>();

	//BotSettings m_botSettings;
	//JPasswordField pass;

	
	public Login() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));  
		JLabel about = new JLabel("Welcome to the SSCU Trench Wars Chat Application! Here you may talk to ");
		JLabel aboutc = new JLabel("people from the web! For more information, type ?about in game.");
		JLabel space = new JLabel(" ");
		JLabel userLabel = new JLabel("Continuum Username:");
		JLabel passLabel = new JLabel("Continuum Password:"); 
		JLabel autoLabel = new JLabel("Chats: - Seperate by ','. ?chat= is not needed, but acceptable");
		JLabel startLabel = new JLabel("Auto: Seperate with a space");
		userField = new JTextField("");
		passField = new JPasswordField(15);
        passField.setEchoChar('*'); 
        autoField = new JTextField("");
        startField = new JTextField("");
		preference = Preferences.userNodeForPackage(this.getClass());
        
        panel.add(about);
        panel.add(aboutc);
        panel.add(space);
		panel.add(userLabel);
		panel.add(userField);
		panel.add(passLabel);
		panel.add(passField);
		panel.add(autoLabel);
		panel.add(autoField);
		panel.add(startLabel);
		panel.add(startField);
		
		preference.get(STORAGE_USERNAME, userField.getText());
		JButton connect = new JButton("Connect");
		

		connect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//doSecret();
				preference = Preferences.userNodeForPackage(this.getClass());
				preference.put(STORAGE_USERNAME, userField.getText());
				connect();
			
		}});
		
		setTitle("TW Chat - Login by Arobas+ and Dezmond");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	    Keymap keymap = passField.getKeymap();
	    KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
	    keymap.removeKeyStrokeBinding(keystroke);

	    getRootPane().setDefaultButton(connect);
		
		add(panel,BorderLayout.CENTER);
		add(connect,BorderLayout.SOUTH);
		
		setSize(450, 250);
		setLocation(100, 100);
		setResizable(false);
		setVisible(true);
		
	}
	
	@SuppressWarnings("deprecation")
    private void connect()
	{
   	
       System.out.println("Starting the TWChat Client...");
       System.out.println("-----------------------------");
       CoreData coreData = new CoreData("66.36.241.110", 5400);
       //CoreData coreData = new CoreData("69.164.209.244", 15001);
       Tools.debugging = false;
		//for (int i=0; i<file.size(); i++)

       while(true) 
       {
           System.out.println("Attempting to connect to server at ss://" + coreData.getServerName() + ":" + coreData.getServerPort());
           System.out.println("Estabilishing connection to SSCU Trench Wars.");
           ThreadGroup group = new ThreadGroup("Main");
           Class<? extends SubspaceBot> clientClass = null;

           try {
               clientClass = ClassLoader.getSystemClassLoader().loadClass("twcore.bots.chatgui.Client").asSubclass(SubspaceBot.class);
           } catch(ClassNotFoundException e) {
               System.err.println("Class not found.");
               System.exit( 1 );
           }
           

		Session client =
               new Session(
                   coreData,
                   clientClass,
                   userField.getText(),
                   passField.getText(),
                   1,
                   group,
                   true);
       
           client.start();

           try {
        	   
               while( client.getBotState() == Session.STARTING ){
            	   dispose();
                   Thread.sleep(5);
               }

               break;
               
           } catch(Exception e) {
               Tools.printStackTrace(e);
               Tools.printLog( "An exception was encountered; now exiting." );
               System.exit(1);
           }
       }
	}
	
	public static void main(String args[]) {
		new Login();
		preference.get(STORAGE_USERNAME, userField.getText());
	}
	
	//public void doSecret() {
		//try {
        //BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\Derek\\Documents\\TWChat\\TWCore\\bin\\twcore\\bots\\chatgui\\secret.txt"));
	      //BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\Derek\\Documents\\TWChat\\TWCore\\bin\\twcore\\bots\\chatgui\\secret.txt"));
	      
        //if (!in.ready())
            //throw new IOException();

        //String line;
		//while ((line = in.readLine()) != null)
			
           // file.add(line);
		//for (int i=0; i<file.size(); i++)
		//Tools.printLog(file.get(i));

        //in.close();
        
	//} catch (IOException e) {
        //System.out.println(e);}
	//}
	
	@SuppressWarnings("unused")
    private class ServerItem {
		
		private String name;
		private String host;
		private int port;
		
		public String toString() {
			return name;
		}

	}
	
}
