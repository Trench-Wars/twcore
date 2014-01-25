package twcore.core.game;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
 * ArenaSettings is used to track the current arena settings. Currently its main use is for generating checksums,
 * but there might be other applications for this in the future. The only current way of updating the settings is
 * through a packet that the bot receives from the server.
 * <p>
 * The main structure has been copied over from the MervBot and has been ported to Java. Due to Java's limited
 * possibilities in regard to primitive data types, some fields may appear to be off. At some point they will need
 * to be verified and adjusted where needed. Most, if not all of this data can be found in the arena's .cfg files.
 * <p>
 * Due to this being a port, I've tried to give credit where credit is due, by trying to keep most of the original
 * comments in tact. 
 * 
 * @author Trancid
 *
 */
public class ArenaSettings {
    
    // The following constants are used to make sure that values that need to be unsigned, are unsigned.
    // To conserve memory, this conversion is done at runtime. Otherwise, the memory usage would be doubled.
    private static final short  MASK_UINT8  = 0xff;         // Conversion mask UINT8  -> short (INT16)
    private static final int    MASK_UINT16 = 0xffff;       // Conversion mask UINT16 -> int   (INT32)
    private static final long   MASK_UINT32 = 0xffffffff;   // Conversion mask UINT32 -> long  (INT64)

    private ByteArray rawData;                          // Contains the packet data in raw form. Needed for generating checksums.
    
    /*
     * All the raw data broken down into its components.
     */
    // 1428 bytes wide                                  //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    // Initial template by Snrrrub                      // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private byte            version;                    // 0000     | VIE       | Version                       |    15 |    15 |         8 |
    private boolean         exactDamage;                // 0001     | Bullet    | ExactDamage                   |     0 |     1 |         1 | If damage is to be random or not (1=exact, 0=random) [Continuum .36]
    private boolean         hideFlags;                  // 0001     | Spectator | HideFlags                     |     0 |     1 |         1 | If flags are to be shown to specs when they are dropped (1=can't see them) [Continuum .36]
    private boolean         noXRadar;                   // 0001     | Spectator | NoXRadar                      |     0 |     1 |         1 | If specs are allowed to have X (0=yes, 1=no) [Continuum .36]
    private byte[]          pack0 = new byte[3];        // 0001     | ?         | ?                             |       |       |        21 | (14)
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private ShipSettings[]  shipSettings;               // 0004     | Ship      | Shipsettings                  |       |       |  8 * 1152 | See shipSettings declaration...
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private int             bulletDamageLevel;          // 1156     | Bullet    | BulletDamageLevel             |       |       |        32 | Maximum amount of damage that a L1 bullet will cause. Formula; damage = squareroot(rand# * (max damage^2 + 1)) [BulletDamageLevel * 1000]
    private int             bombDamageLevel;            // 1160     | Bomb      | BombDamageLevel               |       |       |        32 | Amount of damage a bomb causes at its center point (for all bomb levels) [BombDamageLevel * 1000]
    private int             bulletAliveTime;            // 1164     | Bullet    | BulletAliveTime               |       |       |        32 | How long bullets live before disappearing (in hundredths of a second)
    private int             bombAliveTime;              // 1168     | Bomb      | BombAliveTime                 |       |       |        32 | Time bomb is alive (in hundredths of a second)
    private int             decoyAliveTime;             // 1172     | Misc      | DecoyAliveTime                |       |       |        32 | Time a decoy is alive (in hundredths of a second)
    private int             safetyLimit;                // 1176     | Misc      | SafetyLimit                   |       |       |        32 | Amount of time that can be spent in the safe zone. (90000 = 15 mins)
    private int             frequencyShift;             // 1180     | Misc      | FrequencyShift                |     0 | 10000 |        32 | Amount of random frequency shift applied to sounds in the game.
    private int             maxFrequency;               // 1184     | Team      | MaxFrequency                  |       |       |        32 | Maximum number of frequencies allowed in arena (5 would allow frequencies 0,1,2,3,4)
    private int             repelSpeed;                 // 1188     | Repel     | RepelSpeed                    |       |       |        32 | Speed at which players are repelled
    private int             mineAliveTime;              // 1192     | Mine      | MineAliveTime                 |     0 | 60000 |        32 | Time that mines are active (in hundredths of a second)
    private int             burstDamageLevel;           // 1196     | Burst     | BurstDamageLevel              |       |       |        32 | Maximum amount of damage caused by a single burst bullet. [BurstDamageLevel * 1000]
    private int             bulletDamageUpgrade;        // 1200     | Bullet    | BulletDamageUpgrade           |       |       |        32 | Amount of extra damage each bullet level will cause [BulletDamageUpgrade * 1000]
    private int             flagDropDelay;              // 1204     | Flag      | FlagDropDelay                 |       |       |        32 | Time before flag is dropped by carrier (0=never)
    private int             enterGameFlaggingDelay;     // 1208     | Flag      | EnterGameFlaggingDelay        |       |       |        32 | Time a new player must wait before they are allowed to see flags
    private int             rocketThrust;               // 1212     | Rocket    | RocketThrust                  |       |       |        32 | Thrust value given while a rocket is active.
    private int             rocketSpeed;                // 1216     | Rocket    | RocketSpeed                   |       |       |        32 | Speed value given while a rocket is active.
    private int             inactiveShrapnelDamage;     // 1220     | Shrapnel  | InactiveShrapDamage           |       |       |        32 | Amount of damage shrapnel causes in it's first 1/4 second of life. [InactiveShrapnelDamage * 1000]
    private int             wormholeSwitchTime;         // 1224     | Wormhole  | SwitchTime                    |       |       |        32 | How often the wormhole switches its destination.
    private int             activateAppShutdownTime;    // 1228     | Misc      | ActivateAppShutdownTime       |       |       |        32 | Amount of time a ship is shutdown after application is reactivated (ie. when they come back from windows mode)
    private int             shrapnelSpeed;              // 1232     | Shrapnel  | ShrapnelSpeed                 |       |       |        32 | Speed that shrapnel travels
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private byte[]          UNKNOWN0 = new byte[16];    // 1236     | ?         | ?                             |       |       |       128 | ?
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private short           sendRoutePercent;           // 1252     | Latency   | SendRoutePercent              |   300 |   800 |        16 | Percentage of the ping time that is spent on the ClientToServer portion of the ping. (used in more accurately syncronizing clocks)
    private short           bombExplodeDelay;           // 1254     | Bomb      | BombExplodeDelay              |       |       |        16 | How long after the proximity sensor is triggered before bomb explodes. (note: it explodes immediately if ship moves away from it after triggering it)
    private short           sendPositionDelay;          // 1256     | Misc      | SendPositionDelay             |     0 |    20 |        16 | Amount of time between position packets sent by client.
    private short           bombExplodePixels;          // 1258     | Bomb      | BombExplodePixels             |       |       |        16 | Blast radius in pixels for an L1 bomb (L2 bombs double this, L3 bombs triple this)
    private short           deathPrizeTime;             // 1260     | Prize     | DeathPrizeTime                |       |       |        16 | How long the prize exists that appears after killing somebody.
    private short           jitterTime;                 // 1262     | Bomb      | JitterTime                    |       |       |        16 | How long the screen jitters from a bomb hit. (in hundredths of a second)
    private short           enterDelay;                 // 1264     | Kill      | EnterDelay                    |       |       |        16 | How long after a player dies before he can re-enter the game.
    private short           engineShutdownTime;         // 1266     | Prize     | EngineShutdownTime            |       |       |        16 | Time the player is affected by an 'Engine Shutdown' Prize (in hundredth of a second)
    private short           proximityDistance;          // 1268     | Bomb      | ProximityDistance             |       |       |        16 | Radius of proximity trigger in tiles.  Each bomb level adds 1 to this amount.
    private short           bountyIncreaseForKill;      // 1270     | Kill      | BountyIncreaseForKill         |       |       |        16 | Number of points added to players bounty each time he kills an opponent.
    private short           bounceFactor;               // 1272     | Misc      | BounceFactor                  |       |       |        16 | How bouncy the walls are (16=no-speed-loss)
    private short           mapZoomFactor;              // 1274     | Radar     | MapZoomFactor                 |     8 |  1000 |        16 | A number representing how far you can see on radar.
    private short           maxBonus;                   // 1276     | Kill      | MaxBonus                      |       |       |        16 | Let's ignore these for now. Or let's not. :) This is if you have flags, can add more points per a kill. Founded by MGB
    private short           maxPenalty;                 // 1278     | Kill      | MaxPenalty                    |       |       |        16 | Let's ignore these for now. Or let's not. :) This is if you have flags, can take away points per a kill. Founded by MGB
    private short           rewardBase;                 // 1280     | Kill      | RewardBase                    |       |       |        16 | Let's ignore these for now. Or let's not. :) This is shown added to a person's bty, but isn't added from points for a kill. Founded by MGB
    private short           repelTime;                  // 1282     | Repel     | RepelTime                     |       |       |        16 | Time players are affected by the repel (in hundredths of a second)
    private short           repelDistance;              // 1284     | Repel     | RepelDistance                 |       |       |        16 | Number of pixels from the player that are affected by a repel.
    private short           helpTickerDelay;            // 1286     | Misc      | TickerDelay                   |       |       |        16 | Amount of time between ticker help messages.
    private boolean         flaggerOnRadar;             // 1288     | Flag      | FlaggerOnRadar                |       |       |        16 | Whether the flaggers appear on radar in red 0=no 1=yes
    private short           flaggerKillMultiplier;      // 1290     | Flag      | FlaggerKillMultiplier         |       |       |        16 | Number of times more points are given to a flagger (1 = double points, 2 = triple points)
    private short           prizeFactor;                // 1292     | Prize     | PrizeFactor                   |       |       |        16 | Number of prizes hidden is based on number of players in game.  This number adjusts the formula, higher numbers mean more prizes. (*Note: 10000 is max, 10 greens per person)
    private short           prizeDelay;                 // 1294     | Prize     | PrizeDelay                    |       |       |        16 | How often prizes are regenerated (in hundredths of a second)
    private short           prizeMinimumVirtual;        // 1296     | Prize     | MinimumVirtual                |       |       |        16 | Distance from center of arena that prizes/flags/soccer-balls will generate
    private short           prizeUpgradeVirtual;        // 1298     | Prize     | UpgradeVirtual                |       |       |        16 | Amount of additional distance added to MinimumVirtual for each player that is in the game.
    private short           prizeMaxExist;              // 1300     | Prize     | PrizeMaxExist                 |       |       |        16 | Maximum amount of time that a hidden prize will remain on screen. (actual time is random)
    private short           prizeMinExist;              // 1302     | Prize     | PrizeMinExist                 |       |       |        16 | Minimum amount of time that a hidden prize will remain on screen. (actual time is random)
    private short           prizeNegativeFactor;        // 1304     | Prize     | PrizeNegativeFactor           |       |       |        16 | Odds of getting a negative prize.  (1 = every prize, 32000 = extremely rare)
    private short           doorDelay;                  // 1306     | Door      | DoorDelay                     |       |       |        16 | How often doors attempt to switch their state.
    private short           antiwarpPixels;             // 1308     | Toggle    | AntiWarpPixels                |       |       |        16 | Distance Anti-Warp affects other players (in pixels) (note: enemy must also be on radar)
    private short           doorMode;                   // 1310     | Door      | DoorMode                      |       |       |        16 | Door mode (-2=all doors completely random, -1=weighted random (some doors open more often than others), 0-255=fixed doors (1 bit of byte for each door specifying whether it is open or not)
    private short           flagBlankDelay;             // 1312     | Flag      | FlagBlankDelay                |       |       |        16 | Amount of time that a user can get no data from server before flags are hidden from view for 10 seconds.
    private short           noDataFlagDropDelay;        // 1314     | Flag      | NoDataFlagDropDelay           |       |       |        16 | Amount of time that a user can get no data from server before flags he is carrying are dropped.
    private short           multiPrizeCount;            // 1316     | Prize     | MultiPrizeCount               |       |       |        16 | Number of random 'Greens' given with a 'MultiPrize'
    private short           brickTime;                  // 1318     | Brick     | BrickTime                     |       |       |        16 | How long bricks last (in hundredths of a second)
    private short           warpRadiusLimit;            // 1320     | Misc      | WarpRadiusLimit               |       |       |        16 | When ships are randomly placed in the arena, this parameter will limit how far from the center of the arena they can be placed (1024=anywhere)
    private short           eBombShutdownTime;          // 1322     | Bomb      | EBombShutdownTime             |       |       |        16 | Maximum time recharge is stopped on players hit with an EMP bomb.
    private short           eBombDamagePercent;         // 1324     | Bomb      | EBombDamagePercent            |       |       |        16 | Percentage of normal damage applied to an EMP bomb 0=0% 1000=100% 2000=200%
    private short           radarNeutralSize;           // 1326     | Radar     | RadarNeutralSize              |     0 |  1024 |        16 | Size of area between blinded radar zones (in pixels)
    private short           warpPointDelay;             // 1328     | Misc      | WarpPointDelay                |       |       |        16 | How long a Portal point is active.
    private short           nearDeathLevel;             // 1330     | Misc      | NearDeathLevel                |       |       |        16 | Amount of energy that constitutes a near-death experience (ships bounty will be decreased by 1 when this occurs -- used for dueling zone)
    private short           bBombDamagePercent;         // 1332     | Bomb      | BBombDamagePercent            |       |       |        16 | Percentage of normal damage applied to a bouncing bomb 0=0% 1000=100% 2000=200%
    private short           shrapnelDamagePercent;      // 1334     | Shrapnel  | ShrapnelDamagePercent         |       |       |        16 | Percentage of normal damage applied to shrapnel (relative to bullets of same level) 0=0% 1000=100% 2000=200%
    private short           clientSlowPacketTime;       // 1336     | Latency   | ClientSlowPacketTime          |    20 |   200 |        16 | Amount of latency S2C that constitutes a slow packet.
    private short           flagDropResetReward;        // 1338     | Flag      | FlagDropResetReward           |       |       |        16 | Minimum kill reward that a player must get in order to have his flag drop timer reset.
    private short           flaggerFireCostPercent;     // 1340     | Flag      | FlaggerFireCostPercent        |       |       |        16 | Percentage of normal weapon firing cost for flaggers 0=Super 1000=100% 2000=200%
    private short           flaggerDamagePercent;       // 1342     | Flag      | FlaggerDamagePercent          |       |       |        16 | Percentage of normal damage received by flaggers 0=Invincible 1000=100% 2000=200%
    private short           flaggerBombFireDelay;       // 1344     | Flag      | FlaggerBombFireDelay          |       |       |        16 | Delay given to flaggers for firing bombs (0=ships normal firing rate -- note: please do not set this number less than 20)
    private short           soccerPassDelay;            // 1346     | Soccer    | PassDelay                     |     0 | 10000 |        16 | How long after the ball is fired before anybody can pick it up (in hundredths of a second)
    private short           soccerBallBlankDelay;       // 1348     | Soccer    | BallBlankDelay                |       |       |        16 | Amount of time a player can receive no data from server and still pick up the soccer ball.
    private short           s2CNoDataKickoutDelay;      // 1350     | Latency   | S2CNoDataKickoutDelay         |   100 | 32000 |        16 | Amount of time a user can receive no data from server before connection is terminated.
    private short           flaggerThrustAdjustment;    // 1352     | Flag      | FlaggerThrustAdjustment       |       |       |        16 | Amount of thrust adjustment player carrying flag gets (negative numbers mean less thrust)
    private short           flaggerSpeedAdjustment;     // 1354     | Flag      | FlaggerSpeedAdjustment        |       |       |        16 | Amount of speed adjustment player carrying flag gets (negative numbers mean slower)
    private short           cliSlowPacketSampleSize;    // 1356     | Latency   | ClientSlowPacketSampleSize    |    50 |  1000 |        16 | Number of packets to sample S2C before checking for kickout.
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private byte[]          UNKNOWN1 = new byte[10];    // 1358     | ?         | ?                             |       |       |        80 | ?
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private boolean         randomShrapnel;             // 1368     | Shrapnel  | Random                        |     0 |     1 |         1 | Whether shrapnel spreads in circular or random patterns 0=circular 1=random
    private boolean         soccerBallBounce;           // 1369     | Soccer    | BallBounce                    |     0 |     1 |         1 | Whether the ball bounces off walls (0=ball go through walls, 1=ball bounces off walls)
    private boolean         soccerAllowBombs;           // 1370     | Soccer    | AllowBombs                    |     0 |     1 |         1 | Whether the ball carrier can fire his bombs (0=no 1=yes)
    private boolean         soccerAllowGuns;            // 1371     | Soccer    | AllowGuns                     |     0 |     1 |         1 | Whether the ball carrier can fire his guns (0=no 1=yes)
    private byte            soccerMode;                 // 1372     | Soccer    | Mode                          |     0 |     6 |         8 | Goal configuration (0=any goal, 1=left-half/right-half, 2=top-half/bottom-half, 3=quadrants-defend-one-goal, 4=quadrants-defend-three-goals, 5=sides-defend-one-goal, 6=sides-defend-three-goals)
    private byte            maxPerTeam;                 // 1373     | Team      | MaxPerTeam                    |       |       |         8 | Maximum number of players on a non-private frequency
    private byte            maxPerPrivateTeam;          // 1374     | Team      | MaxPerPrivateTeam             |       |       |         8 | Maximum number of players on a private frequency (0=same as MaxPerTeam)
    private short           teamMaxMines;               // 1375     | Mine      | TeamMaxMines                  |     0 | 32000 |       15? | Maximum number of mines allowed to be placed by an entire team
    private boolean         wormholeGravityBombs;       // 1376     | Wormhole  | GravityBombs                  |     0 |     1 |         1 | Whether a wormhole affects bombs (0=no 1=yes)
    private boolean         bombSafety;                 // 1377     | Bomb      | BombSafety                    |     0 |     1 |         1 | Whether proximity bombs have a firing safety (0=no 1=yes).  If enemy ship is within proximity radius, will it allow you to fire.
    private boolean         messageReliable;            // 1378     | Message   | MessageReliable               |     0 |     1 |         1 | :Whether messages are sent reliably.
    private boolean         takePrizeReliable;          // 1379     | Prize     | TakePrizeReliable             |     0 |     1 |         1 | Whether prize packets are sent reliably (C2S)
    private boolean         allowAudioMessages;         // 1380     | Message   | AllowAudioMessages            |     0 |     1 |         1 | Whether players can send audio messages (0=no 1=yes)
    private byte            prizeHideCount;             // 1381     | Prize     | PrizeHideCount                |       |       |         8 | Number of prizes that are regenerated every PrizeDelay.
    private boolean         extraPositionData;          // 1382     | Misc      | ExtraPositionData             |     0 |     1 |         1 | Whether regular players receive sysop data about a ship (leave this at zero)
    private boolean         slowFrameCheck;             // 1383     | Misc      | SlowFrameCheck                |     0 |     1 |         1 | Whether to check for slow frames on the client (possible cheat technique) (flawed on some machines, do not use)
    private byte            carryFlags;                 // 1384     | Flag      | CarryFlags                    |     0 |     2 |         2 | Whether the flags can be picked up and carried (0=no, 1=yes, 2=yes-one at a time)
    private boolean         allowSavedShip;             // 1385     | Misc      | AllowSavedShips               |     0 |     1 |         1 | Whether saved ships are allowed (do not allow saved ship in zones where sub-arenas may have differing parameters) 1 = Savedfrom last arena/lagout, 0 = New Ship when entering arena/zone
    private byte            radarMode;                  // 1386     | Radar     | RadarMode                     |     0 |     4 |         3 | Radar mode (0=normal, 1=half/half, 2=quarters, 3=half/half-see team mates, 4=quarters-see team mates)
    private boolean         victoryMusic;               // 1387     | Misc      | VictoryMusic                  |     0 |     1 |         1 | Whether the zone plays victory music or not.
    private boolean         flaggerGunUpgrade;          // 1388     | Flag      | FlaggerGunUpgrade             |     0 |     1 |         1 | Whether the flaggers get a gun upgrade 0=no 1=yes
    private boolean         flaggerBombUpgrade;         // 1389     | Flag      | FlaggerBombUpgrade            |     0 |     1 |         1 | Whether the flaggers get a bomb upgrade 0=no 1=yes
    private boolean         soccerUseFlagger;           // 1390     | Soccer    | UseFlagger                    |     0 |     1 |         1 | If player with soccer ball should use the Flag:Flagger* ship adjustments or not (0=no, 1=yes)
    private boolean         soccerBallLocation;         // 1391     | Soccer    | BallLocation                  |     0 |     1 |         1 | Whether the balls location is displayed at all times or not (0=not, 1=yes)
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private byte[]          UNKNOWN2 = new byte[8];     // 1392     | ?         | ?                             |       |       |        64 | ?
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                        // Offset   | Category  | Name                          | Min.  | Max.  | Bitsize   | Description
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    private PrizeSettings   prizeSettings;              // 1400     | Prize     | PrizeSettings                 |       |       |       224 | See PrizeSettings declaration...
                                                        //----------+-----------+-------------------------------+-------+-------+-----------+---------------------------------------------------------------
    
