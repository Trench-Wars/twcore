package twcore.core;

import java.io.*;
import java.net.DatagramPacket;

public class ByteArray {

    byte[]     m_array;
    int        m_pointer=0;

    public ByteArray(){
    }

    public ByteArray( int size ){

        m_array = new byte[size];
    }

    public ByteArray( DatagramPacket packet ){

        m_array = new byte[packet.getLength()];
        System.arraycopy( packet.getData(), 0, m_array, 0, packet.getLength() );
    }

    public ByteArray( byte[] byteArray ){

        m_array = new byte[byteArray.length];

        addByteArray( byteArray );
    }

    public ByteArray( int[] intArray ){

        m_array = new byte[intArray.length];

        addByteArray( intArray );
    }

    public int size(){
        return m_array.length;
    }

    public void growArray( int newSize ){
        byte[]    newarray = new byte[newSize];

        System.arraycopy( m_array, 0, newarray, 0, m_array.length );
        m_array = newarray;
    }

    public void shrinkArray( int newSize ){
        byte[]    newarray = new byte[newSize];

        System.arraycopy( m_array, 0, newarray, 0, newSize );
        m_array = newarray;
    }

    public void setPointerIndex( int newpointer ){

        m_pointer = newpointer;
    }

    public int getPointerIndex(){

        return m_pointer;
    }

    public void incrementPointer(){

        m_pointer++;
    }

    public void decrementPointer(){
        if( m_pointer != 0 ){
            m_pointer++;
        }
    }

    public void addByte( byte theByte ){
        m_array[m_pointer++] = theByte;
    }

    public void addByte( byte theByte, int index ){
        m_array[index] = theByte;
    }

    public void addByte( int theByte ){
        m_array[m_pointer++] = (byte)((theByte) & 0xff);
    }

    public void addByte( int theByte, int index ){
        m_array[index] = (byte)((theByte) & 0xff);
    }

    public void addShort( short theShort ){
        m_array[m_pointer++] = (byte)((theShort>>8) & 0xff);
        m_array[m_pointer++] = (byte)((theShort) & 0xff);
    }

    public void addShort( short theShort, int index ){
        m_array[index] = (byte)((theShort>>8) & 0xff);
        m_array[index+1] = (byte)((theShort) & 0xff);
    }

    public void addLittleEndianShort( short theShort ){
        m_array[m_pointer++] = (byte)((theShort) & 0xff);
        m_array[m_pointer++] = (byte)((theShort>>8) & 0xff);
    }

    public void addLittleEndianShort( short theShort, int index ){
        m_array[index] = (byte)((theShort) & 0xff);
        m_array[index+1] = (byte)((theShort>>8) & 0xff);
    }

    public void addInt( int theInt ){
        m_array[m_pointer++] = (byte)((theInt>>24) & 0xff);
        m_array[m_pointer++] = (byte)((theInt>>16) & 0xff);
        m_array[m_pointer++] = (byte)((theInt>>8) & 0xff);
        m_array[m_pointer++] = (byte)((theInt) & 0xff);
    }

    public void addInt( int theInt, int index ){
        m_array[index] = (byte)((theInt>>24) & 0xff);
        m_array[index+1] = (byte)((theInt>>16) & 0xff);
        m_array[index+2] = (byte)((theInt>>8) & 0xff);
        m_array[index+3] =  (byte)((theInt) & 0xff);
    }

    public void addLittleEndianInt( int theInt ){
        m_array[m_pointer++] = (byte)((theInt) & 0xff);
        m_array[m_pointer++] = (byte)((theInt>>8) & 0xff);
        m_array[m_pointer++] = (byte)((theInt>>16) & 0xff);
        m_array[m_pointer++] = (byte)((theInt>>24) & 0xff);
    }

    public void addLittleEndianInt( int theInt, int index ){
        m_array[index] = (byte)((theInt) & 0xff);
        m_array[index+1] = (byte)((theInt>>8) & 0xff);
        m_array[index+2] = (byte)((theInt>>16) & 0xff);
        m_array[index+3] =  (byte)((theInt>>24) & 0xff);
    }

