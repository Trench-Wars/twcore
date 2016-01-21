package twcore.core.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import twcore.core.util.ByteArray;
import twcore.core.util.Point;

/**
    This class stores all the information of a map, tile wise.<code><pre>
    VIE map format

    X    = WORD((N >> 12) & 0xFFF);  // This puts actual map dimensions at 4095x4095 possible tiles.
    Y    = WORD(N & 0xFFF);
    TILE = BYTE(v >> 24);

    0        = No tile
    1-19     = Normal tiles
    20       = Border
    21-161   = Normal tiles
    162-165  = Vertical doors
    166-169  = Horizontal doors
    170      = Flag
    171      = Safe zone
    172      = Goal area
    173-175  = Fly over tiles
    176-191  = Fly under tiles

    Warning: Deviating from this format may invalidate the security checksum.</pre></code>
    Original field research is credited to the folks from MervBot.
    Implementation idea is taken from the MervBot.
    @author Trancid
*/
public class LvlMap {
    private static final int TILE_MAX_X = 0x400;            // Maximum amount of tiles horizontally.
    private static final int TILE_MAX_Y = 0x400;            // Maximum amount of tiles vertically.
    private static final int TILE_MAX_LINEAR = 0x100000;    // Maximum amount of tiles when the array is linearized.

    public static final byte SOLID     = 0x0;
    public static final byte PERMEABLE = 0x1;
    public static final byte DOOR      = 0x2;
    public static final byte WORMHOLE  = 0x3;
    public static final byte BRICK     = 0x4;


    /*
        VIE tile constants.
    */
    public static final char vieNoTile = 0;

    public static final char vieNormalStart = 1;
    public static final char vieBorder = 20;            // Borders are not included in the .lvl files
    public static final char vieNormalEnd = 161;        // Tiles up to this point are part of sec.chk

    public static final char vieVDoorStart = 162;
    public static final char vieVDoorEnd = 165;

    public static final char vieHDoorStart = 166;
    public static final char vieHDoorEnd = 169;

    public static final char vieTurfFlag = 170;

    public static final char vieSafeZone = 171;         // Also included in sec.chk

    public static final char vieGoalArea = 172;

    public static final char vieFlyOverStart = 173;
    public static final char vieFlyOverEnd = 175;
    public static final char vieFlyUnderStart = 176;
    public static final char vieFlyUnderEnd = 190;

    public static final char vieAsteroidStart = 216;
    public static final char vieAsteroidEnd = 218;

    public static final char vieStation = 219;

    public static final char vieWormhole = 220;

    public static final char ssbTeamBrick = 221;        // These are internal
    public static final char ssbEnemyBrick = 222;

    public static final char ssbTeamGoal = 223;
    public static final char ssbEnemyGoal = 224;

    public static final char ssbTeamFlag = 225;
    public static final char ssbEnemyFlag = 226;

    public static final char ssbPrize = 227;

    public static final char ssbBorder = 228;           // Use ssbBorder instead of vieBorder to fill border

    private ByteArray mapData = new ByteArray(TILE_MAX_LINEAR); // Stores the parsed map data.
    private boolean gotMap = false;                             // Enabled on a successful parse.

    /**
        LvlMap constructor.
        @param filename Filename of map file, including path and extension.
    */
    public LvlMap(String filename) {
        convertFileToMatrix(filename);
    }

    /**
        Converts a given x, y and tile type to tile data.
        @param x X-coordinate in tiles.
        @param y Y-coordinate in tiles.
        @param type Type of tile. (See VIE constants)
        @return Serialized tile data.
    */
    public int makeTileData(int x, int y, char type) {
        return (x & 0xFFF) | ((y & 0xFFF) << 12) | (type << 24);
    }

    /**
        Converts a given x, y and tile type to the TileData class.
        @param raw Raw, serialized tile data.
        @return TileData
    */
    public TileData makeTileData(int raw) {
        return new TileData((int) (raw & 0xFFF), (int) ((raw >> 12) & 0xFFF), (char) (raw >> 24));
    }

    //////// Map coordinates ////////
    // Enormous amount of timed saved by FACTS team
    /**
        Converts a Y-coordinate into the Y-coordinate that is displayed to players.
        @param y Y-coordinate in tiles.
        @return Y-coordinate as numeral.
    */
    public int getNumeric(int y) {
        return (y * 20 / TILE_MAX_Y) + 1;
    }

    /**
        Converts a X-coordinate into the X-coordinate that is displayed to players.
        @param x X-coordinate in tiles.
        @return X-coordinate as letter.
    */
    public char getAlpha(int x) {
        return (char) ('A' + x * 20 / TILE_MAX_X);
    }

    /**
        Converts a given coordinate into the type of coordinate that is displayed to players (i.e. A1, K9 etc).
        @param x X-coordinate in tiles.
        @param y Y-coordinate in tiles.
        @return Accompanied alphanumeric coordinate.
    */
    public String getCoords(int x, int y) {
        String s = getAlpha(x) + Integer.toString(getNumeric(y));

        return s;
    }

