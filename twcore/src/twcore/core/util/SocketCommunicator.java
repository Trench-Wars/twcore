package twcore.core.util;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import twcore.core.SubspaceBot;
import twcore.core.events.SocketMessageEvent;


/**
 * Main class of the Socket Communication protocol of TWCore, handling routing
 * of messages from a socket along specific channels.  Also handles channel subscriptions,
 * creation/deletion of channels, and firing of SocketMessageEvent.
 *
 * @author  arobas+ (most of the code from IPC)
 */
public class SocketCommunicator extends Thread {


	private Map<String, SocketChannel> channels;

	private ServerSocket servsock;
	private boolean running = true;

	private int port = 6969;
	private int timeout = (2 * 1000); // 2 seconds
	private boolean debug = false;

	/** Creates a new instance of RadioStatusServer */
	public SocketCommunicator(int port) {

		this.port = port;
		channels = new HashMap<String, SocketChannel>();
		
		try {
			servsock = new ServerSocket();
			servsock.bind(new InetSocketAddress("127.0.0.1",port));
			servsock.setSoTimeout(timeout);
			
		} catch (IOException ioe) {
			Tools.printLog("IOException occured opening port (" + port + "):" + ioe.getMessage());
			Tools.printStackTrace(ioe);
		}

		if (debug)
			System.out.println("SocketCommunicator server started on port " + port);
		start();
	}

	public void run() {

		while (running) {

			try {
				Socket socket = servsock.accept();
				socket.setSoTimeout(timeout);
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String response = "";

				if (debug) {
					System.out.println("SocketServer: Client connected ("
									+ socket.getInetAddress()
									+ ":"
									+ socket.getPort() + ")");
				}

				String command = in.readLine();

				if (command != null)
					command = command.trim();

				if (debug) {
					System.out.println("SocketServer: -> " + command);
				}

				if (command != null && command.indexOf(":") != -1) {
					
					String channelName = command.substring(0, command.indexOf(":"));
					command = command.substring(command.indexOf(":")+1);
					
					if (debug)
						System.out.println("command recognized, responding...");

					SocketMessageEvent event = new SocketMessageEvent(null,
							channelName, command);
					
					Thread eventThread = new Thread(event); 
					eventThread.start();
					
					broadcast(channelName, event);
					
					try {
						synchronized (eventThread) {
							eventThread.wait();
						}
					} catch (InterruptedException e) { }
	
					response = event.getResponse();

				}

				if (response != null) {
					out.writeUTF(response +"\n");
				}

				socket.close();

				if (debug) {
					System.out.println("SocketServer: Client disconnected");
				}

			} catch (SocketTimeoutException ste) {

			} catch (IOException ioe) {
				Tools.printLog("SocketServer TCP Communication Error: "
								+ ioe.getMessage());
			}
		}
	}

	public void die() {

		System.out.println("SocketServer: Attempting to kill all connections...");
		running = false;
		try {
			servsock.close();
		} catch (IOException ioe) {
			Tools.printLog("SocketServer: Closed the block accept thread.");
		}
	}



	/**
	 * Check if a channel exists.
	 * 
	 * @param channelName
	 *            Name of channel
	 * @return True if channel exists
	 */
	public synchronized boolean channelExists(String channelName) {
		return channels.containsKey(channelName);
	}

	/**
	 * Broadcast a message to a specific IPC channel.
	 * 
	 * @param channelName
	 *            Name of the channel to broadcast to
	 * @param senderName
	 *            Name of message sender
	 * @param bot
	 *            SubspaceBot object of bot executing command
	 * @param o
	 *            Object, generally an IPCMessage, to transmit
	 */
	public synchronized void broadcast(String channelName, SocketMessageEvent event) {

		if (channels.containsKey(channelName)) {
			SocketChannel channel = channels.get(channelName);
			channel.broadcast(event);
		}
		else {
			event.setResponse("NO LISTENER");
		}
	}

