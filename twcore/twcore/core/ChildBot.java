package twcore.core;

public class ChildBot {
    private Session      m_bot;
    private String       m_creator;
    private String       m_className;
    
    ChildBot( String className, String creator, Session bot ){
        m_bot = bot;
        m_creator = creator;
        m_className = className;
    }
    
    public String getClassName(){
        return m_className;
    }
    
    public String getCreator(){
        return m_creator;
    }
    
    public Session getBot(){
        return m_bot;
    }
}