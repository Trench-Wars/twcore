package twcore.core.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import twcore.core.game.ArenaSettings;
import twcore.core.util.ByteArray;

/**
    This static class handles all the checksum related calculations. Due to the complexity of most
    of this class' functions, one should not alter it unless he/she really knows what he/she is doing.
    It is best to just regard this as a black box.
    <p>
    Most of the routines in here have been ported from the in C++ written MervBot. All original research
    should be credited to them.
    @author Trancid

*/
public final class ChecksumGenerator {

    private static final int G4_MODIFIER   =    0x77073096;     // I am pretty sure these "constants"
    private static final int G16_MODIFIER  =    0x076dc419;     // are all constants.  That is to
    private static final int G64_MODIFIER  =    0x1db71064;     // say, maybe they are dependant on
    private static final int G256_MODIFIER =    0x76dc4190;     // the key provided to the algorithm.
    private static final long UINT32_MASK  =    4294967295L;    // Max value of an uint32. Java doesn't support uint32 through normal means...

    /** Hidden constructor */
    private ChecksumGenerator() {

    }

    /**
        This checksum generator is one of two possibilities to be used by the large send position packet.
        <p>
        Credit: Assembly rips by Coconut Emulator. Main code: MervBot.
        @param buffer The entire packet data.
        @param len Length of the packet data.
        @return The generated checksum
    */
    public static byte simpleChecksum(byte[] buffer, int len) {
        byte chksum = 0;

        for (int i = 0; i < len; ++i) {
            chksum ^= buffer[i];
        }

        return chksum;
    }

    /**
        This checksum generator is one of the two possibilities to be used by the large send position packet.
        @param buffer Packet data that will be check summed.
        @param len Length of the data.
        @return The generated checksum
    */
    public static byte simpleChecksum(ByteArray buffer, int len) {
        byte chksum = 0;

        for (int i = 0; i < len; ++i) {
            chksum ^= buffer.readByte(i);
        }

        return chksum;
    }

    /**
        Checksum generator for files. This checksum is to be used on verifying that local files
        match the files that the server has, like maps and the news.txt, through the provided checksum
        by the server. This function should generally not be called directly, but through {@link #getFileChecksum(String, long[])}.
        <p>
        Credits: Original research by Snrrrub and original code from MervBot
        @param buffer Data that the checksum will be generated for.
        @param dictionary The encoding dictionary. See: {@link #generateDictionary()}
        @param len Length of the buffer.
        @return A long key that mimics an unsigned 32 bit integer.
    */
    public static long getFileChecksum(byte[] buffer, long[] dictionary, int len) {
        // These variables need to be long to ensure the will behave like unsigned integers.
        long index = 0;
        long key   = UINT32_MASK;

        for (int i = 0; i < len; ++i) {
            // The weirdness has to do with signed/unsigned values and the length of the dictionary.
            index   = dictionary[(int) ((key & 255) ^ (((int)buffer[i]) & 255))];
            key     = (key >> 8) ^ index;
        }

        return ((~key) & UINT32_MASK);
    }

    /**
        Shell function for the file checksum generator. Call upon this instead of {@link #getFileChecksum(byte[], long[], int)}
        to have the file automatically read from disk and generate the checksum for it.
        @param fileName Filename including path and extension.
        @param dictionary The encoding dictionary. See: {@link #generateDictionary()}
        @return Generated checksum when successful. Otherwise a long containing the signed integer value -1, f.e. when the file is not present.
    */
    public static long getFileChecksum(String fileName, long[] dictionary) {
        long chksum = 0xffffffff;

        try {
            File file = new File(fileName);
            int length = (int) file.length();
            byte[] byteBuffer = new byte[length];
            FileInputStream fis = new FileInputStream(file);
            fis.read(byteBuffer);
            fis.close();

            chksum = getFileChecksum(byteBuffer, dictionary, length);
            // These are handled by the unaltered chksum value in the return below.
        } catch (FileNotFoundException e) {
            // File not found.
        } catch (NullPointerException e) {
            // Invalid file name.
        } catch (IOException e) {
            // Error when reading file.
        } catch (SecurityException e) {
            // Insufficient rights to read file.
        }

        return chksum;
    }

    /**
        Used in {@link #generateDictionary()} to generate a chunk of 4 long[]s of the dictionary.
        @param dictionary Original dictionary
        @param offset Offset.
        @param key Encoding key.
        @return The dictionary generated up to this point.
    */
    private static long[] generate4(long[] dictionary, int offset, long key) {
        dictionary[offset] = key;
        dictionary[offset + 1] = key ^ G4_MODIFIER;
        key = (key ^ (G4_MODIFIER << 1)) & UINT32_MASK;
        dictionary[offset + 2] = key;
        dictionary[offset + 3] = key ^ G4_MODIFIER;

        return dictionary;
    }

