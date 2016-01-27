package twcore.bots.chatgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import twcore.core.BotAction;
import twcore.core.events.Message;

public abstract class Chat extends JPanel {

    /**

    */
    public static final Color CHAT_BLUE = new Color(160, 160, 255);
    public static final Color CHAT_GREEN = new Color(64, 255, 64);
    public static final Color CHAT_RED = new Color(255, 128, 0);
    public static final Color CHAT_YELLOW = new Color(255, 240, 64);
    public static final Color CHAT_WARNING = new Color(255, 0, 0);

    private static final long serialVersionUID = 1L;
    protected String name;
    protected Client client;
    protected BotAction m_botAction;

    protected JTextPane chatArea;
    protected JTextField input;

    protected StyleContext context;
    protected StyledDocument document;
    private Style style;

    public Chat(String name, Client client, BotAction botAction) {

        this.name = name;
        this.client = client;
        this.m_botAction = botAction;

        this.chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setAutoscrolls(true);
        chatArea.setBackground(Color.BLACK);

        context = new StyleContext();
        document = new DefaultStyledDocument(context);
        style = context.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontSize(style, 12);
        StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
        StyleConstants.setFontFamily(style, "Verdana");



        final JScrollPane scrollingArea = new JScrollPane(chatArea);
        scrollingArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollingArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollingArea.setAutoscrolls(true);
        scrollingArea.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            BoundedRangeModel brm = scrollingArea.getVerticalScrollBar().getModel();
            boolean wasAtBottom = true;

            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!brm.getValueIsAdjusting()) {
                    if (wasAtBottom)
                        brm.setValue(brm.getMaximum());
                } else
                    wasAtBottom = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());

            }
        });




        input = new JTextField();
        input.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage(input.getText());
                    input.setText("");
                }
            }
        });

        setLayout(new BorderLayout());
        add(input, BorderLayout.SOUTH);
        add(scrollingArea, BorderLayout.CENTER);



    }

    public void focusOnInput() {
        input.requestFocusInWindow();
    }

    public void appendText(String text, int MESSAGE_TYPE) {

        switch (MESSAGE_TYPE) {
        case Message.PUBLIC_MESSAGE:
            StyleConstants.setForeground(style, CHAT_BLUE);
            break;

        case Message.CHAT_MESSAGE:
            StyleConstants.setForeground(style, CHAT_RED);
            break;

        case Message.ARENA_MESSAGE:
            StyleConstants.setForeground(style, CHAT_GREEN);
            break;

        case Message.PRIVATE_MESSAGE:
            StyleConstants.setForeground(style, CHAT_GREEN);
            break;

        case Message.TEAM_MESSAGE:
            StyleConstants.setForeground(style, CHAT_YELLOW);
            break;

        default:
            StyleConstants.setForeground(style, CHAT_BLUE);
            break;
        }

        try {
            document.insertString(document.getLength(), text + "\r\n", style);
        }
        catch (BadLocationException ex) {
            System.err.println(ex);
        }

        chatArea.setDocument(document);
    }

    public abstract void sendMessage(String message);
    public abstract String getTitle();
}
