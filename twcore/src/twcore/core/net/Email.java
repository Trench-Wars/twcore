package twcore.core.net;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.util.Properties;

import twcore.core.BotSettings;

public class Email {
	
	private String SMTP_HOST_NAME;
    private int SMTP_HOST_PORT = 465;
    private String SMTP_AUTH_USER;
    private String SMTP_AUTH_PASS;
    private int SSL;

	public Email(String[] args, BotSettings cfg) throws Exception{
		if(args.length != 3)return;
		SMTP_HOST_NAME = cfg.getString("MailHost");
		SMTP_HOST_PORT = cfg.getInt("MailPort");
		SMTP_AUTH_USER = cfg.getString("MailUser");
		SMTP_AUTH_PASS = cfg.getString("MailPass");
		SSL = cfg.getInt("SSL");
		Properties props = new Properties();
		props.put("mail.host", SMTP_HOST_NAME);
		if(SSL == 1){
		   	props.put("mail.smtps.auth", "true");
		   	props.put("mail.transport.protocol", "smtps");
		}
		else
		  	props.put("mail.transport.protocol", "smtp");
		Session session = Session.getDefaultInstance(props, null);
		Transport transport = session.getTransport();
			
		Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_AUTH_USER));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(args[0]));
        message.setSubject(args[1]);
        message.setText(args[2]);
           
        transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, SMTP_AUTH_USER, SMTP_AUTH_PASS);
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
	}
}