	/**
	 * Given a SubspaceBot, return bot's subscribed channels.
	 * 
	 * @param bot
	 *            SubspaceBot in question
	 * @return String[] containing all subscribed channels
	 */
	public synchronized String[] getSubscribedChannels(SubspaceBot bot) {
		synchronized (channels) {
			Iterator<SocketChannel> i = channels.values().iterator();
			ArrayList<String> list = new ArrayList<String>();
			while (i.hasNext()) {
				SocketChannel ipc = (SocketChannel) i.next();
				if (ipc.isSubscribed(bot)) {
					list.add(ipc.getName());
				}
			}
			return list.toArray(new String[list.size()]);
		}
	}

	/**
	 * Subscribe a bot to a given channel. If the channel does not exist, it is
	 * created.
	 * 
	 * @param channel
	 *            String containing channel to join
	 * @param bot
	 *            SubspaceBot to subscribe
	 */
	public synchronized void subscribe(String channel, SubspaceBot bot) {
		if (bot == null) {
			Tools
					.printLog("IPC Subscribe failed.  Please subscribe your bot "
							+ "to IPC in the LoggedOn handler, not in the constructor.");
			return;
		}
		if (!channelExists(channel)) {
			channels.put(channel, new SocketChannel(channel));
		}
		SocketChannel ipcChan = channels.get(channel);
		if (!ipcChan.isSubscribed(bot)) {
			ipcChan.subscribe(bot);
		}
	}

	/**
	 * Unsubscribe a bot from a given channel.
	 * 
	 * @param channel
	 *            String containing channel to unsubscribe from
	 * @param bot
	 *            SubspaceBot to unsubscribe
	 */
	public synchronized void unSubscribe(String channel, SubspaceBot bot) {
		if (!channelExists(channel))
			return;
		channels.get(channel).unsubscribe(bot);
	}

	/**
	 * Kill a given channel.
	 * 
	 * @param channel
	 *            String containing channel to kill.
	 */
	public synchronized void destroy(String channel) {
		channels.remove(channel);
	}

	/**
	 * Remove bot from all channels.
	 * 
	 * @param bot
	 *            SubspaceBot to unsubscribe.
	 */
	public synchronized void removeFromAll(SubspaceBot bot) {
		synchronized (channels) {
			Iterator<SocketChannel> i = channels.values().iterator();
			while (i.hasNext()) {
				SocketChannel channel = (SocketChannel) i.next();
				channel.unsubscribe(bot);
				if (channel.isEmpty()) {
					i.remove();
				}
			}
		}
	}

	/**
	 * Internal class of InterProcessCommunicator, IPCChannel
	 * 
	 * Representation of an IPC communications channel in the IPC message
	 * protocol.
	 */
	class SocketChannel {
		
		private List<SubspaceBot> bots;
		private String channel;

		public SocketChannel(String channelName) {
			
			bots = Collections.synchronizedList(new ArrayList<SubspaceBot>());
			channel = channelName;
		}

		/**
		 * Checks subscription status of a bot on this channel.
		 * 
		 * @param bot
		 *            SubspaceBot to check
		 * @return True if bot is subscribed to this channel
		 */
		public boolean isSubscribed(SubspaceBot bot) {
			return bots.contains(bot);
		}

		/**
		 * @return name of channel
		 */
		public String getName() {
			return channel;
		}

		/**
		 * Broadcast an SocketMessageEvent containing a message over this
		 * channel.
		 * 
		 * @param e
		 *            InterProcessEvent to broadcast
		 */
		public void broadcast(SocketMessageEvent e) {
			synchronized (bots) {
				Iterator<SubspaceBot> i = bots.iterator();
				while (i.hasNext()) {
					(i.next()).handleEvent(e);
				}
			}
		}

		/**
		 * Subscribe a bot to this channel.
		 * 
		 * @param bot
		 *            SubspaceBot to subscribe
		 */
		public void subscribe(SubspaceBot bot) {
			if (!bots.contains(bot)) {
				bots.add(bot);
			}
		}

		/**
		 * Unsubscribe a bot from this channel.
		 * 
		 * @param bot
		 *            SubspaceBot to unsubscribe
		 */
		public void unsubscribe(SubspaceBot bot) {
			bots.remove(bot);
		}

		/**
		 * @return True if no bots are subscribed to this channel
		 */
		public boolean isEmpty() {
			return bots.size() == 0;
		}

    }

}