    /** ArenaSettings constructor */
    public ArenaSettings(ByteArray rawData) {
        this.rawData = rawData;
        
        this.shipSettings = new ShipSettings[8]; 
        
        updateVariables(rawData);
    }
    
    /**
     * Updates the current settings to the new settings.
     * When nothing has been changed, no changes will be made, and this function will return immediately.
     * @param newSettings New settings as the raw packet data.
     */
    public void updateSettings(ByteArray newSettings) {
        if(!this.rawData.equals(newSettings)) {
            this.rawData = newSettings;
            updateVariables(newSettings);
        }
    }
    
    /**
     * Fetches the arena settings raw data.
     * @return The pure raw data.
     */
    public ByteArray getRawSettings() {
        return rawData;
    }
    
    /**
     * The actual updating of the fields of this class.
     * <p>
     * For the weird data read out and formats, please refer to the comments near the declarations of each variable.
     * @param data Raw data that contains the information.
     */
    public void updateVariables(ByteArray data) {
        
        this.version                    = data.readByte(0);
        this.exactDamage                = (ByteArray.getPartial(data.readByte(1), 0, 0) == 1);
        this.hideFlags                  = (ByteArray.getPartial(data.readByte(1), 1, 1) == 1);
        this.noXRadar                   = (ByteArray.getPartial(data.readByte(1), 2, 2) == 1);
        this.pack0[0]                   = (byte) ByteArray.getPartial(data.readByte(1), 3, 7);
        this.pack0[1]                   = data.readByte(2);
        this.pack0[2]                   = data.readByte(3);
        
        for(int i = 0; i < 8; i++) 
            this.shipSettings[i]        = new ShipSettings(data.readByteArray(4 + i * 144, 4 + i * 144 + 143));
        
        this.bulletDamageLevel          = data.readLittleEndianInt(1156);
        this.bombDamageLevel            = data.readLittleEndianInt(1160);
        this.bulletAliveTime            = data.readLittleEndianInt(1164);
        this.bombAliveTime              = data.readLittleEndianInt(1168);
        this.decoyAliveTime             = data.readLittleEndianInt(1172);
        this.safetyLimit                = data.readLittleEndianInt(1176);
        this.frequencyShift             = data.readLittleEndianInt(1180);
        this.maxFrequency               = data.readLittleEndianInt(1184);
        this.repelSpeed                 = data.readLittleEndianInt(1188);
        this.mineAliveTime              = data.readLittleEndianInt(1192);
        this.burstDamageLevel           = data.readLittleEndianInt(1196);
        this.bulletDamageUpgrade        = data.readLittleEndianInt(1200);
        this.flagDropDelay              = data.readLittleEndianInt(1204);
        this.enterGameFlaggingDelay     = data.readLittleEndianInt(1208);
        this.rocketThrust               = data.readLittleEndianInt(1212);
        this.rocketSpeed                = data.readLittleEndianInt(1216);
        this.inactiveShrapnelDamage     = data.readLittleEndianInt(1220);
        this.wormholeSwitchTime         = data.readLittleEndianInt(1224);
        this.activateAppShutdownTime    = data.readLittleEndianInt(1228);
        this.shrapnelSpeed              = data.readLittleEndianInt(1232);
        
        for(int i = 0; i < 16; i++)
            this.UNKNOWN0[i]            = data.readByte(1236 + i);
        
        this.sendRoutePercent           = data.readLittleEndianShort(1252);
        this.bombExplodeDelay           = data.readLittleEndianShort(1254);
        this.sendPositionDelay          = data.readLittleEndianShort(1256);
        this.bombExplodePixels          = data.readLittleEndianShort(1258);
        this.deathPrizeTime             = data.readLittleEndianShort(1260);
        this.jitterTime                 = data.readLittleEndianShort(1262);
        this.enterDelay                 = data.readLittleEndianShort(1264);
        this.engineShutdownTime         = data.readLittleEndianShort(1266);
        this.proximityDistance          = data.readLittleEndianShort(1268);
        this.bountyIncreaseForKill      = data.readLittleEndianShort(1270);
        this.bounceFactor               = data.readLittleEndianShort(1272);
        this.mapZoomFactor              = data.readLittleEndianShort(1274);
        this.maxBonus                   = data.readLittleEndianShort(1276);
        this.maxPenalty                 = data.readLittleEndianShort(1278);
        this.rewardBase                 = data.readLittleEndianShort(1280);
        this.repelTime                  = data.readLittleEndianShort(1282);
        this.repelDistance              = data.readLittleEndianShort(1284);
        this.helpTickerDelay            = data.readLittleEndianShort(1286);
        this.flaggerOnRadar             = (data.readLittleEndianShort(1288) != 0);
        this.flaggerKillMultiplier      = data.readLittleEndianShort(1290);
        this.prizeFactor                = data.readLittleEndianShort(1292);
        this.prizeDelay                 = data.readLittleEndianShort(1294);
        this.prizeMinimumVirtual        = data.readLittleEndianShort(1296);
        this.prizeUpgradeVirtual        = data.readLittleEndianShort(1298);
        this.prizeMaxExist              = data.readLittleEndianShort(1300);
        this.prizeMinExist              = data.readLittleEndianShort(1302);
        this.prizeNegativeFactor        = data.readLittleEndianShort(1304);
        this.doorDelay                  = data.readLittleEndianShort(1306);
        this.antiwarpPixels             = data.readLittleEndianShort(1308);
        this.doorMode                   = data.readLittleEndianShort(1310);
        this.flagBlankDelay             = data.readLittleEndianShort(1312);
        this.noDataFlagDropDelay        = data.readLittleEndianShort(1314);
        this.multiPrizeCount            = data.readLittleEndianShort(1316);
        this.brickTime                  = data.readLittleEndianShort(1318);
        this.warpRadiusLimit            = data.readLittleEndianShort(1320);
        this.eBombShutdownTime          = data.readLittleEndianShort(1322);
        this.eBombDamagePercent         = data.readLittleEndianShort(1324);
        this.radarNeutralSize           = data.readLittleEndianShort(1326);
        this.warpPointDelay             = data.readLittleEndianShort(1328);
        this.nearDeathLevel             = data.readLittleEndianShort(1330);
        this.bBombDamagePercent         = data.readLittleEndianShort(1332);
        this.shrapnelDamagePercent      = data.readLittleEndianShort(1334);
        this.clientSlowPacketTime       = data.readLittleEndianShort(1336);
        this.flagDropResetReward        = data.readLittleEndianShort(1338);
        this.flaggerFireCostPercent     = data.readLittleEndianShort(1340);
        this.flaggerDamagePercent       = data.readLittleEndianShort(1342);
        this.flaggerBombFireDelay       = data.readLittleEndianShort(1344);
        this.soccerPassDelay            = data.readLittleEndianShort(1346);
        this.soccerBallBlankDelay       = data.readLittleEndianShort(1348);
        this.s2CNoDataKickoutDelay      = data.readLittleEndianShort(1350);
        this.flaggerThrustAdjustment    = data.readLittleEndianShort(1352);
        this.flaggerSpeedAdjustment     = data.readLittleEndianShort(1354);
        this.cliSlowPacketSampleSize    = data.readLittleEndianShort(1356);
        
        for(int i = 0; i < 10; i++)
            this.UNKNOWN1[i]            = data.readByte(1358 + i);
        
        this.randomShrapnel             = (data.readByte(1368) == 1);
        this.soccerBallBounce           = (data.readByte(1369) == 1);
        this.soccerAllowBombs           = (data.readByte(1370) == 1);
        this.soccerAllowGuns            = (data.readByte(1371) == 1);
        this.soccerMode                 = data.readByte(1372);
        this.maxPerTeam                 = data.readByte(1373);
        this.maxPerPrivateTeam          = data.readByte(1374);
        short tmpShort = data.readLittleEndianShort(1375);
        this.teamMaxMines               = (short) ByteArray.getPartial(tmpShort, 14, 0);
        this.wormholeGravityBombs       = (ByteArray.getPartial(tmpShort, 15, 15) == 1);
        this.bombSafety                 = (data.readByte(1377) == 1);
        this.messageReliable            = (data.readByte(1378) == 1);
        this.takePrizeReliable          = (data.readByte(1379) == 1);
        this.allowAudioMessages         = (data.readByte(1380) == 1);
        this.prizeHideCount             = data.readByte(1381);
        this.extraPositionData          = (data.readByte(1382) == 1);
        this.slowFrameCheck             = (data.readByte(1383) == 1);
        this.carryFlags                 = data.readByte(1384);
        this.allowSavedShip             = (data.readByte(1385) == 1);
        this.radarMode                  = data.readByte(1386);
        this.victoryMusic               = (data.readByte(1387) == 1);
        this.flaggerGunUpgrade          = (data.readByte(1388) == 1);
        this.flaggerBombUpgrade         = (data.readByte(1389) == 1);
        this.soccerUseFlagger           = (data.readByte(1390) == 1);
        this.soccerBallLocation         = (data.readByte(1391) == 1);
        
        for(int i = 0; i < 8; i++)
            this.UNKNOWN2[i]            = data.readByte(1392 + i);
     
        this.prizeSettings              = new PrizeSettings(data.readByteArray(1400, 1427));
    }
    
    /*
     * Automatically generated getters.
     */
    /**
     * Returns the raw data.
     * @return rawData
     */
    public ByteArray getRawData() {
        return rawData;
    }

    /**
     * [Unknown] Unknown
     * Version number of arena settings? Seems to be 15 by default.
     * @return Version number.
     */
    public short getVersion() {
        return (short) (version & MASK_UINT8);
    }

    /**
     * <b>[Bullet] ExactDamage</b><br>
     * Whether bullet damage should be exact, instead of the default random.
     * @return True when the damage is exact, false when it is random.
     */
    public boolean isExactDamage() {
        return exactDamage;
    }

    /**
     * <b>[Spectator] HideFlags</b><br>
     * Whether server should hide dropped flags from spectators. 
     * This will help keep teams from using spectators to locate enemy bases/flags.
     * @return True when the dropped flags are hidden, false otherwise.
     */
    public boolean isHideFlags() {
        return hideFlags;
    }

    /**
     * <b>[Spectator] NoXRadar</b><br>
     * Whether server should prevent spectators from having X-Radar. 
     * Note that a value of yes will cause spectators not to have X-Radar.
     * @return True when spectators do not have X-radar, false if they do have it.
     */
    public boolean isNoXRadar() {
        return noXRadar;
    }

    /**
     * Section of undocumented data.
     * @return pack0.
     */
    public byte[] getPack0() {
        return pack0;
    }

    /**
     * Fetches the ship settings for a specific ship.
     * @param internalShipType Ship type according to the INTERNAL list in {@link Ship}. 
     * @return Ship settings for the specific ship, or null when an invalid parameter has been given.
     */
    public ShipSettings getShipSetting(byte internalShipType) {
        if(internalShipType < Ship.INTERNAL_WARBIRD || internalShipType > Ship.INTERNAL_SHARK) 
            return null;
        
        return shipSettings[internalShipType];
    }
    
    /**
     * Fetches all the ship settings.
     * @return {@link ShipSettings} array of length 8, with each index matching {@link Ship#INTERNAL_WARBIRD} to {@link Ship#INTERNAL_SHARK}.
     */
    public ShipSettings[] getShipSettings() {
        return shipSettings;
    }

