package twcore.core;
/*
 * PositionUpdator.java
 *
 * Created on October 24, 2002, 10:11 AM
 */

/**
 *
 * @author  harvey
 */
public class Ship extends Thread {

    public static int       MOVING_TIME = 100;
    public static int       UNMOVING_TIME = 1000;
    public static boolean   MOVEMENT_MOVING = true;
    public static boolean   MOVEMENT_UNMOVING = false;

    private int         x = 8192;
    private int         y = 8192;
    private int         xVel = 0;
    private int         yVel = 0;
    private int         bounty = 3;
    private int         energy = 1500;
    private byte        togglables = 0;
    private byte        direction = 0x0;
    private boolean     m_moving = MOVEMENT_UNMOVING;

    private GamePacketGenerator     m_gen;

    Ship( ThreadGroup group, GamePacketGenerator gen ){
        super( group, "Ship" );
        m_gen = gen;
    }

    public void move( int direction, int x, int y, int xVel, int yVel, int togglables, int energy, int bounty ){
        this.x = x;
        this.y = y;
        this.xVel = xVel;
        this.yVel = yVel;
        this.direction = (byte)direction;
        this.togglables = (byte)togglables;
        this.energy = energy;
        this.bounty = bounty;
        setMoving( true );
        sendPositionPacket();
    }

    public void move( int x, int y, int xVel, int yVel ){
        this.x = x;
        this.y = y;
        this.xVel = xVel;
        this.yVel = yVel;
        setMoving( true );
        sendPositionPacket();
    }

    public void move( int x, int y ){
        this.x = x;
        this.y = y;
        this.xVel = 0;
        this.yVel = 0;
        setMoving( false );
        sendPositionPacket();
    }

    public void setMoving( boolean moving ){
        m_moving = moving;
    }

    public void setRotation( int rotation ){
        direction = (byte)rotation;
        sendPositionPacket();
    }

    public void sendPositionPacket()
    {
        m_gen.sendPositionPacket( direction, (short)xVel, (short)y, (byte)togglables, (short)x, (short)yVel, (short)bounty, (short)energy, (short)0 );
    }

    public void fire( int weapon ){
        m_gen.sendPositionPacket( direction, (short)xVel, (short)y, (byte)togglables, (short)x, (short)yVel, (short)bounty, (short)energy, (short)weapon );
    }

    public void run(){
        try {
            while( !interrupted() ){
                sendPositionPacket();
                if( m_moving == MOVEMENT_MOVING ){
                    Thread.sleep( MOVING_TIME );
                } else {
                    Thread.sleep( UNMOVING_TIME );
                }
            }
        } catch( InterruptedException e ){
            return;
        }
    }

    public void rotateRadians( double rads ){
        rotateDegrees( Math.round( (int)Math.toDegrees( rads )) );
    }

    public void rotateDegrees( int degrees ){
        int rotate = (Math.round( (float)degrees / (float)9 ) + 10) % 40;
        setRotation( rotate );
    }

    public void setShip( int shipType ){
        m_gen.sendShipChangePacket( (short)shipType );
    }

    public void setFreq( int freq ){
        m_gen.sendFreqChangePacket( (short)freq );
    }

    public void attach( int playerId ){
        m_gen.sendAttachRequestPacket( (short)playerId );
    }

    public void unattach(){
        m_gen.sendAttachRequestPacket( (short)-1 );
    }
    
    /**
     * @return Returns the x.
     */
    public int getX(){
        return x;
    }
    /**
     * @return Returns the y.
     */
    public int getY(){
        return y;
    }
}