    /**
        Used in {@link #generateDictionary()} to generate a chunk of 16 long[]s of the dictionary.
        @param dictionary Original dictionary
        @param offset Offset.
        @param key Encoding key.
        @return The dictionary generated up to this point.
    */
    private static long[] generate16(long[] dictionary, int offset, long key) {
        dictionary = generate4(dictionary, offset, key);
        dictionary = generate4(dictionary, offset + 4, key  ^ G16_MODIFIER);
        key = (key ^ (G16_MODIFIER << 1)) & UINT32_MASK;
        dictionary = generate4(dictionary, offset + 8, key);
        dictionary = generate4(dictionary, offset + 12, key ^ G16_MODIFIER);
        return dictionary;
    }

    /**
        Used in {@link #generateDictionary()} to generate a chunk of 64 long[]s of the dictionary.
        @param dictionary Original dictionary
        @param offset Offset.
        @param key Encoding key.
        @return The dictionary generated up to this point.
    */
    private static long[] generate64(long[] dictionary, int offset, long key) {
        dictionary = generate16(dictionary, offset, key);
        dictionary = generate16(dictionary, offset + 16, key ^ G64_MODIFIER);
        key = (key ^ (G64_MODIFIER << 1)) & UINT32_MASK;
        dictionary = generate16(dictionary, offset + 32, key);
        dictionary = generate16(dictionary, offset + 48, key ^ G64_MODIFIER);
        return dictionary;
    }

    /**
        Generates a dictionary used in the file checksum generation, {@link #getFileChecksum(String, long[])}.
        <p>
        Due to the way Java works, the size of the dictionary is actually twice as big as preferred. The main cause for this
        is that all the values need to be unsigned 32 bit integers. This is not easily supported by Java though.
        That being said, this shouldn't be touched, unless you really know what you are doing.
        <p>
        Credits: The original routine that this was based on is from MervBot
        @return Generated dictionary.
    */
    public static long[] generateDictionary() {
        long key = 0;
        long[] dictionary = new long[256];
        dictionary = generate64(dictionary, 0, key);
        dictionary = generate64(dictionary, 64, key  ^ G256_MODIFIER);
        key = (key ^ (G256_MODIFIER << 1)) & UINT32_MASK;
        dictionary = generate64(dictionary, 128, key);
        dictionary = generate64(dictionary, 192, key ^ G256_MODIFIER);

        return dictionary;
    }

    /**
        Used to generate a checksum based on the contents of the map the bot is currently in. This checksum
        is mainly used in the security synchronization packet.
        <p>
        Credits: Coconot Emulator and Snrrrub for the assembly rips, MervBot for the original code.
        <p>
        This code was originally a pure assembly rip modified to work in C++. The variable names are therefore not original,
        they were register type names like EAX, EDX etc. In an attempt to make more clear what their role is, they have been
        renamed to what has been estimated to be their role.
        @param key The encoding key.
        @param mapData The map data for which the checksum is to be generated.
        @return The checksum
    */
    public static int generateLevelChecksum(int key, ByteArray mapData) {
        int startIndex, signedKey, offset, unsignedKey;
        int cnt;

        if (key < 0) {
            // key is negative
            signedKey = -((-key) & 0x1F); // Make the key positive, filter the last 5 bits, and make it negative again.

            if (signedKey >= 1024) // Don't think this can actually happen.
                return key;

        } else {
            // key is positive
            signedKey = key & 0x1F; // The signed key will have a value between 0 and 31 (5 bit)
        }

        // Take the remained of a division by 31 to generate the initial unsigned key.
        unsignedKey = key % 0x1F;

        // Offset. Max starting value will be 0b0111 1100 0001 1111
        offset = (signedKey << 0x0A) + unsignedKey;
        // Starting value of startIndex is 1024 - (0 ~ 31)
        startIndex = 1024 - unsignedKey;
        // Maximum starting value of cnt: 0b0100 0001 1111 - (0 ~ 31) >> 5 --> 0b0010 0000 = 32?
        cnt = (1024 + 31 - signedKey) >> 5;

        // Store the original key, since that is what will be used to encode it all.
        int original_key = key;

        // Loop through all the map data for cnt times, increase the offset by 5 byte each time.
        for (; cnt > 0; --cnt, offset += 0x8000) {
            int endIndex = startIndex + offset;
            int currIndex = offset;

            // Check in case the current address goes negative (out of bounds).
            if (offset >= endIndex)
                continue;

            // While our currIndex has not reach the end index yet, do this loop, increase address step by 5 bit each time.
            while (currIndex < endIndex) {
                int value = (int) (mapData.readByte(currIndex) & 0xFF);

                // When the current tile value is between 0 and 160 or is equal to 171, XOR the value with the original key and at it to the returning key.
                if (value != 0 && (value < 0xA1 || value == 0xAB))
                    key += (original_key ^ value);

                currIndex += 0x1F;
            }
        }

        return key;
    }