    public void addLong( long theLong ){
        m_array[m_pointer++] = (byte)((theLong>>56) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>48) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>40) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>32) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>24) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>16) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>8) & 0xff);
        m_array[m_pointer++] = (byte)((theLong) & 0xff);
    }

    public void addLong( long theLong, int index ){
        m_array[index] = (byte)((theLong>>56) & 0xff);
        m_array[index+1] = (byte)((theLong>>48) & 0xff);
        m_array[index+2] = (byte)((theLong>>40) & 0xff);
        m_array[index+3] = (byte)((theLong>>32) & 0xff);
        m_array[index+4] = (byte)((theLong>>24) & 0xff);
        m_array[index+5] = (byte)((theLong>>16) & 0xff);
        m_array[index+6] = (byte)((theLong>>8) & 0xff);
        m_array[index+7] = (byte)((theLong) & 0xff);
    }

    public void addLittleEndianLong( long theLong ){
        m_array[m_pointer++] = (byte)((theLong) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>8) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>16) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>24) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>32) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>40) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>48) & 0xff);
        m_array[m_pointer++] = (byte)((theLong>>56) & 0xff);
    }

    public void addLittleEndianLong( long theLong, int index ){
        m_array[index] = (byte)((theLong) & 0xff);
        m_array[index+1] = (byte)((theLong>>8) & 0xff);
        m_array[index+2] = (byte)((theLong>>16) & 0xff);
        m_array[index+3] = (byte)((theLong>>24) & 0xff);
        m_array[index+4] = (byte)((theLong>>32) & 0xff);
        m_array[index+5] = (byte)((theLong>>40) & 0xff);
        m_array[index+6] = (byte)((theLong>>48) & 0xff);
        m_array[index+7] = (byte)((theLong>>56) & 0xff);
    }

    public void addString( String str ){
        byte[] bytearr = str.getBytes();

        System.arraycopy( bytearr, 0, m_array, m_pointer, bytearr.length );
        m_pointer += bytearr.length;
    }

    public void addString( String str, int index ){
        byte[] bytearr = str.getBytes();

        System.arraycopy( bytearr, 0, m_array, index, bytearr.length );
    }

    public void addPaddedString( String str, int totalLength ){
        if( str == null ){
            repeatAdd( 0x0, totalLength );
        } else {
            byte[] bytearr = str.getBytes();

            System.arraycopy( bytearr, 0, m_array, m_pointer, bytearr.length );
            m_pointer += bytearr.length;

            if( totalLength > str.length() ){
                repeatAdd( 0x0, totalLength - str.length() );
            }
        }
    }

    public void addPaddedString( String str, int index, int totalLength ){
        if( str == null ){
            repeatAdd( 0x0, totalLength, index );
        } else {
            byte[] bytearr = str.getBytes();

            System.arraycopy( bytearr, 0, m_array, index, bytearr.length );
            if( totalLength > str.length() ){
                repeatAdd( 0x0, totalLength - str.length(), index );
            }
        }
    }

    public void addByteArray( ByteArray byteArray ){
        byte[]        tempArray = byteArray.getByteArray();

        System.arraycopy( tempArray, 0, m_array, m_pointer, tempArray.length );

        m_pointer += tempArray.length;
    }

    public void addByteArray( ByteArray byteArray, int index ){
        byte[]        tempArray = byteArray.getByteArray();

        System.arraycopy( tempArray, 0, m_array, index, tempArray.length );
    }

    public void addByteArray( byte[] byteArray ){

        System.arraycopy( byteArray, 0, m_array, m_pointer, byteArray.length );

        m_pointer += byteArray.length;
    }

    public void addByteArray( byte[] byteArray, int index ){

        System.arraycopy( byteArray, 0, m_array, index, byteArray.length );
    }

    public void addByteArray( int[] intArray ){

        for( int i=0; i<intArray.length; i++ ){
            m_array[m_pointer++] = (byte)((intArray[i]) & 0xff);
        }

        m_pointer += intArray.length;
    }

    public void addByteArray( int[] intArray, int index ){

        for( int i=0; i<intArray.length; i++ ){
            m_array[index+i] = (byte)((intArray[i]) & 0xff);
        }
    }

    public void addFileContents( File file ) throws IOException {
        int              length;
        byte             buffer[];
        FileInputStream  fileReader;

        length = (int)file.length();
        buffer = new byte[length];

        fileReader = new FileInputStream( file );
        fileReader.read( buffer, 0, length );
        addByteArray( buffer );
        fileReader.close();
    }

    public void addPartialByteArray( ByteArray byteArray, int sourceIndex, int length ){
        byte[]        tempArray = byteArray.getByteArray();

        System.arraycopy( tempArray, sourceIndex, m_array, m_pointer, length );
        m_pointer += length;
    }

    public void addPartialByteArray( ByteArray byteArray, int destIndex, int sourceIndex, int length ){
        byte[]        tempArray = byteArray.getByteArray();

        System.arraycopy( tempArray, sourceIndex, m_array, destIndex, length );
    }

    public void addPartialByteArray( byte[] byteArray, int sourceIndex, int length ){

        System.arraycopy( byteArray, sourceIndex, m_array, m_pointer, length );
        m_pointer += length;
    }

    public void addPartialByteArray( byte[] byteArray, int destIndex, int sourceIndex, int length ){

        System.arraycopy( byteArray, sourceIndex, m_array, destIndex, length );
    }

    public void addPartialByteArray( int[] intArray, int sourceIndex, int length ){

        for( int i=0; i<length; i++ ){
            m_array[m_pointer++] = (byte)((intArray[i + sourceIndex]) & 0xff);
        }
    }

    public void addPartialByteArray( int[] intArray, int destIndex, int sourceIndex, int length ){

        for( int i=0; i<length; i++ ){
            m_array[destIndex+i] = (byte)((intArray[i + sourceIndex]) & 0xff);
        }
    }

    public void repeatAdd( byte theByte, int number ){
        for( int i=0; i<number; i++ ){
            m_array[m_pointer++] = theByte;
        }
    }

    public void repeatAdd( byte theByte, int number, int index ){
        int        oldPointer = m_pointer;

        for( int i=0; i<number; i++ ){
            m_array[index+i] = theByte;
        }
    }

    public void repeatAdd( int theInt, int number ){
        for( int i=0; i<number; i++ ){
            m_array[m_pointer++] = (byte)(theInt & 0xff);
        }
    }

    public void repeatAdd( int theInt, int number, int index ){
        for( int i=0; i<number; i++ ){
            m_array[index+i] = (byte)(theInt & 0xff);
        }
    }

    public byte[] getByteArray(){
        return m_array;
    }

    public byte readByte( int index ){
        return m_array[index];
    }

    public short readShort( int index ){
        return (short)(((m_array[index] & 0xff)<<8) | ((m_array[index+1] & 0xff)));
    }

    public short readLittleEndianShort( int index ){
        return (short)(((m_array[index+1] & 0xff)<<8) | ((m_array[index] & 0xff)));
    }

    public int readInt( int index ){
        return (int)(((m_array[index] & 0xff)<<24) | ((m_array[index+1] & 0xff)<<16) |
        ((m_array[index+2] & 0xff)<<8) | ((m_array[index+3] & 0xff)));
    }

    public int readLittleEndianInt( int index ){
        return (int)(((m_array[index+3] & 0xff)<<24) | ((m_array[index+2] & 0xff)<<16) |
        ((m_array[index+1] & 0xff)<<8) | ((m_array[index] & 0xff)));
    }

    public long readLong( int index ){
        return (long)(((m_array[index] & 0xff)<<56) | ((m_array[index+1] & 0xff)<<48) |
        ((m_array[index+2] & 0xff)<<40) | ((m_array[index+3] & 0xff)<<32) |
        ((m_array[index+4] & 0xff)<<24) | ((m_array[index+5] & 0xff)<<16) |
        ((m_array[index+6] & 0xff)<<8) | ((m_array[index+7] & 0xff)));
    }

    public long readLittleEndianLong( int index ){
        return (long)(((m_array[index+7] & 0xff)<<56) | ((m_array[index+6] & 0xff)<<48) |
        ((m_array[index+5] & 0xff)<<40) | ((m_array[index+4] & 0xff)<<32) |
        ((m_array[index+3] & 0xff)<<24) | ((m_array[index+2] & 0xff)<<16) |
        ((m_array[index+1] & 0xff)<<8) | ((m_array[index] & 0xff)));
    }

    public String readString( int index, int length ){
        char[]        charArray = new char[length];

		//This old method couldn't handle special characters
        /*for( int i=0; i<length; i++ ){
            charArray[i] = (char)(m_array[index+i]);
        }

        return new String( charArray ).trim();*/

        //New method can (Sorry Sika) - D1st0rt
        return new String(m_array, index, length).trim();
    }

    public String readNullTerminatedString( int index ){
        int           i = 0;

        while( m_array[index + i] != '\0' ){
            i++;
        }

        return readString( index, i );
    }

    public ByteArray readByteArray( int begin, int end ){
        byte[] newarray = new byte[end-begin+1];
        for( int i=0; i<end+1-begin; i++){
            newarray[i] = m_array[i+begin];
        }
        return new ByteArray(newarray);
    }

    public static void show( byte[] b ){
        for( int i=0; i<b.length; i++ ){
            System.out.print( ConvertHex.byteToHex( b[i] ) + " " );
        }

        System.out.println();
    }

    public static void showShort( byte[] b ){
        for( int i=0; i<b.length && i<26; i++ ){
            System.out.print( ConvertHex.byteToHex( b[i] ) + " " );
        }

        System.out.println();
    }

    public static void showShort( ByteArray byteArray ){
        byte[] b = byteArray.getByteArray();
        for( int i=0; i<b.length && i<26; i++ ){
            System.out.print( ConvertHex.byteToHex( b[i] ) + " " );
        }

        System.out.println();
    }

    public void show(){
        for( int i=0; i<m_array.length; i++ ){
            System.out.print( ConvertHex.byteToHex( m_array[i] ) + " " );
        }

        System.out.println();
    }

    public void showShort(){
        for( int i=0; i<m_array.length && i<26; i++ ){
            System.out.print( ConvertHex.byteToHex( m_array[i] ) + " " );
        }
    }

    public static byte[] toByteArray( int[] intarray ){
        byte[] barray = new byte[intarray.length];

        for( int i=0; i<intarray.length; i++ ){
            barray[i] = (byte)((intarray[i]) & 0xff);
        }

        return barray;
    }
}

