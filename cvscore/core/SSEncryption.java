package twcore.core;

public class SSEncryption {
    
    long      m_serverKey;
    long      m_clientKey;
    ByteArray m_table;
    
    public SSEncryption(){
        
        m_table = new ByteArray( 520 );
        
        m_serverKey = 0;
        m_clientKey = 0;
    }
    
    public SSEncryption( long clientKey, long serverKey ){
        
        m_table = new ByteArray( 520 );
        
        m_serverKey = serverKey;
        m_clientKey = clientKey;
        
        initialize( m_serverKey );
    }
    
    public SSEncryption( ByteArray clientKey, ByteArray serverKey ){
        
        m_table = new ByteArray( 520 );
        
        m_serverKey = serverKey.readLittleEndianInt( 0 );
        m_clientKey = clientKey.readLittleEndianInt( 0 );
        
        initialize( m_serverKey );
    }
    
    public void setClientKey( long newClientKey ){
        
        m_clientKey = newClientKey;
    }
    
    public void setClientKey( ByteArray newClientKey ){
        
        m_clientKey = newClientKey.readLittleEndianInt( 0 );
        
        initialize( m_serverKey );
    }
    
    public void setServerKey( long newServerKey ){
        
        m_serverKey = newServerKey;
        
        initialize( m_serverKey );
    }
    
    public void setServerKey( ByteArray newServerKey ){
        
        m_serverKey = newServerKey.readLittleEndianInt( 0 );
        
        initialize( m_serverKey );
    }
    
    private void initialize( long seed ){
        long        oldSeed;
        long        tempSeed = seed;
        
        m_table.setPointerIndex( 0 );
        for( int i = 0; i < (520 / 2); i++ ){
            oldSeed = tempSeed;
            
            tempSeed = ((oldSeed * 0x834E0B5FL) >> 48) & 0xffffffffL;
            tempSeed = ((tempSeed + (tempSeed >> 31)) & 0xffffffffL);
            tempSeed = ((((oldSeed % 0x1F31DL) * 16807) - (tempSeed * 2836) + 123) & 0xffffffffL);
            if( tempSeed > 0x7fffffffL ){
                tempSeed = ((tempSeed + 0x7fffffffL) & 0xffffffffL);
            }
            
            m_table.addLittleEndianShort( (short)(tempSeed & 0xffff) );
        }
    }
    
    public void encrypt( ByteArray data, int length ){
        
        encrypt( data, length, 0 );
    }
    
    public void encrypt( ByteArray data, int length, int index ){
        long           tempInt;
        long           tempKey = m_serverKey;
        int            count = data.size() + (4 - data.size()%4);
        ByteArray      output = new ByteArray( count );
        
        output.addPartialByteArray( data, 0, index, length );
        for( int i=0; i<count; i+=4 ){
            tempInt = output.readLittleEndianInt( i ) ^ m_table.readLittleEndianInt( i ) ^ tempKey;
            tempKey = tempInt;
            output.addLittleEndianInt( (int)(tempInt & 0xffffffff), i );
        }
        
        data.addPartialByteArray( output, index, 0, length );
    }
    
    public void decrypt( ByteArray data, int length ){
        
        decrypt( data, length, 0 );
    }
    
    public void decrypt( ByteArray data, int length, int index ){
        long           tempInt;
        long           tempKey = m_serverKey;
        int            count = data.size() + (4 - data.size()%4);
        ByteArray      output = new ByteArray( count );
        
        output.addPartialByteArray( data, 0, index, length );
        for( int i=0; i<count; i+=4 ){
            tempInt = m_table.readLittleEndianInt( i ) ^ tempKey ^ output.readLittleEndianInt( i );
            tempKey = output.readLittleEndianInt( i );
            output.addLittleEndianInt( (int)(tempInt & 0xffffffff), i );
        }
        
        data.addPartialByteArray( output, index, 0, length );
    }
}