    /**
        Converts a x- and y-index into a linearized index.
        @param x X-coordinate, in tiles.
        @param y Y-coordinate, in tiles.
        @return Linearized coordinate, basically x + 1024 * y
    */
    public int getLinear(int x, int y) {
        return (int) (((y & 1023) << 10) | (x & 1023));
    }

    /**
        Reads in a file and parses its contents into tile data and map data.
        @param filename Filename, including path and extension.
    */
    void convertFileToMatrix(String filename) {
        // Load the file into memory, before starting to process it.
        ByteArray rawData = loadMap(filename);
        int index = 0;

        if(rawData == null)
            return;

        int length = rawData.size();

        if(length == 0)
            return;

        // BMP format, this means we can skip a bit of data.
        if(length >= 6 && rawData.readString(0, 2).equals("BM"))
            index = rawData.readLittleEndianInt(2);

        // One tile, including coordinates in raw form is 4 bytes.
        for(; index + 4 < length; index += 4) {
            TileData td = makeTileData(rawData.readLittleEndianInt(index));

            if(td.getType() < vieAsteroidEnd) {
                mapData.addByte(td.getType(), getLinear(td.getX(), td.getY()));
            } else if (td.getType() == vieAsteroidEnd) {
                mapData.addByte(td.getType(), getLinear(td.getX()    , td.getY()    ));
                mapData.addByte(td.getType(), getLinear(td.getX()    , td.getY() + 1));
                mapData.addByte(td.getType(), getLinear(td.getX() + 1, td.getY()    ));
                mapData.addByte(td.getType(), getLinear(td.getX() + 1, td.getY() + 1));
            } else if (td.getType() == vieWormhole) {
                for (int x = 0; x < 5 && td.getX() + x < TILE_MAX_X; x++)
                    for (int y = 0; y < 5 && td.getY() + x < TILE_MAX_Y; y++)
                        mapData.addByte(td.getType(), getLinear(td.getX() + x, td.getY() + y));
            } else if (td.getType() == vieStation) {
                for (int x = 0; x < 6 && td.getX() + x < TILE_MAX_X; x++)
                    for (int y = 0; y < 6 && td.getY() + x < TILE_MAX_Y; y++)
                        mapData.addByte(td.getType(), getLinear(td.getX() + x, td.getY() + y));
            } else {
                mapData.addByte(td.getType(), getLinear(td.getX(), td.getY()));
            }

        }

        // If no exception has been thrown, mark the conversion as a success.
        gotMap = true;

    }

    /**
        Counts all the tiles that have an original VIE type tile.
        @return Total tiles counted.
    */
    public int getMapSize() {
        int offset = 0;

        for (int x = 1; x < 1023; ++x)       // Ignore border
            for (int y = 1; y < 1023; ++y) {
                char type = (char) mapData.readByte(getLinear(x, y));

                if (type == 0)
                    continue;

                if ( ((type >= vieNormalStart) && (type <= vieFlyUnderEnd))             // Regular tiles
                        || ((type >= vieAsteroidStart) && (type <= vieWormhole)) ) {    // Animated tiles
                    // The tile is not internal
                    ++offset;
                }
            }

        return (offset << 2);
    }

    /**
        Loads a level file into the memory.
        @param filename Filename including path and extension.
        @return Raw file data, or null on any error.
    */
    public ByteArray loadMap(String filename) {
        try {
            File file = new File(filename);
            int length = (int) file.length();
            byte[] byteBuffer = new byte[length];
            FileInputStream fis = new FileInputStream(file);
            fis.read(byteBuffer);
            fis.close();

            return new ByteArray(byteBuffer);
        } catch (FileNotFoundException e) {
            // File not found.
        } catch (NullPointerException e) {
            // Invalid file name.
        } catch (IOException e) {
            // Error when reading file.
        } catch (SecurityException e) {
            // Insufficient rights to read file.
        }

        return null;
    }

    /**
        Uncompresses a Zlib compressed map file and stores the raw map data to disk.
        @param fileName Filename including path and extension.
        @param cmpData Compressed data
        @param length Length of data
        @return True if the uncompress was a success, false otherwise.
    */
    public static boolean saveMap(String fileName, ByteArray cmpData, int length) {
        Inflater inflater = new Inflater();
        inflater.setInput(cmpData.getByteArray());
        List<Byte> decompDataList = new ArrayList<Byte>();

        // Uncompressing
        try {
            while(!inflater.needsInput()) {
                byte[] decompBuffer = new byte[length];
                int count = inflater.inflate(decompBuffer);

                for(int i = 0; i < count; i++)
                    decompDataList.add(decompBuffer[i]);
            }

        } catch (DataFormatException dfe) {
            inflater.end();
            return false;
        }

        inflater.end();

        // Convert the uncompressed data into a byte array.
        int size = decompDataList.size();
        ByteArray decompData = new ByteArray(size);

        for(int i = 0; i < size; i++)
            decompData.addByte(decompDataList.get(i));

        // Stores the raw data to disk.
        try {
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decompData.getByteArray());
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            // File not found.
        } catch (NullPointerException e) {
            // Invalid file name.
        } catch (IOException e) {
            // Error when reading file.
        } catch (SecurityException e) {
            // Insufficient rights to read file.
        }