    /**
     * <b>[Bullet] BulletDamageLevel</b><br>
     * Maximum amount of damage that a L1 bullet will cause.<br>
     * <i>Formula:</i> damage = squareroot(rand# * (max damage^2 + 1)).<br>
     * <b>NOTE:</b> This damage is * 1000 compared to the actual arena settings. I.e. if the setting is 5, this value will be 5000.
     * @return Bullet damage level for a L1 bullet in energy units.
     */
    public long getBulletDamageLevel() {
        return (long) (bulletDamageLevel & MASK_UINT32);
    }

    /**
     * <b>[Bomb] BombDamageLevel</b><br>
     * Amount of damage a bomb causes at its center point (for all bomb levels).<br>
     * <b>NOTE:</b> This damage is *1000 compared to the actual arena setting. I.e. if the setting is 5, this value will be 5000.
     * @return Bomb damage level in energy units.
     */
    public long getBombDamageLevel() {
        return (long) (bombDamageLevel & MASK_UINT32);
    }

    /**
     * <b>[Bullet] BulletAliveTime</b><br>
     * How long bullets live before disappearing (in hundredths of a second).
     * @return Bullet alive time in centiseconds.
     */
    public long getBulletAliveTime() {
        return (long) (bulletAliveTime & MASK_UINT32);
    }

    /**
     * <b>[Bomb] BombAliveTime</b><br>
     * Time a bomb can fly before is disappears. This is really a networking setting and not a weapon setting,
     * so the default should be sufficient. This setting can be used to control the range of bombs and can be
     * used to restrict line bombing. If your settings allow a lot of bombs to be flying around at once,
     * lowering the value of this setting can improve frame rates.
     * @return Bomb alive time in centiseconds.
     */
    public long getBombAliveTime() {
        return (long) (bombAliveTime & MASK_UINT32);
    }

    /**
     * <b>[Misc] DecoyAliveTime</b><br>
     * Time before a decoy disappears. Note that decoy will instantly disappear if owner enters safe zone.
     * @return Decoy alive time in centiseconds.
     */
    public long getDecoyAliveTime() {
        return (long) (decoyAliveTime & MASK_UINT32);
    }

    /**
     * <b>[Misc] SafetyLimit</b><br>
     * Time a player can spend in the safe zone before getting kicked. Use this to encourage users to spec when AFK. (90000 = 15 mins).
     * @return Safety limit in centiseconds.
     */
    public long getSafetyLimit() {
        return (long) (safetyLimit & MASK_UINT32);
    }

    /**
     * <b>[Misc] FrequencyShift</b><br>
     * Random frequency shift applied to sounds to make them more realistic.
     * @return Frequency shift in Hz.
     */
    public long getFrequencyShift() {
        return (long) (frequencyShift & MASK_UINT32);
    }

    /**
     * <b>[Team] MaxFrequency</b><br>
     * Maximum frequency allowed in arena. Note that this is not the number of frequencies. (5 would allow frequencies 0,1,2,3,4)
     * This does not include spectator frequencies!
     * @return Maximum allowed playable frequencies.
     */
    public long getMaxFrequency() {
        return (long) (maxFrequency & MASK_UINT32);
    }

    /**
     * <b>[Repel] RepelSpeed</b><br>
     * Speed at which players are repelled.
     * @return Repel speed in pixels per 10 seconds.
     */
    public long getRepelSpeed() {
        return (long) (repelSpeed & MASK_UINT32);
    }

    /**
     * <b>[Mine] MineAliveTime</b><br>
     * Time before mines disappear if no one runs into them (in hundredths of a second).
     * @return Mine alive time in centiseconds.
     */
    public long getMineAliveTime() {
        return (long) (mineAliveTime & MASK_UINT32);
    }

    /**
     * [Burst] BurstDamageLevel
     * Maximum amount of damage caused by a single burst bullet. Note that since Continuum .36,
     * this specifies the constant amount of damage, instead of random, if Bullet:ExactDamage is enabled.<br>
     * <b>NOTE:</b> This damage is * 1000 compared to the actual arena settings. I.e. if the setting is 5, this value will be 5000.
     * @return Burst damage level in energy units.
     */
    public long getBurstDamageLevel() {
        return (long) (burstDamageLevel & MASK_UINT32);
    }

    /**
     * <b>[Bullet] BulletDamageUpgrade</b><br>
     * Amount of extra damage each bullet level will cause.<br>
     * <b>NOTE:</b> This damage is * 1000 compared to the actual arena settings. I.e. if the setting is 5, this value will be 5000.
     * @return Bullet damage upgrade amount in energy units.
     */
    public long getBulletDamageUpgrade() {
        return (long) (bulletDamageUpgrade & MASK_UINT32);
    }

    /**
     * <b>[Flag] FlagDropDelay</b><br>
     * Time before flag is dropped by carrier. Special value: 0 = no timer.
     * @return Flag drop delay in centiseconds. When flag is only dropped upon death or warping, then 0.
     */
    public long getFlagDropDelay() {
        return (long) (flagDropDelay & MASK_UINT32);
    }

    /**
     * <b>[Flag] EnterGameFlaggingDelay</b><br>
     * Delay before an newly-entered player is allowed to see flags.
     * @return Enter game flagging delay in centiseconds.
     */
    public long getEnterGameFlaggingDelay() {
        return (long) (enterGameFlaggingDelay & MASK_UINT32);
    }

    /**
     * <b>[Rocket] RocketThrust</b><br>
     * The rocket will update a player's thrust to this value while the rocket is active.
     * @return Rocket thrust in an unknown unit.
     */
    public long getRocketThrust() {
        return (long) (rocketThrust & MASK_UINT32);
    }

    /**
     * <b>[Rocket] RocketSpeed</b><br>
     * The rocket will update a player's speed to this value while the rocket is active.
     * @return Rocket speed in pixels per 10 seconds.
     */
    public long getRocketSpeed() {
        return (long) (rocketSpeed & MASK_UINT32);
    }

    /**
     * <b>[Shrapnel] InactiveShrapnelDamage</b><br>
     * Damage shrapnel causes in its first 1/4 second of life. Though it is not suggested in the default files,
     * this number is only a base. Inactive shrapnel also seems to change based on gun level.
     * L1 inactive shrapnel does 0 damage. L2 deals this setting worth of damage.
     * L3 deals twice as much, and L4 inactive shrapnel (only attainable when you have no gun at all) deals triple.
     * <b>NOTE:</b> This damage is * 1000 compared to the actual arena settings. I.e. if the setting is 5, this value will be 5000.
     * @return Inactive shrapnel damage in energy units.
     */
    public long getInactiveShrapnelDamage() {
        return (long) (inactiveShrapnelDamage & MASK_UINT32);
    }

    /**
     * <b>[Wormhole] SwitchTime</b><br>
     * How often the wormhole switches its destination.
     * @return Wormhole switch time in centiseconds.
     */
    public long getWormholeSwitchTime() {
        return (long) (wormholeSwitchTime & MASK_UINT32);
    }

    /**
     * <b>[Misc] ActivateAppShutdownTime</b><br>
     * Time a ship is shutdown when user reactivates client application (when user comes back from Windows mode).
     * @return Activate app shutdown time in centiseconds.
     */
    public long getActivateAppShutdownTime() {
        return (long) (activateAppShutdownTime & MASK_UINT32);
    }

    /**
     * <b>[Shrapnel] ShrapnelSpeed</b><br>
     * Speed that shrapnel travels.
     * @return Shrapnel speed in pixels per 10 seconds.
     */
    public long getShrapnelSpeed() {
        return (long) (shrapnelSpeed & MASK_UINT32);
    }

    /**
     * Section of undocumented data.
     * @return UNKNOWN0
     */
    public byte[] getUNKNOWN0() {
        return UNKNOWN0;
    }

    /**
     * <b>[Latency] SendRoutePercent</b><br>
     * Percentage of the ping time that is spent on the ClientToServer portion of the ping.
     * (Used in more accurately syncronizing clocks.)
     * @return Send route percent in promille. (1000 = 100%)
     */
    public int getSendRoutePercent() {
        return (int) (sendRoutePercent & MASK_UINT16);
    }

    /**
     * <b>[Bomb] BombExplodeDelay</b><br>
     * How long after the proximity sensor is triggered before bomb explodes.
     * (Note: it explodes immediately if ship moves away from it after triggering it.)
     * @return Bomb explode delay in centiseconds.
     */
    public int getBombExplodeDelay() {
        return (int) (bombExplodeDelay & MASK_UINT16);
    }

    /**
     * <b>[Misc] SendPositionDelay</b><br>
     * Time between position packets sent by clients. Do not modify this setting without a good reason.
     * @return Send position delay in centiseconds.
     */
    public int getSendPositionDelay() {
        return (int) (sendPositionDelay & MASK_UINT16);
    }

    /**
     * <b>[Bomb] BombExplodePixels</b><br>
     * Initial blast radius for a bomb, then multiplied by bomb level (so L1 gets this, L2 gets double, etc.).
     * Note that this is purely the damage radius, not affected by prox.
     * @return Bomb explode pixels in pixels.
     */
    public int getBombExplodePixels() {
        return (int) (bombExplodePixels & MASK_UINT16);
    }

    /**
     * <b>[Prize] DeathPrizeTime</b><br>
     * Time the prize exists that appears after killing somebody.
     * @return Death prize time in centiseconds.
     */
    public int getDeathPrizeTime() {
        return (int) (deathPrizeTime & MASK_UINT16);
    }

    /**
     * <b>[Bomb] JitterTime</b><br>
     * How long the screen jitters from a bomb hit.
     * @return Jitter time in centiseconds.
     */
    public int getJitterTime() {
        return (int) (jitterTime & MASK_UINT16);
    }

    /**
     * <b>[Kill] EnterDelay</b><br>
     * Delay before a killed player re-enters game (respawns).
     * <i>Special value:</i> 0 = player respawns at same location after a very short delay.
     * @return Enter delay in centiseconds.
     */
    public int getEnterDelay() {
        return (int) (enterDelay & MASK_UINT16);
    }

    /**
     * <b>[Prize] EngineShutdownTime</b><br>
     * How long an "Engine Shutdown" prize effects your ship. When you get an engine shutdown 
     * you cannot accelerate and your rotation speed is decreased significantly.
     * @return Engine shutdown time in centiseconds.
     */
    public int getEngineShutdownTime() {
        return (int) (engineShutdownTime & MASK_UINT16);
    }

    /**
     * <b>[Bomb] ProximityDistance</b><br>
     * Radius of proximity trigger of an L1 bomb. Each bomb level adds 1 to this amount.
     * @return Proximity distance in pixels.
     */
    public int getProximityDistance() {
        return (int) (proximityDistance & MASK_UINT16);
    }

    /**
     * <b>[Kill] BountyIncreaseForKill</b><br>
     * Points added to players bounty each time he kills an opponent.
     * @return Bounty increase for kill in points.
     */
    public int getBountyIncreaseForKill() {
        return (int) (bountyIncreaseForKill & MASK_UINT16);
    }

    /**
     * <b>[Misc] BounceFactor</b><br>
     * How bouncy the walls are. <i>Formula:</i> SpeedAfter = SpeedBefore * (16 / BounceFactor).
     * @return Bounce factor (multiplier).
     */
    public int getBounceFactor() {
        return (int) (bounceFactor & MASK_UINT16);
    }

    /**
     * <b>[Radar] MapZoomFactor</b><br>
     * A number representing how far you can see on radar.
     * @return Map zoom factor.
     */
    public int getMapZoomFactor() {
        return (int) (mapZoomFactor & MASK_UINT16);
    }

    /**
     * <b>[Kill] MaxBonus</b><br>
     * This is if you have flags, the maximum amount of points that can be awarded per kill.
     * @return Max bonus in points.
     */
    public int getMaxBonus() {
        return (int) (maxBonus & MASK_UINT16);
    }

    /**
     * <b>[Kill] MaxPenalty</b><br>
     * This is if you have flags, the maximum amount of points that can be taken away per a death.
     * @return Max penalty in points.
     */
    public int getMaxPenalty() {
        return (int) (maxPenalty & MASK_UINT16);
    }

    /**
     * <b>[Kill] RewardBase</b><br>
     * This is shown added to a person's bounty, but isn't added from points for a kill.
     * @return Reward base in points.
     */
    public int getRewardBase() {
        return (int) (rewardBase & MASK_UINT16);
    }

    /**
     * <b>[Repel] RepelTime</b><br>
     * How long a repel affects players.
     * @return Repel time in centiseconds.
     */
    public int getRepelTime() {
        return (int) (repelTime & MASK_UINT16);
    }

    /**
     * <b>[Repel] RepelDistance</b><br>
     * Distance that a repel has an affect on players.
     * @return Repel distance in pixels.
     */
    public int getRepelDistance() {
        return (int) (repelDistance & MASK_UINT16);
    }

