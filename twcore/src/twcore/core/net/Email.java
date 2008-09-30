package twcore.core.net;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import twcore.core.util.Tools;

public class Email {
    
    private Properties emailProperties;
    private String hostname;
    private int port;
    private String user;
    private String password;
	
	public Email(String hostname, int port, String user, String password, boolean SSL) {
	    emailProperties = new Properties();
	    emailProperties.put("mail.host", hostname);
	    
	    if(SSL){
	        emailProperties.put("mail.smtps.auth", "true");
            emailProperties.put("mail.transport.protocol", "smtps");
        } else {
            emailProperties.put("mail.transport.protocol", "smtp");
        }
	    
	    this.hostname = hostname;
	    this.port = port;
	    this.user = user;
	    this.password = password;
	}
	
	public void send(String from, String recipient, String subject, String text) {
	    try {
    		Session session = Session.getDefaultInstance(emailProperties, null);
    		Transport transport = session.getTransport();
    			
    		Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(from));
            message.setSubject(subject);
            message.setText(text);
               
            transport.connect(hostname, port, user, password);
            transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            transport.close();
	    } catch(MessagingException me) {
	        Tools.printLog("MessagingException encountered while trying to send an email: "+me.getMessage());
            Tools.printStackTrace(me);
	    }
	}
	    
}