        return false;
    }

    /*
        Getters
    */
    public ByteArray getMap() {
        return mapData;
    }

    public boolean haveMap() {
        return gotMap;
    }

    /**
        Gets the exact tile type that is located at the given coordinate.
        @param coordX X-coordinate.
        @param coordY Y-coordinate.
        @return A vie or ssb constant.
    */
    public int getTileInfo(int coordX, int coordY) {
        return mapData.readByte(getLinear(coordX, coordY));
    }

    /**
        Checks whether a given coordinate has a solid tile. Possible return types are:
        <ul>
        <li>SOLID - Unpassable tile;
        <li>PERMABLE - Passable tile;
        <li>DOOR - Can be passable as well as unpassable;
        <li>WORMHOLE - Passable in a way, but teleports the object;
        <li>BRICK - Can be passable.
        </ul>
        @param coordX X-coordinate in tiles.
        @param coordY Y-coordinate in tiles.
        @return One of the constants listed above, PERMEABLE in the case of an error.
    */
    public byte isSolid(int coordX, int coordY) {
        int tile = getTileInfo(coordX, coordY);

        if((tile >= vieNormalStart && tile <= vieNormalEnd)
                || (tile >= vieAsteroidStart && tile <= vieStation)) {
            return SOLID;
        } else if(tile == vieNoTile
                  || (tile >= vieFlyOverStart && tile <= vieFlyUnderEnd)
                  || (tile >= ssbTeamGoal && tile <= ssbPrize)) {
            return PERMEABLE;
        } else if(tile >= vieVDoorStart && tile <= vieHDoorEnd) {
            return DOOR;
        } else if(tile == vieWormhole) {
            return WORMHOLE;
        } else if(tile == ssbTeamBrick || tile == ssbEnemyBrick) {
            return BRICK;
        } else {
            return PERMEABLE;
        }
    }

    /**
        Checks whether the given tile is a door.
        @param coordX X-coordinate in tiles.
        @param coordY Y-coordinate in tiles.
        @return True when the tile is a door.
    */
    public boolean isDoor(int coordX, int coordY) {
        int tile = getTileInfo(coordX, coordY);
        return (tile >= vieVDoorStart && tile <= vieHDoorEnd);
    }

    /**
        Checks what kind of door the tile is.
        <ul>
        <li> 0: Vertical door type 1;
        <li> 1: Vertical door type 2;
        <li> 2: Vertical door type 3;
        <li> 3: Vertical door type 4;
        <li> 4: Horizontal door type 1;
        <li> 5: Horizontal door type 2;
        <li> 6: Horizontal door type 3;
        <li> 7: Horizontal door type 4;
        <li>-1: Not a door.
        </ul>
        @param coordX X-coordinate in tiles.
        @param coordY Y-coordinate in tiles.
        @return One of the values in the list mentioned above.
    */
    public int getDoorType(int coordX, int coordY) {
        int tile = getTileInfo(coordX, coordY);

        if(tile >= vieVDoorStart && tile <= vieHDoorEnd) {
            return tile - vieVDoorStart;
        } else {
            return -1;
        }
    }

    /**
        This class holds the information of a single parsed tile.
        @author Trancid

    */
    public class TileData {
        private Point location;     // Location of the tile, in tile coordinates.
        private char type;          // Type of tile. See VIE constants.

        /**
            Empty TileData constructor. Creates a tile with default parameters.
        */
        public TileData() {
            this(0, 0, vieNoTile);
        }

        /**
            TileData constructor. Tile type will be initialized as {@link LvlMap#vieNoTile}.
            @param x X-coordinate in tiles.
            @param y Y-coordinate in tiles.
        */
        public TileData(int x, int y) {
            this(x, y, vieNoTile);
        }

        /**
            TileData constructor.
            @param x X-coordinate in tiles.
            @param y Y-coordinate in tiles.
            @param type Tile type. See VIE constants.
        */
        public TileData(int x, int y, char type) {
            this.location = new Point(x, y);
            this.type = type;
        }

        /*
            Getters
        */
        public int getX() {
            return location.x;
        }

        public int getY() {
            return location.y;
        }

        public Point getLocation() {
            return location;
        }

        public char getType() {
            return type;
        }

        public void setType(char type) {
            this.type = type;
        }
    }
}