    /**
        Generates the parameter checksum based on the raw arena settings.
        <p>
        Credits: Coconot Emulator and Snrrrub for the assembly rips, MervBot for the original code.
        @param key Key used for the checksum generation.
        @param settings The raw arena settings data.
        @return The generated checksum
        @see ArenaSettings
    */
    public static int generateParameterChecksum(int key, ByteArray settings) {
        int chksum = 0;

        for (int i = 0; i < 357; ++i) {
            chksum += (settings.readLittleEndianInt(i * 4) ^ key);
        }

        return chksum;
    }

    /**
        Generates a hard coded checksum.
        <p>
        Credits: Coconot Emulator and Snrrrub for the assembly rips, MervBot for the original code.
        @param key Key that is to be used.
        @return Generated checksum
    */
    public static int generateEXEChecksum( int key ) {
        int part, checksum = 0;

        part = 0xc98ed41f;
        part += 0x3e1bc | key;
        part ^= 0x42435942 ^ key;
        part += 0x1d895300 | key;
        part ^= 0x6b5c4032 ^ key;
        part += 0x467e44 | key;
        part ^= 0x516c7eda ^ key;
        part += 0x8b0c708b | key;
        part ^= 0x6b3e3429 ^ key;
        part += 0x560674c9 | key;
        part ^= 0xf4e6b721 ^ key;
        part += 0xe90cc483 | key;
        part ^= 0x80ece15a ^ key;
        part += 0x728bce33 | key;
        part ^= 0x1fc5d1e6 ^ key;
        part += 0x8b0c518b | key;
        part ^= 0x24f1a96e ^ key;
        part += 0x30ae0c1 | key;
        part ^= 0x8858741b ^ key;
        checksum += part;

        part = 0x9c15857d;
        part += 0x424448b | key;
        part ^= 0xcd0455ee ^ key;
        part += 0x727 | key;
        part ^= 0x8d7f29cd ^ key;
        checksum += part;

        part = 0x824b9278;
        part += 0x6590 | key;
        part ^= 0x8e16169a ^ key;
        part += 0x8b524914 | key;
        part ^= 0x82dce03a ^ key;
        part += 0xfa83d733 | key;
        part ^= 0xb0955349 ^ key;
        part += 0xe8000003 | key;
        part ^= 0x7cfe3604 ^ key;
        checksum += part;

        part = 0xe3f8d2af;
        part += 0x2de85024 | key;
        part ^= 0xbed0296b ^ key;
        part += 0x587501f8 | key;
        part ^= 0xada70f65 ^ key;
        checksum += part;

        part = 0xcb54d8a0;
        part += 0xf000001 | key;
        part ^= 0x330f19ff ^ key;
        part += 0x909090c3 | key;
        part ^= 0xd20f9f9f ^ key;
        part += 0x53004add | key;
        part ^= 0x5d81256b ^ key;
        part += 0x8b004b65 | key;
        part ^= 0xa5312749 ^ key;
        part += 0xb8004b67 | key;
        part ^= 0x8adf8fb1 ^ key;
        part += 0x8901e283 | key;
        part ^= 0x8ec94507 ^ key;
        part += 0x89d23300 | key;
        part ^= 0x1ff8e1dc ^ key;
        part += 0x108a004a | key;
        part ^= 0xc73d6304 ^ key;
        part += 0x43d2d3 | key;
        part ^= 0x6f78e4ff ^ key;
        checksum += part;

        part = 0x45c23f9;
        part += 0x47d86097 | key;
        part ^= 0x7cb588bd ^ key;
        part += 0x9286 | key;
        part ^= 0x21d700f8 ^ key;
        part += 0xdf8e0fd9 | key;
        part ^= 0x42796c9e ^ key;
        part += 0x8b000003 | key;
        part ^= 0x3ad32a21 ^ key;
        checksum += part;

        part = 0xb229a3d0;
        part += 0x47d708 | key;
        part ^= 0x10b0a91 ^ key;
        checksum += part;

        part = 0x466e55a7;
        part += 0xc7880d8b | key;
        part ^= 0x44ce7067 ^ key;
        part += 0xe4 | key;
        part ^= 0x923a6d44 ^ key;
        part += 0x640047d6 | key;
        part ^= 0xa62d606c ^ key;
        part += 0x2bd1f7ae | key;
        part ^= 0x2f5621fb ^ key;
        part += 0x8b0f74ff | key;
        part ^= 0x2928b332;
        checksum += part;

        part = 0x62cf369a;
        checksum += part;

        return checksum;
    }
}
