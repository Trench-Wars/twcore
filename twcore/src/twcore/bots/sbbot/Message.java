package twcore.bots.sbbot;

public class Message {
    private final Object contents;
    String text;

    public Message(Object c, String t) {
	contents = c;
	text = t;
    }

    public Message(Object c) {
	contents = c;
	text = (String) null;
    }

    public Message(String m) {
	text = m;
	contents = (Object) null;
    }
    public Message() {
	contents = (Object) null;
	text = (String) null;
    }


    public Object getContents() { return contents; }
    public String getText() { return text; }
}