    /**
     * <b>[Misc] TickerDelay</b><br>
     * Time between ticker help messages.
     * @return Ticker delay in centiseconds.
     */
    public int getHelpTickerDelay() {
        return (int) (helpTickerDelay & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlaggerOnRadar</b><br>
     * Whether the flaggers appear on radar in red.
     * @return True when flaggers are marked in red on the radar, otherwise false.
     */
    public boolean isFlaggerOnRadar() {
        return flaggerOnRadar;
    }

    /**
     * <b>[Flag] FlaggerKillMultiplier</b><br>
     * Number of times more points are given to a flagger. <i>Formula:</i> Points = (FlaggerKillMultiplier + 1) * InitialPoints.
     * @return Flagger kill multiplier.
     */
    public int getFlaggerKillMultiplier() {
        return (int) (flaggerKillMultiplier & MASK_UINT16);
    }

    /**
     * <b>[Prize] PrizeFactor</b><br>
     * Adjusts number of prizes generated per player. <i>Formula:</i> NumPrizes = PrizeFactor * PlayerCount / 1000.
     * @return Prize factor.
     */
    public int getPrizeFactor() {
        return (int) (prizeFactor & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeDelay</b><br>
     * How often server regenerates prizes.
     * @return Prize delay in centiseconds.
     */
    public int getPrizeDelay() {
        return (int) (prizeDelay & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeMinimumVirtual</b><br>
     * Base distance from center of arena that prizes/flags/soccer-balls will generate.
     * Setting this below 8 will crash subgame.
     * @return Prize minimum virtual in tiles.
     */
    public int getPrizeMinimumVirtual() {
        return (int) (prizeMinimumVirtual & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeUpgradeVirtual</b><br>
     * Additional distance added to MinimumVirtual per player in game.
     * @return Prize upgrade virtual in tiles.
     */
    public int getPrizeUpgradeVirtual() {
        return (int) (prizeUpgradeVirtual & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeMaxExist</b><br>
     * Maximum random time that a hidden prize will remain on screen.
     * @return Prize max exist in centiseconds.
     */
    public int getPrizeMaxExist() {
        return (int) (prizeMaxExist & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeMinExist</b><br>
     * Minimum random time that a hidden prize will remain on screen.
     * @return Prize min exist in centiseconds.
     */
    public int getPrizeMinExist() {
        return (int) (prizeMinExist & MASK_UINT16);
    }
    
    /**
     * <b>[Prize] PrizeNegativeFactor</b><br>
     * The chance of greening a negative prize, higher values mean less likely, 0 means never. 
     * <i>Formula:</i> NegativePrizeCount = 1 / PrizeNegativeFactor * TotalPrizeCount.
     * A negative prize takes an upgrade away from your ship, such as antiwarp.
     * @return Prize negative factor.
     */
    public int getPrizeNegativeFactor() {
        return (int) (prizeNegativeFactor & MASK_UINT16);
    }

    /**
     * <b>[Door] DoorDelay</b><br>
     * How often doors attempt to switch their state.
     * @return Door delay in centiseconds.
     */
    public int getDoorDelay() {
        return (int) (doorDelay & MASK_UINT16);
    }

    /**
     * <b>[Toggle] AntiWarpPixels</b><br>
     * Distance Anti-Warp affects other players. Note that AntiWarp is still limited by the radar area.
     * @return Antiwarp pixels in pixels.
     */
    public int getAntiwarpPixels() {
        return (int) (antiwarpPixels & MASK_UINT16);
    }

    /**
     * <b>[Door] DoorMode</b><br>
     * Each bit of this field determines whether its represented door is open or not. 
     * The 8 bits, from left to right, represent the 8 door tiles in the tileset, from left to right.<br>
     * Special values:
     * <li>-2 = all doors completely random
     * <li>-1 = weighted random (some doors open more often than others)
     * <li>0-255 = fixed doors (1 bit of byte for each door specifying whether it is open or not)
     * @return Door mode as bitfield.
     */
    public short getDoorMode() {
        return doorMode;
    }

    /**
     * <b>[Flag] FlagBlankDelay</b><br>
     * Amount of time that a user can get no data from server before the client will hide flags for 10 seconds. 
     * Lessen this if you have lag-related flagging trouble.
     * @return Flag blank delay in centiseconds.
     */
    public int getFlagBlankDelay() {
        return (int) (flagBlankDelay & MASK_UINT16);
    }

    /**
     * <b>[Flag] NoDataFlagDropDelay</b><br>
     * Amount of time that a user can get no data from server before flags he is carrying are dropped. 
     * @return No data flag drop delay in centiseconds.
     */
    public int getNoDataFlagDropDelay() {
        return (int) (noDataFlagDropDelay & MASK_UINT16);
    }

    /**
     * <b>[Prize] MultiPrizeCount</b><br>
     * Amount of prizes within a multiprize green.
     * @return Multiprize count.
     */
    public int getMultiPrizeCount() {
        return (int) (multiPrizeCount & MASK_UINT16);
    }

    /**
     * <b>[Brick] BrickTime</b><br>
     * How long bricks last.
     * @return Brick time in centiseconds.
     */
    public int getBrickTime() {
        return (int) (brickTime & MASK_UINT16);
    }

    /**
     * <b>[Misc] WarpRadiusLimit</b><br>
     * Ships randomly placed on the map can be a maximum of this far from the map's center.
     * Note that 1024 will cover the entire map.
     * @return Warp radius limit in tiles.
     */
    public int getWarpRadiusLimit() {
        return (int) (warpRadiusLimit & MASK_UINT16);
    }

    /**
     * <b>[Bomb] EBombShutdownTime</b><br>
     * Maximum time an EMP bomb suspends recharge. When the EMP hits the player indirectly
     * (through the blast radius), this time is reduced.
     * @return EMP bomb shutdown time in centiseconds.
     */
    public int geteBombShutdownTime() {
        return (int) (eBombShutdownTime & MASK_UINT16);
    }

    /**
     * <b>[Bomb] EBombDamagePercent</b><br>
     * Percentage of normal damage applied to an EMP bomb.
     * @return EMP bomb damage percent in promille. (1000 = 100%, 2000 = 200%)
     */
    public int geteBombDamagePercent() {
        return (int) (eBombDamagePercent & MASK_UINT16);
    }

    /**
     * <b>[Radar] RadarNeutralSize</b><br>
     * Size of area between blinded radar zones.
     * @return Radar neutral size in pixels? (0 to 1024)
     */
    public int getRadarNeutralSize() {
        return (int) (radarNeutralSize & MASK_UINT16);
    }

    /**
     * <b>[Misc] WarpPointDelay</b><br>
     * How long a Portal point is active.
     * @return Warppoint delay in centiseconds.
     */
    public int getWarpPointDelay() {
        return (int) (warpPointDelay & MASK_UINT16);
    }

    /**
     * <b>[Misc] NearDeathLevel</b><br>
     * Energy that constitutes a near-death experience. The server will reduce the ship's
     * bounty by 1 when this occurs. (Used for dueling zones.)
     * @return Near death level in energy.
     */
    public int getNearDeathLevel() {
        return (int) (nearDeathLevel & MASK_UINT16);
    }

    /**
     * <b>[Bomb] BBombDamagePercent</b><br>
     * Percentage of normal damage applied to a bouncing bomb. Note that even though a bouncing bomb
     * loses its bouncing picture after the last bounce, it retains this percentage modifier.
     * @return Bouncing bomb damage percent in promille. (1000 = 100%, 2000 = 200%)
     */
    public int getbBombDamagePercent() {
        return (int) (bBombDamagePercent & MASK_UINT16);
    }

    /**
     * <b>[Shrapnel] ShrapnelDamagePercent</b><br>
     * Percentage of normal bullet damage applied to shrapnel (relative to same level).
     * Note that shrapnel does obey Bullet:ExactDamage.
     * @return Shrapnel damage percent in promille. (1000 = 100%, 2000 = 200%)
     */
    public int getShrapnelDamagePercent() {
        return (int) (shrapnelDamagePercent & MASK_UINT16);
    }

    /**
     * <b>[Latency] ClientSlowPacketTime</b><br>
     * The client will consider an S2C packet slow if it takes this long or longer to receive.
     * @return Client slow packet time in centiseconds.
     */
    public int getClientSlowPacketTime() {
        return (int) (clientSlowPacketTime & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlagDropResetReward</b><br>
     * Minimum kill reward that a flagger must get to reset his drop timer. 
     * See Flag Settings for flag reward customization.
     * @return Flag drop reset reward in points.
     */
    public int getFlagDropResetReward() {
        return (int) (flagDropResetReward & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlaggerFireCostPercent</b><br>
     * Percentage of normal weapon firing cost for flaggers. Note that 0% will essentially give flaggers superpower.
     * @return Flagger fire cost percent in promille. (1000 = 100%, 2000 = 200%)
     */
    public int getFlaggerFireCostPercent() {
        return (int) (flaggerFireCostPercent & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlaggerDamagePercent</b><br>
     * Percentage of normal damage received by flaggers. Note that 0% will essentially make flaggers invincible.
     * @return Flagger damage percent in promille. (1000 = 100%, 2000 = 200%)
     */
    public int getFlaggerDamagePercent() {
        return (int) (flaggerDamagePercent & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlaggerBombFireDelay</b><br>
     * Delay added to standard bomb firing delay for flaggers. Note: Please do not set this number between 1 and 20.
     * @return Flagger bomb fire delay in centiseconds.
     */
    public int getFlaggerBombFireDelay() {
        return (int) (flaggerBombFireDelay & MASK_UINT16);
    }

    /**
     * <b>[Soccer] PassDelay</b><br>
     * How long after the ball is fired before anybody can pick it up.
     * @return Soccer pass delay in centiseconds.
     */
    public int getSoccerPassDelay() {
        return (int) (soccerPassDelay & MASK_UINT16);
    }

    /**
     * <b>[Soccer] BallBlankDelay</b><br>
     * Amount of time a player can receive no data from server and still pick up the soccer ball.
     * @return Soccer ball blank delay in centiseconds.
     */
    public int getSoccerBallBlankDelay() {
        return (int) (soccerBallBlankDelay & MASK_UINT16);
    }

    /**
     * <b>[Latency] S2CNoDataKickoutDelay</b><br>
     * Time a client can receive no data from server before it automatically disconnects.
     * This must be higher than the KeepAliveDelay specified in server.ini.
     * @return S2C no data kickout delay in centiseconds.
     */
    public int getS2CNoDataKickoutDelay() {
        return (int) (s2CNoDataKickoutDelay & MASK_UINT16);
    }

    /**
     * <b>[Flag] FlaggerThrustAdjustment</b><br>
     * Thrust adjustment for flaggers. Note that negative numbers will reduce their thrust.
     * @return Flagger thrust adjustment in an unknown unit. (Range: -128 to 128)
     */
    public short getFlaggerThrustAdjustment() {
        return flaggerThrustAdjustment;
    }

    /**
     * <b>[Flag] FlaggerSpeedAdjustment</b><br>
     * Speed adjustment for flaggers. Note that negative numbers will slow down flaggers.
     * @return Flagger speed adjustment in pixels per 10 seconds. (Range: -128 to 128). 
     */
    public short getFlaggerSpeedAdjustment() {
        return flaggerSpeedAdjustment;
    }

    /**
     * <b>[Latency] ClientSlowPacketSampleSize
     * Number of packets to sample S2C latency before checking for kickout.
     * Only in very rare cases will this need to be altered, so just leave it at its default.
     * @return Client slow packet sample size, unitless.
     */
    public int getCliSlowPacketSampleSize() {
        return (int) (cliSlowPacketSampleSize & MASK_UINT16);
    }

    /**
     * Section of undocumented data.
     * @return UNKNOWN1
     */
    public byte[] getUNKNOWN1() {
        return UNKNOWN1;
    }

    /**
     * <b>[Shrapnel] Random</b><br>
     * Whether shrapnel spreads randomly instead of in the standard circle pattern.
     * @return True when spread randomly, false when spread evenly.
     */
    public boolean isRandomShrapnel() {
        return randomShrapnel;
    }

    /**
     * <b>[Soccer] BallBounce</b><br>
     * Whether the ball bounces off walls instead of going through them.
     * @return True when the ball bounces, otherwise false.
     */
    public boolean isSoccerBallBounce() {
        return soccerBallBounce;
    }

    /**
     * <b>[Soccer] AllowBombs</b><br>
     * Whether the ball carrier can fire bombs. If a ball carrier tries to fire
     * a bomb and this settings disabled, the ball will be released before the bomb.
     * @return True when a carrier can fire a bomb while keeping possession of the ball. False otherwise.
     */
    public boolean isSoccerAllowBombs() {
        return soccerAllowBombs;
    }

    /**
     * <b>[Soccer] AllowGuns</b><br>
     * Whether the ball carrier can fire his guns. If the carrier tries, the ball will be released before the bullets fire.
     * @return True when a carrier can fire his/her gun while keeping possession of the ball. False otherwise.
     */
    public boolean isSoccerAllowGuns() {
        return soccerAllowGuns;
    }

    /**
     * <b>[Soccer] Mode</b><br>
     * Goal configuration.
     * <li>0: any goal
     * <li>1: left-half/right-half
     * <li>2: top-half/bottom-half
     * <li>3: quadrants-defend-one-goal
     * <li>4: quadrants-defend-three-goals
     * <li>5: sides-defend-one-goal
     * <li>6: sides-defend-three-goals
     * @return One of the above options, depending on the mode.
     */
    public byte getSoccerMode() {
        return soccerMode;
    }

    /**
     * <b>[Team] MaxPerTeam</b><br>
     * Maximum playercount on a public frequency.
     * @return Max per team count.
     */
    public short getMaxPerTeam() {
        return (short) (maxPerTeam & MASK_UINT8);
    }

    /**
     * <b>[Team] MaxPerPrivateTeam</b><br>
     * Maximum playercount on a private frequency. Special Value: 0 = same as MaxPerTeam.<br>
     * <b>Note:</b> This function will automatically return MaxPerTeam if MaxPerPrivateTeam's value is 0.
     * @return When 0, Max per team count, otherwise Max per private team count.
     */
    public short getMaxPerPrivateTeam() {
        if(maxPerPrivateTeam == 0)
            return getMaxPerTeam();
        return (short) (maxPerPrivateTeam & MASK_UINT8);
    }
    
    /**
     * <b>[Team] MaxPerPrivateTeam</b><br>
     * Maximum playercount on a private frequency. Special Value: 0 = same as MaxPerTeam.<br>
     * <b>Note:</b> This function will return the raw data.
     * @return Max per private team count. Note: 0 indicates it is equal to {@link #getMaxPerTeam()}.
     */
    public short getRawMaxPerPrivateTeam() {
        return (short) (maxPerPrivateTeam & MASK_UINT8);
    }

    /**
     * <b>[Mine] TeamMaxMines</b><br>
     * Maximum mines allowed per team.
     * @return Team max mines, up to 32000.
     */
    public short getTeamMaxMines() {
        return teamMaxMines;
    }

    /**
     * <b>[Wormhole] GravityBombs</b><br>
     * Whether a wormhole affects bombs.
     * @return True when bombs are affected by the wormhole's gravitational pull. False otherwise.
     */
    public boolean isWormholeGravityBombs() {
        return wormholeGravityBombs;
    }

    /**
     * <b>[Bomb] BombSafety</b><br>
     * Enables a firing safety for proximity bombs. If an enemy ship is within proximity radius,
     * the firing safety will not let the user fire.
     * @return True when a player cannot fire while in the proximity trigger range of a bomb.
     */
    public boolean isBombSafety() {
        return bombSafety;
    }

    /**
     * <b>[Message] MessageReliable</b><br>
     * Whether messages are sent reliably, which could increase bandwidth usage.
     * @return True when messages need to be sent reliably, otherwise false.
     */
    public boolean isMessageReliable() {
        return messageReliable;
    }

    /**
     * <b>[Prize] TakePrizeReliable</b><br>
     * Whether client-to-server prize packets are sent reliably.
     * This will increase bandwidth usage, but also helps maintain integrity.
     * @return True when this setting is enabled. False otherwise.
     */
    public boolean isTakePrizeReliable() {
        return takePrizeReliable;
    }

    /**
     * <b>[Message] AllowAudioMessages</b><br>
     * Whether players can send audio messages.
     * @return True when players are allowed to send audio messages. False otherwise.
     */
    public boolean isAllowAudioMessages() {
        return allowAudioMessages;
    }

    /**
     * <b>[Prize] PrizeHideCount</b><br>
     * Number of prizes that server regenerates every PrizeDelay.
     * Note that this is not the total number of prizes (which is determined by PrizeFactor).
     * @return Prize hide count.
     */
    public short getPrizeHideCount() {
        return (short) (prizeHideCount & MASK_UINT8);
    }

    /**
     * <b>[Misc] ExtraPositionData</b><br>
     * Whether regular players receive sysop data about a ship. Leave this off.
     * @return True when players receive the data. False otherwise.
     */
    public boolean isExtraPositionData() {
        return extraPositionData;
    }

    /**
     * <b>[Misc] SlowFrameCheck</b><br>
     * Whether to check for slow frames on the client (possible cheat technique).
     * <b>Warning:</b> flawed on some machines, do not use.
     * @return True when slow frame checking is enabled. False otherwise.
     */
    public boolean isSlowFrameCheck() {
        return slowFrameCheck;
    }

    /**
     * <b>[Flag] CarryFlags</b><br>
     * <li>0: Flags cannot be picked up.
     * <li>1: A player can pick up and carry all flags.
     * <li>2: A player can pick up and carry only one flag at a time.
     * @return One of the values above.
     */
    public byte getCarryFlags() {
        return carryFlags;
    }

    /**
     * <b>[Misc] AllowSavedShips</b><br>
     * Whether ships' stats are saved between arenas and after lagouts.
     * Do not enable this if subarenas have different ship settings.
     * @return True when stats are saved, false otherwise.
     */
    public boolean isAllowSavedShip() {
        return allowSavedShip;
    }

    /**
     * <b>[Radar] RadarMode</b><br>
     * Radar mode.
     * <li>0: normal
     * <li>1: half/half
     * <li>2: quarters
     * <li>3: half/half-see team mates
     * <li>4: quarters-see team mates
     * @return One of the options above.
     */
    public byte getRadarMode() {
        return radarMode;
    }

    /**
     * <b>[Misc] VictoryMusic</b><br>
     * Whether the zone plays the victory music when a team is winning a flag game.
     * @return True when this is enabled, false otherwise.
     */
    public boolean isVictoryMusic() {
        return victoryMusic;
    }

    /**
     * <b>[Flag] FlaggerGunUpgrade</b><br>
     * Whether the flaggers get a gun upgrade. This is the only way to have L4 guns.
     * @return True when this feature is enabled, false otherwise.
     */
    public boolean isFlaggerGunUpgrade() {
        return flaggerGunUpgrade;
    }

    /**
     * <b>[Flag] FlaggerBombUpgrade</b><br>
     * Whether the flaggers get a bomb upgrade. This is the only way to have L4 bombs.
     * @return True when this feature is enabled, false otherwise.
     */
    public boolean isFlaggerBombUpgrade() {
        return flaggerBombUpgrade;
    }

    /**
     * <b>[Soccer] UseFlagger</b><br>
     * If player with soccer ball should use the Flag:Flagger* ship adjustments or not.
     * @return True when the Flagger* settings are applied to someone carrying the ball. False if this isn't the case.
     */
    public boolean isSoccerUseFlagger() {
        return soccerUseFlagger;
    }

    /**
     * <b>[Soccer] BallLocation</b><br>
     * Whether the balls' locations are displayed at all times or not.
     * @return True when the ball is always visible, false otherwise.
     */
    public boolean isSoccerBallLocation() {
        return soccerBallLocation;
    }

    /**
     * Section of undocumented data.
     * @return UNKNOWN2
     */
    public byte[] getUNKNOWN2() {
        return UNKNOWN2;
    }

    /**
     * Fetches the prize settings.
     * @return This arena's prize settings.
     */
    public PrizeSettings getPrizeSettings() {
        return prizeSettings;
    }
    
    /**
     * Debug method. Prints out all getter values. 
     * <p>
     * This is/was mainly used to verify that every getter is returning
     * the correct signed/unsigned value. Could prove useful in the future
     * whenever a discrepancy is found between what the bot thinks is an arena setting
     * and what actually is the arena setting.
     * <p>
     * Note: This skips printing the raw data, and automatically executes the toString's of its child classes.
     * @return List of getter names and their values.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        
        Class<?> objClass = this.getClass();
        
        result.append(objClass.getName() + " {" + newLine);
        
        // Get the public methods associated with this class.
        Method[] methods = objClass.getMethods();
        for (Method method:methods)
        {
            if(method.getName().startsWith("getClass") || method.getName().startsWith("getRawData")
                    || method.getName().startsWith("getRawSettings") || method.getName().startsWith("getShipSettings")) {
                continue;
            } else if(method.getName().startsWith("getShipSetting")) {
                for(byte j = 0; j < 8; j++) {
                    result.append(Tools.shipName(j + 1) + ":" + newLine);
                    result.append(getShipSetting(j).toString());
                }
            } else if(method.getName().startsWith("getPrizeSettings")) {
                result.append(getPrizeSettings().toString());
            } else if(method.getName().startsWith("get") || method.getName().startsWith("is")) {
                try {
                    result.append(method.getName() + ": " + method.invoke(this, new Object[] {}) + newLine);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            
        }
        
        result.append("}" + newLine);
        return result.toString();
    }

    /**
     * This class logs all the ship related settings per ship type, obtained from the arena settings.
     * <p>
     * These settings determine the properties of each of the eight ships. These go in an arena's .cfg file
     * for subgame and in arena.conf for ASSS. Ship names themselves can be changed with shipinfo.dat.
     * <p>
     * The various section tags, [Warbird] [Javelin] [Spider] [Leviathan] [Terrier] [Weasel] [Lancaster] [Shark],
     * have been condensed into one tag [All], for documentation purposes.
     * <p>
     * Due to this being a port, I've tried to give credit where credit is due, by trying to keep most of the original
     * comments in tact. Full credit goes to the researchers for and creators of the MervBot.
     * @author Trancid
     *
     */
    public class ShipSettings {
        // 144 bytes wide, offsets are for warbird
        // Mostly Snrrrub                       //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private short   superTime;              // 0004     | All       | SuperTime                 |     1 |       |        16 | How long Super lasts on the ship (in hundredths of a second)
        private short   UNKNOWN0;               // 0006     | (100)     | Salt for actual super time?
        private short   shieldsTime;            // 0008     | All       | ShieldsTime               |     1 |       |        16 | How long Shields lasts on the ship (in hundredths of a second)
        private short   UNKNOWN1;               // 0010     | (30)      | Salt for actual shields time?
        private short   gravity;                // 0012     | All       | Gravity                   |       |       |        16 | Uses this formula, where R = raduis (tiles) and g = this setting; R = 1.325 * (g ^ 0.507)  IE: If set to 500, then your ship will start to get pulled in by the wormhole once you come within 31 tiles of it
        private short   gravityTopSpeed;        // 0014     | All       | GravityTopSpeed           |       |       |        16 | Ship are allowed to move faster than their maximum speed while effected by a wormhole.  This determines how much faster they can go (0 = no extra speed)
        private short   bulletFireEnergy;       // 0016     | All       | BulletFireEnergy          |       |       |        16 | Amount of energy it takes a ship to fire a single L1 bullet
        private short   multiFireEnergy;        // 0018     | All       | MultiFireEnergy           |       |       |        16 | Amount of energy it takes a ship to fire multifire L1 bullets
        private short   bombFireEnergy;         // 0020     | All       | BombFireEnergy            |       |       |        16 | Amount of energy it takes a ship to fire a single bomb
        private short   bombFireEnergyUpgrade;  // 0022     | All       | BombFireEnergyUpgrade     |       |       |        16 | Extra amount of energy it takes a ship to fire an upgraded bomb. ie. L2 = BombFireEnergy+BombFireEnergyUpgrade
        private short   mineFireEnergy;         // 0024     | All       | LandmineFireEnergy        |       |       |        16 | Amount of energy it takes a ship to place a single L1 mine
        private short   mineFireEnergyUpgrade;  // 0026     | All       | LandmineFireEnergyUpgrade |       |       |        16 | Extra amount of energy it takes to place an upgraded landmine.  ie. L2 = LandmineFireEnergy+LandmineFireEnergyUpgrade
        private short   bulletSpeed;            // 0028     | All       | BulletSpeed               |       |       |        16 | How fast bullets travel
        private short   bombSpeed;              // 0030     | All       | BombSpeed                 |       |       |        16 | How fast bombs travel
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    seeBombLevel;           // 0032     | All       | SeeBombLevel              |     0 |     4 |         3 | If ship can see bombs on radar (0=Disabled, 1=All, 2=L2 and up, 3=L3 and up, 4=L4 bombs only) [Continuum .36]
        private boolean disableFastBombs;       // 0032     | All       | DisableFastShooting       |     0 |     1 |         1 | If firing bullets, bombs, or thors is disabled after using afterburners (1=enabled) [Continuum .36]
        private byte    radius;                 // 0032     | All       | Radius                    |       |       |         7 | The ship's radius from center to outside, in pixels. Standard value is 14 pixels. [Continuum .37]
        private byte    pack0;                  // 0033     | Unused    | ?                         |       |       |         6 | (fixed/updated to whatever is current by Niadh@columbus.rr.com)
        private short   multiFireAngle;         // 0034     | All       | MultiFireAngle            |       |       |        16 | Angle spread between multi-fire bullets and standard forward firing bullets. (111 = 1 degree, 1000 = 1 ship-rotation-point)
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private short   cloakEnergy;            // 0036     | All       | CloakEnergy               |     0 | 32000 |        16 | Amount of energy required to have 'Cloak' activated (thousanths per hundredth of a second)
        private short   stealthEnergy;          // 0038     | All       | StealthEnergy             |     0 | 32000 |        16 | Amount of energy required to have 'Stealth' activated (thousanths per hundredth of a second)
        private short   antiWarpEnergy;         // 0040     | All       | AntiWarpEnergy            |     0 | 32000 |        16 | Amount of energy required to have 'Anti-Warp' activated (thousanths per hundredth of a second)
        private short   xRadarEnergy;           // 0042     | All       | XRadarEnergy              |     0 | 32000 |        16 | Amount of energy required to have 'X-Radar' activated (thousanths per hundredth of a second)
        private short   maximumRotation;        // 0044     | All       | MaximumRotation           |       |       |        16 | Maximum rotation rate of the ship (0 = can't rotate, 400 = full rotation in 1 second)
        private short   maximumThrust;          // 0046     | All       | MaximumThrust             |       |       |        16 | Maximum thrust of ship (0 = none)
        private short   maximumSpeed;           // 0048     | All       | MaximumSpeed              |       |       |        16 | Maximum speed of ship (0 = can't move)
        private short   maximumRecharge;        // 0050     | All       | MaximumRecharge           |       |       |        16 | Maximum recharge rate, or how quickly this ship recharges its energy.
        private short   maximumEnergy;          // 0052     | All       | MaximumEnergy             |       |       |        16 | Maximum amount of energy that the ship can have.
        private short   initialRotation;        // 0054     | All       | InitialRotation           |       |       |        16 | Initial rotation rate of the ship (0 = can't rotate, 400 = full rotation in 1 second)
        private short   initialThrust;          // 0056     | All       | InitialThrust             |       |       |        16 | Initial thrust of ship (0 = none)
        private short   initialSpeed;           // 0058     | All       | InitialSpeed              |       |       |        16 | Initial speed of ship (0 = can't move)
        private short   initialRecharge;        // 0060     | All       | InitialRecharge           |       |       |        16 | Initial recharge rate, or how quickly this ship recharges its energy.
        private short   initialEnergy;          // 0062     | All       | InitialEnergy             |       |       |        16 | Initial amount of energy that the ship can have.
        private short   upgradeRotation;        // 0064     | All       | UpgradeRotation           |       |       |        16 | Amount added per 'Rotation' Prize
        private short   upgradeThrust;          // 0066     | All       | UpgradeThrust             |       |       |        16 | Amount added per 'Thruster' Prize
        private short   upgradeSpeed;           // 0068     | All       | UpgradeSpeed              |       |       |        16 | Amount added per 'Speed' Prize
        private short   upgradeRecharge;        // 0070     | All       | UpgradeRecharge           |       |       |        16 | Amount added per 'Recharge Rate' Prize
        private short   upgradeEnergy;          // 0072     | All       | UpgradeEnergy             |       |       |        16 | Amount added per 'Energy Upgrade' Prize
        private short   afterburnerEnergy;      // 0074     | All       | AfterburnerEnergy         |       |       |        16 | Amount of energy required to have 'Afterburners' activated.
        private short   bombThrust;             // 0076     | All       | BombThrust                |       |       |        16 | Amount of back-thrust you receive when firing a bomb.
        private short   burstSpeed;             // 0078     | All       | BurstSpeed                |       |       |        16 | How fast the burst shrapnel is for this ship.
        private short   turretThrustPenalty;    // 0080     | All       | TurretThrustPenalty       |       |       |        16 | Amount the ship's thrust is decreased with a turret riding
        private short   turretSpeedPenalty;     // 0082     | All       | TurretSpeedPenalty        |       |       |        16 | Amount the ship's speed is decreased with a turret riding
        private short   bulletFireDelay;        // 0084     | All       | BulletFireDelay           |       |       |        16 | delay that ship waits after a bullet is fired until another weapon may be fired (in hundredths of a second)
        private short   multiFireDelay;         // 0086     | All       | MultiFireDelay            |       |       |        16 | delay that ship waits after a multifire bullet is fired until another weapon may be fired (in hundredths of a second)
        private short   bombFireDelay;          // 0088     | All       | BombFireDelay             |       |       |        16 | delay that ship waits after a bomb is fired until another weapon may be fired (in hundredths of a second)
        private short   landmineFireDelay;      // 0090     | All       | LandmineFireDelay         |       |       |        16 | delay that ship waits after a mine is fired until another weapon may be fired (in hundredths of a second)
        private short   rocketTime;             // 0092     | All       | RocketTime                |       |       |        16 | How long a Rocket lasts (in hundredths of a second)
        private short   initialBounty;          // 0094     | All       | InitialBounty             |       |       |        16 | Number of 'Greens' given to ships when they start
        private short   damageFactor;           // 0096     | All       | DamageFactor              |       |       |        16 | How likely a the ship is to take damamage (ie. lose a prize) (0=special-case-never, 1=extremely likely, 5000=almost never)
        private short   prizeShareLimit;        // 0098     | All       | PrizeShareLimit           |       |       |        16 | Maximum bounty that ships receive Team Prizes
        private short   attachBounty;           // 0100     | All       | AttachBounty              |       |       |        16 | Bounty required by ships to attach as a turret
        private short   soccerThrowTime;        // 0102     | All       | SoccerThrowTime           |       |       |        16 | Time player has to carry soccer ball (in hundredths of a second)
        private short   soccerBallFriction;     // 0104     | All       | SoccerBallFriction        |       |       |        16 | Amount the friction on the soccer ball (how quickly it slows down -- higher numbers mean faster slowdown)
        private short   soccerBallProximity;    // 0106     | All       | SoccerBallProximity       |       |       |        16 | How close the player must be in order to pick up ball (in pixels)
        private short   soccerBallSpeed;        // 0108     | All       | SoccerBallSpeed           |       |       |        16 | Initial speed given to the ball when fired by the carrier.
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    turretLimit;            // 0110     | All       | TurretLimit               |       |       |         8 | Number of turrets allowed on a ship.
        private byte    burstShrapnel;          // 0111     | All       | BurstShrapnel             |       |       |         8 | Number of bullets released when a 'Burst' is activated
        private byte    maxMines;               // 0112     | All       | MaxMines                  |       |       |         8 | Maximum number of mines allowed in ships
        private byte    repelMax;               // 0113     | All       | RepelMax                  |       |       |         8 | Maximum number of Repels allowed in ships
        private byte    burstMax;               // 0114     | All       | BurstMax                  |       |       |         8 | Maximum number of Bursts allowed in ships
        private byte    decoyMax;               // 0115     | All       | DecoyMax                  |       |       |         8 | Maximum number of Decoys allowed in ships
        private byte    thorMax;                // 0116     | All       | ThorMax                   |       |       |         8 | Maximum number of Thor's Hammers allowed in ships
        private byte    brickMax;               // 0117     | All       | BrickMax                  |       |       |         8 | Maximum number of Bricks allowed in ships
        private byte    rocketMax;              // 0118     | All       | RocketMax                 |       |       |         8 | Maximum number of Rockets allowed in ships
        private byte    portalMax;              // 0119     | All       | PortalMax                 |       |       |         8 | Maximum number of Portals allowed in ships
        private byte    initialRepel;           // 0120     | All       | InitialRepel              |       |       |         8 | Initial number of Repels given to ships when they start
        private byte    initialBurst;           // 0121     | All       | InitialBurst              |       |       |         8 | Initial number of Bursts given to ships when they start
        private byte    initialBrick;           // 0122     | All       | InitialBrick              |       |       |         8 | Initial number of Bricks given to ships when they start
        private byte    initialRocket;          // 0123     | All       | InitialRocket             |       |       |         8 | Initial number of Rockets given to ships when they start
        private byte    initialThor;            // 0124     | All       | InitialThor               |       |       |         8 | Initial number of Thor's Hammers given to ships when they start
        private byte    initialDecoy;           // 0125     | All       | InitialDecoy              |       |       |         8 | Initial number of Decoys given to ships when they start
        private byte    initialPortal;          // 0126     | All       | InitialPortal             |       |       |         8 | Initial number of Portals given to ships when they start
        private byte    bombBounceCount;        // 0127     | All       | BombBounceCount           |       |       |         8 | Number of times a ship's bombs bounce before they explode on impact
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    shrapnelMax;            // 0128     | All       | ShrapnelMax               |     0 |    31 |         5 | Maximum amount of shrapnel released from a ship's bomb
        private byte    shrapnelRate;           // 0128     | All       | ShrapnelRate              |     0 |    31 |         5 | Amount of additional shrapnel gained by a 'Shrapnel Upgrade' prize.
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    cloakStatus;            // 0129     | All       | CloakStatus               |     0 |     2 |         2 | Whether ships are allowed to receive 'Cloak' 0=no 1=yes 2=yes/start-with
        private byte    stealthStatus;          // 0129     | All       | StealthStatus             |     0 |     2 |         2 | Whether ships are allowed to receive 'Stealth' 0=no 1=yes 2=yes/start-with
        private byte    xRadarStatus;           // 0129     | All       | XRadarStatus              |     0 |     2 |         2 | Whether ships are allowed to receive 'X-Radar' 0=no 1=yes 2=yes/start-with
        private byte    antiwarpStatus;         // 0130     | All       | AntiWarpStatus            |     0 |     2 |         2 | Whether ships are allowed to receive 'Anti-Warp' 0=no 1=yes 2=yes/start-with
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    initialGuns;            // 0130     | All       | InitialGuns               |     0 |     3 |         2 | Initial level a ship's guns fire 0=no guns
        private byte    maxGuns;                // 0130     | All       | MaxGuns                   |     0 |     3 |         2 | Maximum level a ship's guns can fire 0=no guns
        private byte    initialBombs;           // 0130     | All       | InitialBombs              |     0 |     3 |         2 | Initial level a ship's bombs fire 0=no bombs
        private byte    maxBombs;               // 0131     | All       | MaxBombs                  |     0 |     3 |         2 | Maximum level a ship's bombs can fire 0=no bombs
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private boolean doubleBarrel;           // 0131     | All       | DoubleBarrel              |     0 |     1 |         1 | Whether ships fire with double barrel bullets 0=no 1=yes
        private boolean empBomb;                // 0131     | All       | EmpBomb                   |     0 |     1 |         1 | Whether ships fire EMP bombs 0=no 1=yes
        private boolean seeMines;               // 0131     | All       | SeeMines                  |     0 |     1 |         1 | Whether ships see mines on radar 0=no 1=yes
        private byte    UNKNOWN2;               // 0131     | ?         | ?                         |       |       |         3 | ?
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category  | Name                      | Min.  | Max.  | Bitsize   | Description
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte[]  UNKNOWN3 = new byte[16];// 0132     | ?         | ?                         |       |       |       128 | ?
                                                //----------+-----------+---------------------------+-------+-------+-----------+---------------------------------------------------------------
        
        /** ShipSettings constructor */
        public ShipSettings(ByteArray data) {
            if(data.size() != 144) {
                Tools.printLog("ERROR: Invalid raw data size for ShipSettings.");
                return;
            }
            
            this.superTime              = data.readLittleEndianShort(0);
            this.UNKNOWN0               = data.readLittleEndianShort(2);
            this.shieldsTime            = data.readLittleEndianShort(4);
            this.UNKNOWN1               = data.readLittleEndianShort(6);
            this.gravity                = data.readLittleEndianShort(8);
            this.gravityTopSpeed        = data.readLittleEndianShort(10);
            this.bulletFireEnergy       = data.readLittleEndianShort(12);
            this.multiFireEnergy        = data.readLittleEndianShort(14);
            this.bombFireEnergy         = data.readLittleEndianShort(16);
            this.bombFireEnergyUpgrade  = data.readLittleEndianShort(18);
            this.mineFireEnergy         = data.readLittleEndianShort(20);
            this.mineFireEnergyUpgrade  = data.readLittleEndianShort(22);
            this.bulletSpeed            = data.readLittleEndianShort(24);
            this.bombSpeed              = data.readLittleEndianShort(26);
            
            short tmpShort = data.readLittleEndianShort(28);
            
            this.seeBombLevel           = (byte) ByteArray.getPartial(tmpShort, 1, 0);
            this.disableFastBombs       = (ByteArray.getPartial(tmpShort, 2, 2) == 1);
            this.radius                 = (byte) ByteArray.getPartial(tmpShort, 9, 3);
            this.pack0                  = (byte) ByteArray.getPartial(tmpShort, 15, 10);
            this.multiFireAngle         = data.readLittleEndianShort(30);

            this.cloakEnergy            = data.readLittleEndianShort(32);
            this.stealthEnergy          = data.readLittleEndianShort(34);
            this.antiWarpEnergy         = data.readLittleEndianShort(36);
            this.xRadarEnergy           = data.readLittleEndianShort(38);
            this.maximumRotation        = data.readLittleEndianShort(40);
            this.maximumThrust          = data.readLittleEndianShort(42);
            this.maximumSpeed           = data.readLittleEndianShort(44);
            this.maximumRecharge        = data.readLittleEndianShort(46);
            this.maximumEnergy          = data.readLittleEndianShort(48);
            this.initialRotation        = data.readLittleEndianShort(50);
            this.initialThrust          = data.readLittleEndianShort(52);
            this.initialSpeed           = data.readLittleEndianShort(54);
            this.initialRecharge        = data.readLittleEndianShort(56);
            this.initialEnergy          = data.readLittleEndianShort(58);
            this.upgradeRotation        = data.readLittleEndianShort(60);
            this.upgradeThrust          = data.readLittleEndianShort(62);
            this.upgradeSpeed           = data.readLittleEndianShort(64);
            this.upgradeRecharge        = data.readLittleEndianShort(66);
            this.upgradeEnergy          = data.readLittleEndianShort(68);
            this.afterburnerEnergy      = data.readLittleEndianShort(70);
            this.bombThrust             = data.readLittleEndianShort(72);
            this.burstSpeed             = data.readLittleEndianShort(74);
            this.turretThrustPenalty    = data.readLittleEndianShort(76);
            this.turretSpeedPenalty     = data.readLittleEndianShort(78);
            this.bulletFireDelay        = data.readLittleEndianShort(80);
            this.multiFireDelay         = data.readLittleEndianShort(82);
            this.bombFireDelay          = data.readLittleEndianShort(84);
            this.landmineFireDelay      = data.readLittleEndianShort(86);
            this.rocketTime             = data.readLittleEndianShort(88);
            this.initialBounty          = data.readLittleEndianShort(90);
            this.damageFactor           = data.readLittleEndianShort(92);
            this.prizeShareLimit        = data.readLittleEndianShort(94);
            this.attachBounty           = data.readLittleEndianShort(96);
            this.soccerThrowTime        = data.readLittleEndianShort(98);
            this.soccerBallFriction     = data.readLittleEndianShort(100);
            this.soccerBallProximity    = data.readLittleEndianShort(102);
            this.soccerBallSpeed        = data.readLittleEndianShort(104);
            
            this.turretLimit            = data.readByte(106);
            this.burstShrapnel          = data.readByte(107);
            this.maxMines               = data.readByte(108);
            this.repelMax               = data.readByte(109);
            this.burstMax               = data.readByte(110);
            this.decoyMax               = data.readByte(111);
            this.thorMax                = data.readByte(112);
            this.brickMax               = data.readByte(113);
            this.rocketMax              = data.readByte(114);
            this.portalMax              = data.readByte(115);
            this.initialRepel           = data.readByte(116);
            this.initialBurst           = data.readByte(117);
            this.initialBrick           = data.readByte(118);
            this.initialRocket          = data.readByte(119);
            this.initialThor            = data.readByte(120);
            this.initialDecoy           = data.readByte(121);
            this.initialPortal          = data.readByte(122);
            this.bombBounceCount        = data.readByte(123);
            
            int tmpInt = data.readLittleEndianInt(124);
            this.shrapnelMax            = (byte) ByteArray.getPartial(tmpInt, 4, 0);
            this.shrapnelRate           = (byte) ByteArray.getPartial(tmpInt, 9, 5);
            
            this.cloakStatus            = (byte) ByteArray.getPartial(tmpInt, 11, 10);
            this.stealthStatus          = (byte) ByteArray.getPartial(tmpInt, 13, 12);
            this.xRadarStatus           = (byte) ByteArray.getPartial(tmpInt, 15, 14);
            this.antiwarpStatus         = (byte) ByteArray.getPartial(tmpInt, 17, 16);
            
            this.initialGuns            = (byte) ByteArray.getPartial(tmpInt, 19, 18);
            this.maxGuns                = (byte) ByteArray.getPartial(tmpInt, 21, 20);
            this.initialBombs           = (byte) ByteArray.getPartial(tmpInt, 23, 22);
            this.maxBombs               = (byte) ByteArray.getPartial(tmpInt, 25, 24);
            
            this.doubleBarrel           = (ByteArray.getPartial(tmpInt, 26, 26) == 1);
            this.empBomb                = (ByteArray.getPartial(tmpInt, 27, 27) == 1);
            this.seeMines               = (ByteArray.getPartial(tmpInt, 28, 28) == 1);
            this.UNKNOWN2               = (byte) ByteArray.getPartial(tmpInt, 31, 29);
            
            for(int i = 0; i < 16; i++)
                this.UNKNOWN3[i]        = data.readByte(128 + i);

        }

        /*
         * Automatically generated getters.
         */
        /**
         * <b>[All] SuperTime</b><br>
         * How long Superpower lasts for this ship. The actual time is random up to this value.
         * @return Super time in centiseconds.
         */
        public int getSuperTime() {
            return (int) (superTime & MASK_UINT16);
        }

        /**
         * Section of undocumented data, possibly a salt for actual super time.
         * @return UNKNOWN0
         */
        public int getUNKNOWN0() {
            return (int) (UNKNOWN0 & MASK_UINT16);
        }

        /**
         * <b>[All] ShieldsTime</b><br>
         * How long Shields last for this ship.
         * @return Shields time in centiseconds.
         */
        public int getShieldsTime() {
            return (int) (shieldsTime & MASK_UINT16);
        }

        /**
         * Section of undocumented data, possibly a salt for actual shieds time.
         * @return UNKNOWN1
         */
        public int getUNKNOWN1() {
            return (int) (UNKNOWN1 & MASK_UINT16);
        }

        /**
         * <b>[All] Gravity</b><br>
         * Modifies radius over which a wormhole's gravity has an effect.<p>
         * <i>Formula:</i> EffectRadius = 1.325 * Gravity ^ 0.507.<br>
         * That is, a wormhole will have an effect over 31 tiles if this is set to 500.<p>
         * The setting for Warbird also controls how far out bombs/mines are effected by wormholes.
         * @return Gravity as factor.
         */
        public int getGravity() {
            return (int) (gravity & MASK_UINT16);
        }

        /**
         * <b>[All] GravityTopSpeed</b><br>
         * How much this ship's maximum speed is increased when under the influence of a wormhole's gravity.
         * Note that this does not replace the previous speed.
         * @return Gravity top speed in pixels per 10 seconds.
         */
        public int getGravityTopSpeed() {
            return (int) (gravityTopSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] BulletFireEnergy</b><br>
         * Base energy it takes a ship to fire a bullet. Formula: EnergyUsed = GunLevel * BulletFireEnergy. 
         * So this setting is for an L1 bullet, and is doubled for L2, and so on.
         * @return Bullet fire energy in energy units.
         */
        public int getBulletFireEnergy() {
            return (int) (bulletFireEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] MultiFireEnergy</b><br>
         * Energy it takes a ship to fire a set of multifire bullets.
         * <i>Formula:</i> EnergyUsed = GunLevel * MultiFireEnergy.
         * @return Multifire energy in energy units.
         */
        public int getMultiFireEnergy() {
            return (int) (multiFireEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] BombFireEnergy</b><br>
         * Amount of energy it takes a ship to fire a bomb.
         * @return Bomb fire energy in energy units.
         */
        public int getBombFireEnergy() {
            return (int) (bombFireEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] BombFireEnergyUpgrade</b><br>
         * Extra energy it takes a ship to fire an upgraded bomb.
         * <i>Formula:</i> EnergyUsed = BombFireEnergy + (BombLevel - 1) * BombFireEnergyUpgrade.
         * @return Bomb fire energy upgrade in energy units.
         */
        public int getBombFireEnergyUpgrade() {
            return (int) (bombFireEnergyUpgrade & MASK_UINT16);
        }

        /**
         * <b>[All] LandmineFireEnergy</b><br>
         * Energy it takes this ship to place an L1 mine
         * @return Mine fire energy in energy units.
         */
        public int getMineFireEnergy() {
            return (int) (mineFireEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] LandmineFireEnergyUpgrade</b><br>
         * Extra energy it takes to place an upgraded landmine.
         * <i>Formula:</i> EnergyUsed = LandmineFireEnergy + (BombLevel - 1) * LandmineFireEnergyUpgrade.
         * @return Mine fire energy upgrade in energy units.
         */
        public int getMineFireEnergyUpgrade() {
            return (int) (mineFireEnergyUpgrade & MASK_UINT16);
        }

        /**
         * <b>[All] BulletSpeed</b><br>
         * How fast bullets travel. Can be negative, which results in shooting from the rear.
         * @return Bullet speed in pixels per 10 seconds.
         */
        public short getBulletSpeed() {
            return bulletSpeed;
        }

        /**
         * <b>[All] BombSpeed</b><br>
         * How fast bombs fired by this ship travel. Can be negative, which results in shooting from the rear.
         * @return Bomb speed in pixels per 10 seconds.
         */
        public short getBombSpeed() {
            return bombSpeed;
        }

        /**
         * <b>[All] SeeBombLevel</b><br>
         * Lowest level of bombs this ship can see on radar. Special Value: 0 = no bombs on radar. (Continuum .36+)
         * Note: 4 means a player can only see L4 bombs. 2 means L2, L3 and L4 bombs are visible on radar.
         * @return See bomb level with a value ranging from 0 to 4.
         */
        public byte getSeeBombLevel() {
            return seeBombLevel;
        }

        /**
         * <b>[All] DisableFastShooting</b><br>
         * Whether firing bullets, bombs, or thors is disabled after using afterburners. (Continuum .36+)
         * @return True when fast firing is disabled, false otherwise.
         */
        public boolean isDisableFastBombs() {
            return disableFastBombs;
        }

        /**
         * <b>[All] Radius</b><br>
         * The ship's radius from center to outside. Special Value: 0 = 14 pixels. (Continuum .37+)
         * Note: This function will return the default value if radius is equal to 0.
         * Use {@link #getRawRadius()} instead if you want it to not do this.
         * @return Radius in pixels. If the radius is 0, then 14 will be returned instead.
         */
        public short getRadius() {
            if(radius == 0)
                return 14;
            return (short) (radius & MASK_UINT8);
        }
        
        /**
         * <b>[All] Radius</b><br>
         * The ship's radius from center to outside. Special Value: 0 = 14 pixels. (Continuum .37+)
         * Note: This function will always return the stored radius. If you want the return value to
         * be converted to the default, when its value is 0, then use {@link #getRadius()} instead.
         * @return Radius in pixels.
         */
        public short getRawRadius() {
            return (short) (radius & MASK_UINT8);
        }

        /**
         * Section of undocumented data. Probably unused.
         * @return pack0.
         */
        public byte getPack0() {
            return pack0;
        }

        /**
         * <b>[All] MultiFireAngle</b><br>
         * Angle spread between multi-fire bullets and standard forward-firing bullets.
         * Rotation Points / 1000. (Note: One rotation point equals exactly 9 degrees.)
         * <p>
         * Some research might be needed. Initial analysis made it look that you actually
         * need to divide MultiFireAngle by 1000 to get the Rotation Points. So, 1000 means
         * a 9 degrees rotation, or one Rotation Point, 40000 is a full 360 degrees or 2PI rad circle (40 Rotation Points).
         * <p>
         * The multifire angle is measured from the normal, and mirrored on both sides. Whenever the
         * speed of a projectile is negative, this is done in regard to the normal sticking out of the backside
         * of the ship. (I.e. the original mirrored in the relative x-axis.) Also, whether or not a ship has a
         * double barrel, the amount of multifire angles/shots is still fixed at two.
         * @return Multifire angle. See above for details.
         */
        public int getMultiFireAngle() {
            return (int) (multiFireAngle & MASK_UINT16);
        }

        /**
         * <b>[All] CloakEnergy</b><br>
         * Amount of energy required to have Cloak activated.
         * @return Cloak energy in energy units per 10 seconds.
         */
        public int getCloakEnergy() {
            return (int) (cloakEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] StealthEnergy</b><br>
         * Amount of energy required to have Stealth activated.
         * @return Stealth energy in energy units per 10 seconds.
         */
        public int getStealthEnergy() {
            return (int) (stealthEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] AntiWarpEnergy</b><br>
         * Amount of energy required to have Anti-Warp activated.
         * @return Antiwarp energy in energy units per 10 seconds.
         */
        public int getAntiWarpEnergy() {
            return (int) (antiWarpEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] XRadarEnergy</b><br>
         * Amount of energy required to have X-Radar activated.
         * @return X-radar energy in energy units per 10 seconds.
         */
        public int getxRadarEnergy() {
            return (int) (xRadarEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] MaximumRotation</b><br>
         * Maximum (with upgrades) rotation rate of the ship.
         * <i>Formula:</i> 90 degrees/(seconds / 100) (That is, 400 for a full rotation in 1s).
         * @return Maximum rotation in Rotation Points per 10 seconds.
         */
        public int getMaximumRotation() {
            return (int) (maximumRotation & MASK_UINT16);
        }

        /**
         * <b>[All] MaximumThrust</b><br>
         * Maximum (with upgrades) thrust of ship.
         * @return Maximum thrust in speed per centisecond. This results in a bit more unclear math. For example, take
         * a thrust of 10, applied for 1 second. This increases the speed in continuum units by 1000. (So, 1000 pixels / 10 seconds)<br>
         * In other words, if thrust is x, then the speed increase is:
         * <li>x / 100m pixels per second, per second of thrust;
         * <li>x / 10m pixels per 10 seconds, per second of thrust;
         * <li>x / 1m ( = x * 1000) pixels per 10 seconds, for 10 seconds of thrust;
         * <li>x pixels per 10 seconds, per centisecond of thrust;
         * <li>x / 10 pixels per second, per centisecond of thrust;
         * <li>x / 100 pixels per millisecond, per second of thrust;
         * <li>x / 10k pixels per millisecond, per centisecond of thrust;
         * <li>x / 100k pixels per millisecond, per millisecond of thrust.
         */
        public int getMaximumThrust() {
            return (int) (maximumThrust & MASK_UINT16);
        }

        /**
         * <b>[All] MaximumSpeed</b><br>
         * Maximum (with upgrades) speed of ship.
         * @return Maximum speed in pixels per 10 seconds. (1000 means 100 pixels per second.) 
         */
        public int getMaximumSpeed() {
            return (int) (maximumSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] MaximumRecharge</b><br>
         * Maximum (with upgrades) recharge rate, or how quickly this ship recharges its energy.
         * @return Maximum recharge in energy per 10 seconds. (1000 means 100 energy per second.)
         */
        public int getMaximumRecharge() {
            return (int) (maximumRecharge & MASK_UINT16);
        }

        /**
         * <b>[All] MaximumEnergy</b><br>
         * Maximum (with upgrades) amount of energy that the ship can have.
         * @return Maximum energy in energy units.
         */
        public int getMaximumEnergy() {
            return (int) (maximumEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] InitialRotation</b><br>
         * Initial rotation rate of the ship.
         * <i>Formula:</i> 90 degrees/(seconds / 100) (That is, 400 for a full rotation in 1s).
         * @return Initial rotation in Rotation Points per 10 seconds.
         */
        public int getInitialRotation() {
            return (int) (initialRotation & MASK_UINT16);
        }

        /**
         * <b>[All] InitialThrust</b><br>
         * Initial thrust of ship.
         * @return Initial thrust in speed per centisecond. This results in a bit more unclear math. For example, take
         * a thrust of 10, applied for 1 second. This increases the speed in continuum units by 1000. (So, 1000 pixels / 10 seconds)<br>
         * In other words, if thrust is x, then the speed increase is:
         * <li>x / 100m pixels per second, per second of thrust;
         * <li>x / 10m pixels per 10 seconds, per second of thrust;
         * <li>x / 1m ( = x * 1000) pixels per 10 seconds, for 10 seconds of thrust;
         * <li>x pixels per 10 seconds, per centisecond of thrust;
         * <li>x / 10 pixels per second, per centisecond of thrust;
         * <li>x / 100 pixels per millisecond, per second of thrust;
         * <li>x / 10k pixels per millisecond, per centisecond of thrust;
         * <li>x / 100k pixels per millisecond, per millisecond of thrust.
         */
        public int getInitialThrust() {
            return (int) (initialThrust & MASK_UINT16);
        }

        /**
         * <b>[All] InitialSpeed</b><br>
         * Initial speed of ship. Note that 0 will prevent the ship from moving without afterburners or a speed upgrade.
         * @return Initial speed in pixels per 10 seconds. (1000 means 100 pixels per second.) 
         */
        public int getInitialSpeed() {
            return (int) (initialSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] InitialRecharge</b><br>
         * Initial recharge rate.
         * @return Initial recharge in energy per 10 seconds. (1000 means 100 energy per second.)
         */
        public int getInitialRecharge() {
            return (int) (initialRecharge & MASK_UINT16);
        }

        /**
         * <b>[All] InitialEnergy</b><br>
         * Initial amount of energy that the ship can have.
         * @return Initial energy in energy units.
         */
        public int getInitialEnergy() {
            return (int) (initialEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] UpgradeRotation</b><br>
         * Amount added per Rotation Upgrade Prize.
         * <i>Formula:</i> 90 degrees/(seconds / 100) (That is, 400 for a full rotation in 1s).
         * @return Upgrade rotation in Rotation Points per 10 seconds.
         */
        public int getUpgradeRotation() {
            return (int) (upgradeRotation & MASK_UINT16);
        }

        /**
         * <b>[All] UpgradeThrust</b><br>
         * Amount added per Thruster Upgrade Prize.
         * @return Upgrade thrust in speed per centisecond. This results in a bit more unclear math. For example, take
         * a thrust of 10, applied for 1 second. This increases the speed in continuum units by 1000. (So, 1000 pixels / 10 seconds)<br>
         * In other words, if thrust is x, then the speed increase is:
         * <li>x / 100m pixels per second, per second of thrust;
         * <li>x / 10m pixels per 10 seconds, per second of thrust;
         * <li>x / 1m ( = x * 1000) pixels per 10 seconds, for 10 seconds of thrust;
         * <li>x pixels per 10 seconds, per centisecond of thrust;
         * <li>x / 10 pixels per second, per centisecond of thrust;
         * <li>x / 100 pixels per millisecond, per second of thrust;
         * <li>x / 10k pixels per millisecond, per centisecond of thrust;
         * <li>x / 100k pixels per millisecond, per millisecond of thrust.
         */
        public int getUpgradeThrust() {
            return (int) (upgradeThrust & MASK_UINT16);
        }

        /**
         * <b>[All] UpgradeSpeed</b><br>
         * Amount added per Speed Upgrade Prize.
         * @return Upgrade speed in pixels per 10 seconds. (1000 means 100 pixels per second.) 
         */
        public int getUpgradeSpeed() {
            return (int) (upgradeSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] UpgradeRecharge</b><br>
         * Amount added per Recharge Upgrade Prize.
         * @return Upgrade recharge in energy per 10 seconds. (1000 means 100 energy per second.)
         */
        public int getUpgradeRecharge() {
            return (int) (upgradeRecharge & MASK_UINT16);
        }

        /**
         * <b>[All] UpgradeEnergy</b><br>
         * Amount added per Energy Upgrade Prize.
         * @return Upgrade energy in energy units.
         */
        public int getUpgradeEnergy() {
            return (int) (upgradeEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] AfterburnerEnergy</b><br>
         * Energy required to have 'Afterburners' activated.
         * @return Afterburner energy in energy units. (Might be energy units per 10 seconds.)
         */
        public int getAfterburnerEnergy() {
            return (int) (afterburnerEnergy & MASK_UINT16);
        }

        /**
         * <b>[All] BombThrust</b><br>
         * Amount of back-thrust (recoil) this ship receives when firing a bomb.
         * @return Bomb thrust in an unknown unit. (Possibly in pixels / (10s)^2)
         */
        public int getBombThrust() {
            return (int) (bombThrust & MASK_UINT16);
        }

        /**
         * <b>[All] BurstSpeed</b><br>
         * How fast the burst "shrapnel" move for this ship.
         * @return Burst speed in pixels per 10 seconds.
         */
        public int getBurstSpeed() {
            return (int) (burstSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] TurretThrustPenalty</b><br>
         * This ship's thrust is decreased this much when a turret it riding.
         * @return Turret thrust penalty in an unknown unit. (Possibly in pixels / (10s)^2)
         */
        public int getTurretThrustPenalty() {
            return (int) (turretThrustPenalty & MASK_UINT16);
        }

        /**
         * <b>[All] TurretSpeedPenalty</b><br>
         * This ship's speed is decreased this much when a turret it riding.
         * @return Turret speed penalty in pixels per 10 seconds.
         */
        public int getTurretSpeedPenalty() {
            return (int) (turretSpeedPenalty & MASK_UINT16);
        }

        /**
         * <b>[All] BulletFireDelay</b><br>
         * Delay before a ship can fire another weapon after it fires a bullet.
         * @return Bullet fire delay in centiseconds.
         */
        public int getBulletFireDelay() {
            return (int) (bulletFireDelay & MASK_UINT16);
        }

        /**
         * <b>[All] MultiFireDelay</b><br>
         * Delay before a ship can fire another weapon after it fires a set of multifire bullets.
         * @return Multifire delay in centiseconds.
         */
        public int getMultiFireDelay() {
            return (int) (multiFireDelay & MASK_UINT16);
        }

        /**
         * <b>[All] BombFireDelay</b><br>
         * Delay before this ship can fire another weapon after it fires a bomb.
         * @return Bomb fire delay in centiseconds.
         */
        public int getBombFireDelay() {
            return (int) (bombFireDelay & MASK_UINT16);
        }

        /**
         * <b>[All] LandmineFireDelay</b><br>
         * Delay before a ship can fire another weapon after it drops a landmine.
         * @return Landmine fire delay in centiseconds.
         */
        public int getLandmineFireDelay() {
            return (int) (landmineFireDelay & MASK_UINT16);
        }

        /**
         * <b>[All] RocketTime</b><br>
         * How long a Rocket lasts.
         * @return Rocket time in centiseconds.
         */
        public int getRocketTime() {
            return (int) (rocketTime & MASK_UINT16);
        }

        /**
         * <b>[All] InitialBounty</b><br>
         * Number of prizes given to this ship when it spawns.
         * @return Initial bounty.
         */
        public int getInitialBounty() {
            return (int) (initialBounty & MASK_UINT16);
        }

        /**
         * <b>[All] DamageFactor</b><br>
         * How likely a the ship is to lose a prize when damaged.
         * (1 is extremely likely, 5000 is almost never.) Special Value: 0 = never.
         * @return Damage factor between 1 and 5000. Special case: 0, meaning it will never lose a bounty point.
         */
        public int getDamageFactor() {
            return (int) (damageFactor & MASK_UINT16);
        }

        /**
         * <b>[All] PrizeShareLimit</b><br>
         * Maximum bounty that this ship can receive by Team Prizes.
         * @return Prize share limit in points.
         */
        public int getPrizeShareLimit() {
            return (int) (prizeShareLimit & MASK_UINT16);
        }

        /**
         * <b>[All] AttachBounty</b><br>
         * Bounty required by ships to attach to another ship to form a turret.
         * @return Attach bounty in points.
         */
        public int getAttachBounty() {
            return (int) (attachBounty & MASK_UINT16);
        }

        /**
         * <b>[All] SoccerThrowTime</b><br>
         * Time player has to carry soccer ball before it automatically fires (out the back of the ship).
         * If this is 0 or 32768 or greater then carry time is unlimited and no timer is shown on the players screen.
         * @return Soccer throw time in centiseconds.
         */
        public int getSoccerThrowTime() {
            return (int) (soccerThrowTime & MASK_UINT16);
        }

        /**
         * <b>[All] SoccerBallFriction</b><br>
         * Amount of friction on the soccer ball when it is not carried by a ship.
         * @return Soccer ball friction in an unknown unit. Could possibly be a deceleration instead of force, i.e. pixels / (10s)^2.
         */
        public int getSoccerBallFriction() {
            return (int) (soccerBallFriction & MASK_UINT16);
        }

        /**
         * <b>[All] SoccerBallProximity</b><br>
         * How close the ship must be in order to pick up a ball. Set this to 0 to disallow the ship from picking up a ball.
         * @return Soccer ball proximity in pixels.
         */
        public int getSoccerBallProximity() {
            return (int) (soccerBallProximity & MASK_UINT16);
        }

        /**
         * <b>[All] SoccerBallSpeed</b><br>
         * Initial speed given to the ball when fired by this ship.
         * @return Soccer ball speed in pixels per 10 seconds.
         */
        public int getSoccerBallSpeed() {
            return (int) (soccerBallSpeed & MASK_UINT16);
        }

        /**
         * <b>[All] TurretLimit</b><br>
         * Number of turrets allowed on this ship. If you don't want ships to attach, set TurretLimit to 0.
         * @return Turret limit.
         */
        public short getTurretLimit() {
            return (short) (turretLimit & MASK_UINT8);
        }

        /**
         * <b>[All] BurstShrapnel</b><br>
         * Burst shrapnel released when this ship fires a Burst.
         * @return Burst shrapnel amount.
         */
        public short getBurstShrapnel() {
            return (short) (burstShrapnel & MASK_UINT8);
        }

        /**
         * <b>[All] MaxMines</b><br>
         * Maximum number of mines this ship can place.
         * @return Max mines.
         */
        public short getMaxMines() {
            return (short) (maxMines & MASK_UINT8);
        }

        /**
         * <b>[All] RepelMax</b><br>
         * Maximum Repels a ship can store. Any Repel prizes after this will be ignored.
         * @return Repel max amount.
         */
        public short getRepelMax() {
            return (short) (repelMax & MASK_UINT8);
        }

        /**
         * <b>[All] BurstMax</b><br>
         * Maximum Bursts a ship can store. Any Repel prizes after this will be ignored.
         * @return Burst max amount.
         */
        public short getBurstMax() {
            return (short) (burstMax & MASK_UINT8);
        }

        /**
         * <b>[All] DecoyMax</b><br>
         * Maximum Decoys a ship can store. Any Repel prizes after this will be ignored.
         * @return Decoy max amount.
         */
        public short getDecoyMax() {
            return (short) (decoyMax & MASK_UINT8);
        }

        /**
         * <b>[All] ThorMax</b><br>
         * Maximum Thor's Hammers a ship can store. Any Repel prizes after this will be ignored.
         * @return Thor max amount.
         */
        public short getThorMax() {
            return (short) (thorMax & MASK_UINT8);
        }

        /**
         * <b>[All] BrickMax</b><br>
         * Maximum Bricks a ship can store. Any Repel prizes after this will be ignored.
         * @return Brick max amount.
         */
        public short getBrickMax() {
            return (short) (brickMax & MASK_UINT8);
        }

        /**
         * <b>[All] RocketMax</b><br>
         * Maximum Rockets a ship can store. Any Repel prizes after this will be ignored.
         * @return Rocket max amount.
         */
        public short getRocketMax() {
            return (short) (rocketMax & MASK_UINT8);
        }

        /**
         * <b>[All] PortalMax</b><br>
         * Maximum Portals a ship can store. Any Repel prizes after this will be ignored.
         * @return Portal max amount.
         */
        public short getPortalMax() {
            return (short) (portalMax & MASK_UINT8);
        }

        /**
         * <b>[All] InitialRepel</b><br>
         * Repels given to ships when they spawn.
         * @return Initial repel amount.
         */
        public short getInitialRepel() {
            return (short) (initialRepel & MASK_UINT8);
        }

        /**
         * <b>[All] InitialBurst</b><br>
         * Bursts given to ships when they spawn.
         * @return Initial burst amount.
         */
        public short getInitialBurst() {
            return (short) (initialBurst & MASK_UINT8);
        }

        /**
         * <b>[All] InitialBrick</b><br>
         * Bricks given to ships when they spawn.
         * @return Initial brick amount.
         */
        public short getInitialBrick() {
            return (short) (initialBrick & MASK_UINT8);
        }

        /**
         * <b>[All] InitialRocket</b><br>
         * Rockets given to ships when they spawn.
         * @return Initial rocket amount.
         */
        public short getInitialRocket() {
            return (short) (initialRocket & MASK_UINT8);
        }

        /**
         * <b>[All] InitialThor</b><br>
         * Thor's Hammers given to ships when they spawn.
         * @return Initial thor amount.
         */
        public short getInitialThor() {
            return (short) (initialThor & MASK_UINT8);
        }

        /**
         * <b>[All] InitialDecoy</b><br>
         * Decoys given to ships when they spawn.
         * @return Initial decoy amount.
         */
        public short getInitialDecoy() {
            return (short) (initialDecoy & MASK_UINT8);
        }

        /**
         * <b>[All] InitialPortal</b><br>
         * Portals given to ships when they spawn.
         * @return Initial portal amount.
         */
        public short getInitialPortal() {
            return (short) (initialPortal & MASK_UINT8);
        }

        /**
         * <b>[All] BombBounceCount</b><br>
         * Times a bomb fired by this ship can bounce before it explodes on impact
         * @return Bomb bounce count amount.
         */
        public short getBombBounceCount() {
            return (short) (bombBounceCount & MASK_UINT8);
        }

        /**
         * <b>[All] ShrapnelMax</b><br>
         * Maximum shrapnel pieces released from this ship's bombs.
         * @return Shrapnel max amount.
         */
        public byte getShrapnelMax() {
            return shrapnelMax;
        }

        /**
         * <b>[All] ShrapnelRate</b><br>
         * Additional shrapnel pieces gained by a 'Shrapnel Upgrade' prize.
         * @return Shrapnel rate amount.
         */
        public byte getShrapnelRate() {
            return shrapnelRate;
        }

        /**
         * <b>[All] CloakStatus</b><br>
         * Whether ships can to receive Cloak.
         * <li>0: no
         * <li>1: yes
         * <li>2: yes & starts with prize
         * @return One of the above values.
         */
        public byte getCloakStatus() {
            return cloakStatus;
        }

        /**
         * <b>[All] StealthStatus</b><br>
         * Whether ships can to receive Stealth.
         * <li>0: no
         * <li>1: yes
         * <li>2: yes & starts with prize
         * @return One of the above values.
         */
        public byte getStealthStatus() {
            return stealthStatus;
        }

        /**
         * <b>[All] XRadarStatus</b><br>
         * Whether ships can to receive Stealth.
         * <li>0: no
         * <li>1: yes
         * <li>2: yes & starts with prize
         * @return One of the above values.
         */
        public byte getxRadarStatus() {
            return xRadarStatus;
        }

        /**
         * <b>[All] AntiWarpStatus</b><br>
         * Whether ships can receive AntiWarp.
         * <li>0: no
         * <li>1: yes
         * <li>2: yes & starts with prize
         * @return One of the above values.
         */
        public byte getAntiwarpStatus() {
            return antiwarpStatus;
        }

        /**
         * <b>[All] InitialGuns</b><br>
         * Gun level given to ships at respawn. Note that a ship cannot start with L4 guns.
         * @return Initial guns level, 0 being none.
         */
        public byte getInitialGuns() {
            return initialGuns;
        }

        /**
         * <b>[All] MaxGuns</b><br>
         * Maximum gun level a ship can have. Note that a ship cannot have L4 guns without a flag upgrade.
         * @return Max guns level, 0 being none.
         */
        public byte getMaxGuns() {
            return maxGuns;
        }

        /**
         * <b>[All] InitialBombs</b><br>
         * Bomb level given to ships at respawn. Note that a ship cannot start with L4 bombs.
         * @return Initial bombs level, 0 being none.
         */
        public byte getInitialBombs() {
            return initialBombs;
        }

        /**
         * <b>[All] MaxBombs</b><br>
         * Maximum bomb level a ship can have. Note that a ship cannot have L4 bombs without a flag upgrade.
         * @return Max bombs level, 0 being none.
         */
        public byte getMaxBombs() {
            return maxBombs;
        }

        /**
         * <b>[All] DoubleBarrel</b><br>
         * Whether ships fire double-barrel bullets.
         * @return True if the ship fires two bullets, false if only one. (Not taking multifire into account.)
         */
        public boolean isDoubleBarrel() {
            return doubleBarrel;
        }

        /**
         * <b>[All] EmpBomb</b><br>
         * Whether this ship fires EMP bombs.
         * @return True if this ship fires EMP bombs, false otherwise.
         */
        public boolean isEmpBomb() {
            return empBomb;
        }

        /**
         * <b>[All] SeeMines</b><br>
         * Whether ships see mines on radar.
         * @return True if the ship is allowed to see mines on the radar, false otherwise.
         */
        public boolean isSeeMines() {
            return seeMines;
        }

        /**
         * Section of undocumented data.
         * @return UNKNOWN2
         */
        public byte getUNKNOWN2() {
            return UNKNOWN2;
        }

        /**
         * Section of undocumented data.
         * @return UNKNOWN3
         */
        public byte[] getUNKNOWN3() {
            return UNKNOWN3;
        }
        
        /**
         * Debug method. Prints out all getter values. 
         * <p>
         * This is/was mainly used to verify that every getter is returning
         * the correct signed/unsigned value. Could prove useful in the future
         * whenever a discrepancy is found between what the bot thinks is an arena setting
         * and what actually is the arena setting.
         * @return List of getter names and their values.
         */
        public String toString() {
            StringBuilder result = new StringBuilder();
            String newLine = System.getProperty("line.separator");
            
            Class<?> objClass = this.getClass();

            result.append(objClass.getName() + " {" + newLine);
            // Get the public methods associated with this class.
            Method[] methods = objClass.getMethods();
            for (Method method:methods)
            {
                if(method.getName().startsWith("getClass")) {
                    continue;
                } else if(method.getName().startsWith("get") || method.getName().startsWith("is")) {
                    try {
                        result.append(method.getName() + ": " + method.invoke(this, new Object[] {}) + newLine);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                
            }
            
            result.append("}" + newLine);
            
            return result.toString();
        }
    }
    
    /**
     * This class logs all the prize related settings, obtained from the arena settings.
     * <p>
     * These settings determine the random green prize weighting. A higher number means a greater 
     * chance of a green containing the prize, a lower a lesser chance. Warning: If you set these 
     * all to 0, subgame will crash when it tries to generate a prize.
     * <p>
     * Due to this being a port, I've tried to give credit where credit is due, by trying to keep most of the original
     * comments in tact. Full credit goes to the researchers for and creators of the MervBot.
     * @author Trancid
     *
     */
    public class PrizeSettings {
        // 28 bytes wide
                                                //----------+---------------+-------------------+-------+-------+-----------+---------------------------------------------------------------
                                                // Offset   | Category      | Name              | Min.  | Max.  | Bitsize   | Description
                                                //----------+---------------+-------------------+-------+-------+-----------+---------------------------------------------------------------
        private byte    recharge;               // 1400     | PrizeWeight   | QuickCharge       |       |       |         8 | Likelyhood of 'Recharge' prize appearing
        private byte    energy;                 // 1401     | PrizeWeight   | Energy            |       |       |         8 | Likelyhood of 'Energy Upgrade' prize appearing
        private byte    rotation;               // 1402     | PrizeWeight   | Rotation          |       |       |         8 | Likelyhood of 'Rotation' prize appearing
        private byte    stealth;                // 1403     | PrizeWeight   | Stealth           |       |       |         8 | Likelyhood of 'Stealth' prize appearing
        private byte    cloak;                  // 1404     | PrizeWeight   | Cloak             |       |       |         8 | Likelyhood of 'Cloak' prize appearing
        private byte    xRadar;                 // 1405     | PrizeWeight   | XRadar            |       |       |         8 | Likelyhood of 'XRadar' prize appearing
        private byte    warp;                   // 1406     | PrizeWeight   | Warp              |       |       |         8 | Likelyhood of 'Warp' prize appearing
        private byte    gun;                    // 1407     | PrizeWeight   | Gun               |       |       |         8 | Likelyhood of 'Gun Upgrade' prize appearing
        private byte    bomb;                   // 1408     | PrizeWeight   | Bomb              |       |       |         8 | Likelyhood of 'Bomb Upgrade' prize appearing
        private byte    bouncingBullets;        // 1409     | PrizeWeight   | BouncingBullets   |       |       |         8 | Likelyhood of 'Bouncing Bullets' prize appearing
        private byte    thruster;               // 1410     | PrizeWeight   | Thruster          |       |       |         8 | Likelyhood of 'Thruster' prize appearing
        private byte    topSpeed;               // 1411     | PrizeWeight   | TopSpeed          |       |       |         8 | Likelyhood of 'Speed' prize appearing
        private byte    quickCharge;            // 1412     | PrizeWeight   | Recharge          |       |       |         8 | Likelyhood of 'Full Charge' prize appearing (NOTE! This is FULL CHARGE, not Recharge!! stupid vie)
        private byte    glue;                   // 1413     | PrizeWeight   | Glue              |       |       |         8 | Likelyhood of 'Engine Shutdown' prize appearing
        private byte    multiFire;              // 1414     | PrizeWeight   | MultiFire         |       |       |         8 | Likelyhood of 'MultiFire' prize appearing
        private byte    proximity;              // 1415     | PrizeWeight   | Proximity         |       |       |         8 | Likelyhood of 'Proximity Bomb' prize appearing
        private byte    allWeapons;             // 1416     | PrizeWeight   | AllWeapons        |       |       |         8 | Likelyhood of 'Super!' prize appearing
        private byte    shields;                // 1417     | PrizeWeight   | Shields           |       |       |         8 | Likelyhood of 'Shields' prize appearing
        private byte    shrapnel;               // 1418     | PrizeWeight   | Shrapnel          |       |       |         8 | Likelyhood of 'Shrapnel Upgrade' prize appearing
        private byte    antiWarp;               // 1419     | PrizeWeight   | AntiWarp          |       |       |         8 | Likelyhood of 'AntiWarp' prize appearing
        private byte    repel;                  // 1420     | PrizeWeight   | Repel             |       |       |         8 | Likelyhood of 'Repel' prize appearing
        private byte    burst;                  // 1421     | PrizeWeight   | Burst             |       |       |         8 | Likelyhood of 'Burst' prize appearing
        private byte    decoy;                  // 1422     | PrizeWeight   | Decoy             |       |       |         8 | Likelyhood of 'Decoy' prize appearing
        private byte    thor;                   // 1423     | PrizeWeight   | Thor              |       |       |         8 | Likelyhood of 'Thor' prize appearing
        private byte    multiPrize;             // 1424     | PrizeWeight   | MultiPrize        |       |       |         8 | Likelyhood of 'Multi-Prize' prize appearing
        private byte    brick;                  // 1425     | PrizeWeight   | Brick             |       |       |         8 | Likelyhood of 'Brick' prize appearing
        private byte    rocket;                 // 1426     | PrizeWeight   | Rocket            |       |       |         8 | Likelyhood of 'Rocket' prize appearing
        private byte    portal;                 // 1427     | PrizeWeight   | Portal            |       |       |         8 | Likelyhood of 'Portal' prize appearing
                                                //----------+---------------+-------------------+-------+-------+-----------+---------------------------------------------------------------
        
        /** PrizeSettings constructor */
        public PrizeSettings(ByteArray data) {
            if(data.size() != 28) {
                Tools.printLog("ERROR: Invalid raw data size for PrizeSettings.");
                return;
            }
            this.recharge           = data.readByte(0);
            this.energy             = data.readByte(1);
            this.rotation           = data.readByte(2);
            this.stealth            = data.readByte(3);
            this.cloak              = data.readByte(4);
            this.xRadar             = data.readByte(5);
            this.warp               = data.readByte(6);
            this.gun                = data.readByte(7);
            this.bomb               = data.readByte(8);
            this.bouncingBullets    = data.readByte(9);
            this.thruster           = data.readByte(10);
            this.topSpeed           = data.readByte(11);
            this.quickCharge        = data.readByte(12);
            this.glue               = data.readByte(13);
            this.multiFire          = data.readByte(14);
            this.proximity          = data.readByte(15);
            this.allWeapons         = data.readByte(16);
            this.shields            = data.readByte(17);
            this.shrapnel           = data.readByte(18);
            this.antiWarp           = data.readByte(19);
            this.repel              = data.readByte(20);
            this.burst              = data.readByte(21);
            this.decoy              = data.readByte(22);
            this.thor               = data.readByte(23);
            this.multiPrize         = data.readByte(24);
            this.brick              = data.readByte(25);
            this.rocket             = data.readByte(26);
            this.portal             = data.readByte(27);
        }
        
        /*
         * Automatically generated getters.
         */
        /**
         * <b>[PrizeWeight] Recharge</b><br>
         * Full Charge prize. (Full charge, not recharge.)
         * @return Recharge weight. (0 to 255)
         */
        public short getRecharge() {
            return (short) (recharge & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Energy</b><br>
         * Energy Upgrade prize.
         * @return weight. (0 to 255)
         */
        public short getEnergy() {
            return (short) (energy & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Rotation</b><br>
         * Rotation prize.
         * @return Rotation weight. (0 to 255)
         */
        public short getRotation() {
            return (short) (rotation & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Stealth</b><br>
         * Stealth prize.
         * @return Stealth weight. (0 to 255)
         */
        public short getStealth() {
            return (short) (stealth & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Cloak</b><br>
         * Cloak prize.
         * @return Cloak weight. (0 to 255)
         */
        public short getCloak() {
            return (short) (cloak & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] XRadar</b><br>
         * XRadar prize.
         * @return XRadar weight. (0 to 255)
         */
        public short getxRadar() {
            return (short) (xRadar & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Warp</b><br>
         * Warp prize.
         * @return Warp weight. (0 to 255)
         */
        public short getWarp() {
            return (short) (warp & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Gun</b><br>
         * Gun Upgrade prize.
         * @return Gun weight. (0 to 255)
         */
        public short getGun() {
            return (short) (gun & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Bomb</b><br>
         * Bomb Upgrade prize.
         * @return Bomb weight. (0 to 255)
         */
        public short getBomb() {
            return (short) (bomb & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] BouncingBullets</b><br>
         * Bouncing Bullets prize.
         * @return Bouncing bullets weight. (0 to 255)
         */
        public short getBouncingBullets() {
            return (short) (bouncingBullets & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Thruster</b><br>
         * Thruster prize.
         * @return Thruster weight. (0 to 255)
         */
        public short getThruster() {
            return (short) (thruster & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] TopSpeed</b><br>
         * Speed prize.
         * @return Top speed weight. (0 to 255)
         */
        public short getTopSpeed() {
            return (short) (topSpeed & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] QuickCharge</b><br>
         * Recharge prize.
         * @return Quick charge weight. (0 to 255)
         */
        public short getQuickCharge() {
            return (short) (quickCharge & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Glue</b><br>
         * Engine Shutdown prize.
         * @return Glue weight. (0 to 255)
         */
        public short getGlue() {
            return (short) (glue & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] MultiFire</b><br>
         * MultiFire prize.
         * @return Multifire weight. (0 to 255)
         */
        public short getMultiFire() {
            return (short) (multiFire & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Proximity</b><br>
         * Proximity Bomb prize.
         * @return Proximity weight. (0 to 255)
         */
        public short getProximity() {
            return (short) (proximity & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] AllWeapons</b><br>
         * Super! prize.
         * @return All weapons weight. (0 to 255)
         */
        public short getAllWeapons() {
            return (short) (allWeapons & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Shields</b><br>
         * Shields prize.
         * @return Shields weight. (0 to 255)
         */
        public short getShields() {
            return (short) (shields & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Shrapnel</b><br>
         * Shrapnel Upgrade prize.
         * @return Shrapnel weight. (0 to 255)
         */
        public short getShrapnel() {
            return (short) (shrapnel & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] AntiWarp</b><br>
         * AntiWarp prize.
         * @return Antiwarp weight. (0 to 255)
         */
        public short getAntiWarp() {
            return (short) (antiWarp & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Repel</b><br>
         * Repel prize.
         * @return Repel weight. (0 to 255)
         */
        public short getRepel() {
            return (short) (repel & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Burst</b><br>
         * Burst prize.
         * @return Burst weight. (0 to 255)
         */
        public short getBurst() {
            return (short) (burst & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Decoy</b><br>
         * Decoy prize.
         * @return Decoy weight. (0 to 255)
         */
        public short getDecoy() {
            return (short) (decoy & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Thor</b><br>
         * Thor prize.
         * @return Thor weight. (0 to 255)
         */
        public short getThor() {
            return (short) (thor & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] MultiPrize</b><br>
         * Multi-prize.
         * @return Multi-prize weight. (0 to 255)
         */
        public short getMultiPrize() {
            return (short) (multiPrize & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Brick</b><br>
         * Brick prize.
         * @return Brick weight. (0 to 255)
         */
        public short getBrick() {
            return (short) (brick & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Rocket</b><br>
         * Rocket prize.
         * @return Rocket weight. (0 to 255)
         */
        public short getRocket() {
            return (short) (rocket & MASK_UINT8);
        }
        
        /**
         * <b>[PrizeWeight] Portal</b><br>
         * Portal prize.
         * @return Portal weight. (0 to 255)
         */
        public short getPortal() {
            return (short) (portal & MASK_UINT8);
        }
        
        /**
         * Debug method. Prints out all getter values. 
         * <p>
         * This is/was mainly used to verify that every getter is returning
         * the correct signed/unsigned value. Could prove useful in the future
         * whenever a discrepancy is found between what the bot thinks is an arena setting
         * and what actually is the arena setting.
         * @return List of getter names and their values.
         */
        public String toString() {
            StringBuilder result = new StringBuilder();
            String newLine = System.getProperty("line.separator");
            
            Class<?> objClass = this.getClass();

            result.append(objClass.getName() + " {" + newLine);
            // Get the public methods associated with this class.
            Method[] methods = objClass.getMethods();
            for (Method method:methods)
            {
                if(method.getName().startsWith("getClass")) {
                    continue;
                } else if(method.getName().startsWith("get") || method.getName().startsWith("is")) {
                    try {
                        result.append(method.getName() + ": " + method.invoke(this, new Object[] {}) + newLine);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                
            }
            
            result.append("}" + newLine);
            
            return result.toString();
        }
    }
}
