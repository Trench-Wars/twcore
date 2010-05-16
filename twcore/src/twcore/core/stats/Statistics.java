package twcore.core.stats;

import java.util.Vector;

		/**
		 * @author FoN
		 *
		 * This class is to congregate all the stats so they can be organised and added + removed easily
		 */
		public class Statistics
		{
			private Vector<Statistic> m_statistics;


			//stats!
			//dont forget to increment the totals accordingly
			//make a class and add it to the vector
			public static final int TOTAL_NORMAL_NUMBER = 40;
			
			public static final int DEATHS = 0;
			public static final int SCORE = 1;
			public static final int WARBIRD_KILL = 2;
			public static final int JAVELIN_KILL = 3;
			public static final int SPIDER_KILL = 4;
			public static final int LEVIATHAN_KILL = 5;
			public static final int TERRIER_KILL = 6;
			public static final int WEASEL_KILL = 7;
			public static final int LANCASTER_KILL = 8;
			public static final int SHARK_KILL = 9;
			public static final int WARBIRD_TEAMKILL = 10;
			public static final int JAVELIN_TEAMKILL = 11;
			public static final int SPIDER_TEAMKILL = 12;
			public static final int LEVIATHAN_TEAMKILL = 13;
			public static final int TERRIER_TEAMKILL = 14;
			public static final int WEASEL_TEAMKILL = 15;
			public static final int LANCASTER_TEAMKILL = 16;
			public static final int SHARK_TEAMKILL = 17;
			public static final int FLAG_CLAIMED = 18;
			public static final int REPELS_USED = 19;
			public static final int TOTAL_KILLS = 20;
			public static final int TOTAL_TEAMKILLS = 21;
			public static final int REPELS_PER_DEATH = 22;
			public static final int RATING = 23;
			public static final int BOMBS_FIRED = 24;
			public static final int BURSTS_FIRED = 25;
			public static final int BULLETS_FIRED = 26;
			public static final int MINES_FIRED = 27;
			public static final int PRIZES = 28;
			public static final int PRIZE_PORTAL = 29;
			public static final int PRIZE_REPEL = 30;
			public static final int PRIZE_BURST = 31;
			public static final int PRIZE_SHRAPNEL = 32;
			public static final int PRIZE_FULL_CHARGE = 33;
			public static final int KILL_SHORT_RANGE = 34;
			public static final int KILL_NORMAL_RANGE = 35;
			public static final int KILL_LONG_RANGE = 36;
			public static final int KILL_ULTRA_LONG_RANGE = 37;
			public static final int DEATH_ON_ATTACH = 38;
			public static final int SPAWN_DEATHS = 39;

			private int m_shipType;

			public Statistics(int shipType)
			{
				m_shipType = shipType;
				m_statistics = new Vector<Statistic>(TOTAL_NORMAL_NUMBER);
				fillStatistics();
			}

			private void fillStatistics()
			{
				m_statistics.add(DEATHS, new Deaths());
				m_statistics.add(SCORE, new Score());
				m_statistics.add(WARBIRD_KILL, new WarbirdKills());
				m_statistics.add(JAVELIN_KILL, new JavelinKills());
				m_statistics.add(SPIDER_KILL, new SpiderKills());
				m_statistics.add(LEVIATHAN_KILL, new LeviathanKills());
				m_statistics.add(TERRIER_KILL, new TerrierKills());
				m_statistics.add(WEASEL_KILL, new WeaselKills());
				m_statistics.add(LANCASTER_KILL, new LancasterKills());
				m_statistics.add(SHARK_KILL, new SharkKills());
				m_statistics.add(WARBIRD_TEAMKILL, new WarbirdTeamKills());
				m_statistics.add(JAVELIN_TEAMKILL, new JavelinTeamKills());
				m_statistics.add(SPIDER_TEAMKILL, new SpiderTeamKills());
				m_statistics.add(LEVIATHAN_TEAMKILL, new LeviathanTeamKills());
				m_statistics.add(TERRIER_TEAMKILL, new TerrierTeamKills());
				m_statistics.add(WEASEL_TEAMKILL, new WeaselTeamKills());
				m_statistics.add(LANCASTER_TEAMKILL, new LancasterTeamKills());
				m_statistics.add(SHARK_TEAMKILL, new SharkTeamKills());
				m_statistics.add(FLAG_CLAIMED, new FlagTouches());
				m_statistics.add(REPELS_USED, new RepelsUsed());
				m_statistics.add(TOTAL_KILLS, new TotalKills());
				m_statistics.add(TOTAL_TEAMKILLS, new TotalTeamKills());
				m_statistics.add(REPELS_PER_DEATH, new RepelsPerDeath(m_shipType));
				m_statistics.add(RATING, new Rating(m_shipType));
				m_statistics.add(BOMBS_FIRED, new BombsFired());
				m_statistics.add(BURSTS_FIRED, new BurstsFired());
				m_statistics.add(BULLETS_FIRED, new BulletsFired());
				m_statistics.add(MINES_FIRED, new MinesFired());
				m_statistics.add(PRIZES, new PrizeTotal());
				m_statistics.add(PRIZE_PORTAL, new PrizePortal());
				m_statistics.add(PRIZE_REPEL, new PrizeRepel());
				m_statistics.add(PRIZE_BURST, new PrizeBurst());
				m_statistics.add(PRIZE_SHRAPNEL, new PrizeShrapnel());
				m_statistics.add(PRIZE_FULL_CHARGE, new PrizeFullCharge());
				m_statistics.add(KILL_SHORT_RANGE, new KillShortRange());
				m_statistics.add(KILL_NORMAL_RANGE, new KillNormalRange());
				m_statistics.add(KILL_LONG_RANGE, new KillLongRange());
				m_statistics.add(KILL_ULTRA_LONG_RANGE, new KillUltraLongRange());
				m_statistics.add(DEATH_ON_ATTACH, new DeathOnAttach());
				m_statistics.add(SPAWN_DEATHS, new SpawnDeaths());
			}



			public int getIntStatistic(int statType)
			{
				return m_statistics.get(statType).getIntValue();
			}

			public double getDoubleStatistic(int statType)
			{
				return m_statistics.get(statType).getDoubleValue();
			}

			public Statistic getStatistic(int statType)
			{
				return m_statistics.get(statType);
			}

			public double getWeightedStatistic(int statType)
			{
				return m_statistics.get(statType).getWeightedValue();
			}

			public void setStatistic(int statType)
			{
				m_statistics.get(statType).setValue();
			}

			public void setStatistic(int statType, int value)
			{
				m_statistics.get(statType).setValue(value);
			}

			public void setStatistic(int statType, double value)
			{
				m_statistics.get(statType).setValue(value);
			}

			public void changeStatistic(int statType, int value)
			{
				m_statistics.get(statType).changeValue(value);
			}

			public void changeStatistic(int statType, double value)
			{
				m_statistics.get(statType).changeValue(value);
			}

			public void decrementStatistic(int statType)
			{
				m_statistics.get(statType).decrement();
			}

			/**
			 * Method getStatistics.
			 * @return String depending on the ship type
			 */
			public String[] getStatisticsSummary()
			{
				Vector<String> stats = new Vector<String>(2);

				final int STATS = 0;
				final int TEAMKILLS = 1;

				switch (m_shipType)
				{
					case 1 : //warbird

						stats.add(
							STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 2 : //jav
						stats.add(
							STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BOMBS_FIRED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));

						stats.add(
							TEAMKILLS,
							"Team Kills - "
								+ getStatistic(WARBIRD_TEAMKILL)
								+ getStatistic(JAVELIN_TEAMKILL)
								+ getStatistic(SPIDER_TEAMKILL)
								+ getStatistic(TERRIER_TEAMKILL)
								+ getStatistic(WEASEL_TEAMKILL)
								+ getStatistic(LANCASTER_TEAMKILL)
								+ getStatistic(SHARK_TEAMKILL));


						break;

					case 3 : //spider

						stats.add(
							STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 4 : //lev
						stats.add(STATS, getStatistic(SCORE).toString());
						break;

					case 5 : //terr
						stats.add(STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(BURSTS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 6 : //weasel
						stats.add(STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 7 : //lanc
						stats.add(STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(BULLETS_FIRED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 8 : //shark
						stats.add(STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(REPELS_USED)
								+ getStatistic(MINES_FIRED)
								+ getStatistic(BOMBS_FIRED)
								+ getStatistic(WARBIRD_KILL)
								+ getStatistic(JAVELIN_KILL)
								+ getStatistic(SPIDER_KILL)
								+ getStatistic(TERRIER_KILL)
								+ getStatistic(WEASEL_KILL)
								+ getStatistic(LANCASTER_KILL)
								+ getStatistic(SHARK_KILL)
								+ getStatistic(FLAG_CLAIMED)
								+ getStatistic(SCORE)
								+ getStatistic(RATING));

						stats.add(
							TEAMKILLS,
							"Team Kills - "
								+ getStatistic(WARBIRD_TEAMKILL)
								+ getStatistic(JAVELIN_TEAMKILL)
								+ getStatistic(SPIDER_TEAMKILL)
								+ getStatistic(TERRIER_TEAMKILL)
								+ getStatistic(WEASEL_TEAMKILL)
								+ getStatistic(LANCASTER_TEAMKILL)
								+ getStatistic(SHARK_TEAMKILL));
						break;

					default : //if errored
						stats.add(STATS, getStatistic(SCORE).toString());
						break;

				}

				stats.trimToSize();
				return stats.toArray(new String[stats.size()]);

			}





			/**
			 * Returns the m_shipType.
			 * @return int
			 */
			public int getShipType()
			{
				return m_shipType;
			}



			private class Deaths extends Statistic
			{
				public Deaths()
				{
					super(DEATHS, "D");
				}
			}
			
			private class SpawnDeaths extends Statistic
			{
				public SpawnDeaths()
				{
					super(SPAWN_DEATHS, "SD");
				}
			}

			private class Score extends Statistic
			{
				public Score()
				{
					super(SCORE, "S");
				}
			}

			private class FlagTouches extends Statistic
			{
				public FlagTouches()
				{
					super(FLAG_CLAIMED, "F");
				}
			}

			private class RepelsUsed extends Statistic
			{
				public RepelsUsed()
				{
					super(REPELS_USED, "Rep");
				}
			}
			
			private class BombsFired extends Statistic
			{
				public BombsFired()
				{
					super(BOMBS_FIRED, "FBomb");
				}
			}
			
			private class BulletsFired extends Statistic
			{
				public BulletsFired()
				{
					super(BULLETS_FIRED, "FBull");
				}
			}
			
			private class BurstsFired extends Statistic
			{
				public BurstsFired()
				{
					super(BURSTS_FIRED, "FBurs");
				}
			}
			
			private class MinesFired extends Statistic
			{
				public MinesFired()
				{
					super(MINES_FIRED, "FMine");
				}
			}
			
			private class PrizeTotal extends Statistic {
				public PrizeTotal() {
					super(PRIZES, "PRZs");
				}
			}
			
			private class PrizePortal extends Statistic {
				public PrizePortal() {
					super(PRIZE_PORTAL, "PPor");
				}
			}
			
			private class PrizeBurst extends Statistic {
				public PrizeBurst() {
					super(PRIZE_BURST, "PBur");
				}
			}
			
			private class PrizeShrapnel extends Statistic {
				public PrizeShrapnel() {
					super(PRIZE_SHRAPNEL, "PShr");
				}
			}
			
			private class PrizeFullCharge extends Statistic {
				public PrizeFullCharge() {
					super(PRIZE_FULL_CHARGE, "PFuC");
				}
			}
			
			private class PrizeRepel extends Statistic {
				public PrizeRepel() {
					super(PRIZE_REPEL, "PRep");
				}
			}
			
			private class KillShortRange extends Statistic {
				public KillShortRange() {
					super(KILL_SHORT_RANGE, "KSR");
				}
			}
			
			private class KillNormalRange extends Statistic {
				public KillNormalRange() {
					super(KILL_NORMAL_RANGE, "KNR");
				}
			}
			
			private class KillLongRange extends Statistic {
				public KillLongRange() {
					super(KILL_LONG_RANGE, "KLR");
				}
			}
			
			private class KillUltraLongRange extends Statistic {
				public KillUltraLongRange() {
					super(KILL_ULTRA_LONG_RANGE, "KULR");
				}
			}
			
			private class DeathOnAttach extends Statistic {
				public DeathOnAttach() {
					super(DEATH_ON_ATTACH, "DOA");
				}
			}
			
			private class WarbirdKills extends Statistic
			{
				public WarbirdKills()
				{
					super(WARBIRD_KILL, "Wk");
				}
			}

			private class JavelinKills extends Statistic
			{
				public JavelinKills()
				{
					super(JAVELIN_KILL, "Jk");
				}
			}

			private class SpiderKills extends Statistic
			{
				public SpiderKills()
				{
					super(SPIDER_KILL, "Sk");
				}
			}

			private class LeviathanKills extends Statistic
			{
				public LeviathanKills()
				{
					super(LEVIATHAN_KILL, "Levk");
				}
			}

			private class TerrierKills extends Statistic
			{
				public TerrierKills()
				{
					super(TERRIER_KILL, "Tek");
				}
			}

			private class WeaselKills extends Statistic
			{
				public WeaselKills()
				{
					super(WEASEL_KILL, "Xk");
				}
			}

			private class LancasterKills extends Statistic
			{
				public LancasterKills()
				{
					super(LANCASTER_KILL, "Lk");
				}
			}

			private class SharkKills extends Statistic
			{
				public SharkKills()
				{
					super(SHARK_KILL, "Shk");
				}
			}

			private class WarbirdTeamKills extends Statistic
			{
				public WarbirdTeamKills()
				{
					super(WARBIRD_TEAMKILL, "Wtk");
				}
			}

			private class JavelinTeamKills extends Statistic
			{
				public JavelinTeamKills()
				{
					super(JAVELIN_TEAMKILL, "Jtk");
				}
			}

			private class SpiderTeamKills extends Statistic
			{
				public SpiderTeamKills()
				{
					super(SPIDER_TEAMKILL, "Stk");
				}
			}

			private class LeviathanTeamKills extends Statistic
			{
				public LeviathanTeamKills()
				{
					super(LEVIATHAN_TEAMKILL, "Levtk");
				}
			}

			private class TerrierTeamKills extends Statistic
			{
				public TerrierTeamKills()
				{
					super(TERRIER_TEAMKILL, "Tetk");
				}
			}

			private class WeaselTeamKills extends Statistic
			{
				public WeaselTeamKills()
				{
					super(WEASEL_TEAMKILL, "Xtk");
				}
			}

			private class LancasterTeamKills extends Statistic
			{
				public LancasterTeamKills()
				{
					super(LANCASTER_TEAMKILL, "Ltk");
				}
			}

			private class SharkTeamKills extends Statistic
			{
				public SharkTeamKills()
				{
					super(SHARK_TEAMKILL, "Shtk");
				}
			}

			//Derived Stats
			private class TotalKills extends Statistic implements DerivedStatisticInterface
			{
				public TotalKills()
				{
					super(TOTAL_KILLS, "T", true);
				}

				public int derivedInt()
				{
					return getIntStatistic(WARBIRD_KILL)
						+ getIntStatistic(JAVELIN_KILL)
						+ getIntStatistic(SPIDER_KILL)
						+ getIntStatistic(LEVIATHAN_KILL)
						+ getIntStatistic(TERRIER_KILL)
						+ getIntStatistic(WEASEL_KILL)
						+ getIntStatistic(LANCASTER_KILL)
						+ getIntStatistic(SHARK_KILL);
				}

				public double derivedDouble()
				{
					return (double)derivedInt();
				}
			}

			private class TotalTeamKills extends Statistic implements DerivedStatisticInterface
			{
				public TotalTeamKills()
				{
					super(TOTAL_TEAMKILLS, "TK", true);
				}

				public int derivedInt()
				{
					return getIntStatistic(WARBIRD_TEAMKILL)
						+ getIntStatistic(JAVELIN_TEAMKILL)
						+ getIntStatistic(SPIDER_TEAMKILL)
						+ getIntStatistic(LEVIATHAN_TEAMKILL)
						+ getIntStatistic(TERRIER_TEAMKILL)
						+ getIntStatistic(WEASEL_TEAMKILL)
						+ getIntStatistic(LANCASTER_TEAMKILL)
						+ getIntStatistic(SHARK_TEAMKILL);
				}

				public double derivedDouble()
				{
					return (double)derivedInt();
				}
			}

			private class RepelsPerDeath extends Statistic implements DerivedStatisticInterface
			{
			    private int m_shipType;

			    public RepelsPerDeath(int shipType)
			    {
			        super(REPELS_PER_DEATH, "R/D", true); //derived is true
			        m_shipType = shipType;
			    }

			    public double derivedDouble()
			    {
			        if (m_shipType == 8) //shark
			        {
			            if (getIntStatistic(DEATHS)!= 0) //so you don't get a divide by zero error
			                return (getIntStatistic(REPELS_USED)/(getDoubleStatistic(DEATHS) * 2.0)); //same repel count *2 for each death, needs to be fixed
			            else
			                return (getIntStatistic(REPELS_USED)/2.0); //bug since repels used seem to be counted twice must be removed when the bug is found
			        }
			        else
			            return 0; //if any other ship
			    }

			    public int derivedInt()
			    {
			        return (int)derivedDouble();
			    }
			}

			private class Rating extends Statistic implements DerivedStatisticInterface
			{
				private int m_shipType;

				public Rating(int shipType)
				{
					super(RATING, "R", true); //derived is true
					m_shipType = shipType;
					setWeights();
				}

				/**
				 * Method getRating.
				 * This returns the rating for the player according to this:
				 *
				 * warbird: .45Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
				 *
				 * jav: .6Points * (.05wb + .06jav + .066spid + 0.14terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .13terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
				 *
				 * spiders: .4points * (.06wb + .06jav + .04spid + .09terr + .05x + .05lanc + .089shark - .05deaths)
				 *
				 * terr: 2.45points * (.03wb + .03jav + .036spid + .12terr + .35x + .025lanc + .052shark - .21deaths)
				 *
				 * weasel: .8points * (sum(.09allships) - 0.05deaths)
				 *
				 * lanc: .6Points * (.07wb + .07jav + .055spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
				 *
				 * shark: points * (.65*repels/death + .005terr + .0015shark + sum(.001allotherships) - 0.001deaths - (.07(allothershipstks) + .72spider + .5x + .15terrtk + .08sharkTK)))
				 *
				 * Original idea by Bleen and FoN
				 *
				 * @author FoN
				 *
				 * @return int which is the rating depending on the shiptype
				 */
				private void setWeights()
				{
					switch(m_shipType)
					{
						case 1: //wb
							m_statistics.get(SCORE).setWeight(0.45);
							m_statistics.get(WARBIRD_KILL).setWeight(0.07);
							m_statistics.get(JAVELIN_KILL).setWeight(0.07);
							m_statistics.get(SPIDER_KILL).setWeight(0.05);
							m_statistics.get(TERRIER_KILL).setWeight(0.12);
							m_statistics.get(WEASEL_KILL).setWeight(0.05);
							m_statistics.get(LANCASTER_KILL).setWeight(0.06);
							m_statistics.get(SHARK_KILL).setWeight(0.08);
							m_statistics.get(DEATHS).setWeight(0.04);
							break;
						case 2: //jav
							m_statistics.get(SCORE).setWeight(0.6);
							m_statistics.get(WARBIRD_KILL).setWeight(0.05);
							m_statistics.get(JAVELIN_KILL).setWeight(0.06);
							m_statistics.get(SPIDER_KILL).setWeight(0.066);
							m_statistics.get(TERRIER_KILL).setWeight(0.14);
							m_statistics.get(WEASEL_KILL).setWeight(0.07);
							m_statistics.get(LANCASTER_KILL).setWeight(0.05);
							m_statistics.get(SHARK_KILL).setWeight(0.09);
							m_statistics.get(DEATHS).setWeight(0.05);

							//teamkills
							m_statistics.get(WARBIRD_TEAMKILL).setWeight(0.07);
							m_statistics.get(JAVELIN_TEAMKILL).setWeight(0.07);
							m_statistics.get(SPIDER_TEAMKILL).setWeight(0.06);
							m_statistics.get(TERRIER_TEAMKILL).setWeight(0.13);
							m_statistics.get(WEASEL_TEAMKILL).setWeight(0.06);
							m_statistics.get(LANCASTER_TEAMKILL).setWeight(0.07);
							m_statistics.get(SHARK_TEAMKILL).setWeight(0.09);
							break;
						case 3: //spider
							m_statistics.get(SCORE).setWeight(0.4);
							m_statistics.get(WARBIRD_KILL).setWeight(0.06);
							m_statistics.get(JAVELIN_KILL).setWeight(0.06);
							m_statistics.get(SPIDER_KILL).setWeight(0.04);
							m_statistics.get(TERRIER_KILL).setWeight(0.09);
							m_statistics.get(WEASEL_KILL).setWeight(0.05);
							m_statistics.get(LANCASTER_KILL).setWeight(0.05);
							m_statistics.get(SHARK_KILL).setWeight(0.089);
							m_statistics.get(DEATHS).setWeight(0.05);
							break;
						case 4: //lev
							break;
						case 5: //terrier
							m_statistics.get(SCORE).setWeight(2.45);
							m_statistics.get(WARBIRD_KILL).setWeight(0.03);
							m_statistics.get(JAVELIN_KILL).setWeight(0.03);
							m_statistics.get(SPIDER_KILL).setWeight(0.036);
							m_statistics.get(TERRIER_KILL).setWeight(0.12);
							m_statistics.get(WEASEL_KILL).setWeight(0.035);
							m_statistics.get(LANCASTER_KILL).setWeight(0.025);
							m_statistics.get(SHARK_KILL).setWeight(0.052);
							m_statistics.get(DEATHS).setWeight(0.21);
							break;
						case 6: //x
							m_statistics.get(SCORE).setWeight(0.8);
							m_statistics.get(WARBIRD_KILL).setWeight(0.09);
							m_statistics.get(JAVELIN_KILL).setWeight(0.09);
							m_statistics.get(SPIDER_KILL).setWeight(0.09);
							m_statistics.get(TERRIER_KILL).setWeight(0.9);
							m_statistics.get(WEASEL_KILL).setWeight(0.09);
							m_statistics.get(LANCASTER_KILL).setWeight(0.09);
							m_statistics.get(SHARK_KILL).setWeight(0.09);
							m_statistics.get(DEATHS).setWeight(0.05);
							break;
						case 7: //lanc
							m_statistics.get(SCORE).setWeight(0.6);
							m_statistics.get(WARBIRD_KILL).setWeight(0.07);
							m_statistics.get(JAVELIN_KILL).setWeight(0.07);
							m_statistics.get(SPIDER_KILL).setWeight(0.055);
							m_statistics.get(TERRIER_KILL).setWeight(0.12);
							m_statistics.get(WEASEL_KILL).setWeight(0.05);
							m_statistics.get(LANCASTER_KILL).setWeight(0.06);
							m_statistics.get(SHARK_KILL).setWeight(0.08);
							m_statistics.get(DEATHS).setWeight(0.04);
							break;
						case 8: //shark
							m_statistics.get(SCORE).setWeight(1);
							m_statistics.get(REPELS_PER_DEATH).setWeight(0.65);
							m_statistics.get(WARBIRD_KILL).setWeight(0.001);
							m_statistics.get(JAVELIN_KILL).setWeight(0.001);
							m_statistics.get(SPIDER_KILL).setWeight(0.001);
							m_statistics.get(TERRIER_KILL).setWeight(0.005);
							m_statistics.get(WEASEL_KILL).setWeight(0.001);
							m_statistics.get(LANCASTER_KILL).setWeight(0.001);
							m_statistics.get(SHARK_KILL).setWeight(0.0015);
							m_statistics.get(DEATHS).setWeight(0.001);

							//teamkills
							m_statistics.get(WARBIRD_TEAMKILL).setWeight(0.07);
							m_statistics.get(JAVELIN_TEAMKILL).setWeight(0.07);
							m_statistics.get(SPIDER_TEAMKILL).setWeight(0.072);
							m_statistics.get(TERRIER_TEAMKILL).setWeight(0.15);
							m_statistics.get(WEASEL_TEAMKILL).setWeight(0.05);
							m_statistics.get(LANCASTER_TEAMKILL).setWeight(0.07);
							m_statistics.get(SHARK_TEAMKILL).setWeight(0.08);
							break;
					};
				}


				public double derivedDouble()
				{
					return (double)derivedInt();
				}

				public int derivedInt()
				{
					int rating = 0;

					switch (m_shipType)
					{
						case 1 : //warbird
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS)));
							return rating;

						case 2 : //jav
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS) - getTotalWeightedTeamKills()));
							return rating;

						case 3 : //spider
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS)));
							return rating;

						case 4 : //lev
							rating = (int) getWeightedStatistic(SCORE);
							return rating;

						case 5 : //terr
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS)));
							return rating;

						case 6 : //weasel
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS)));
							return rating;

						case 7 : //lanc
							rating = (int) (getWeightedStatistic(SCORE) * (getTotalWeightedKills() - getWeightedStatistic(DEATHS)));
							return rating;

						case 8 : //shark
							rating = (int) (getWeightedStatistic(SCORE) * (getWeightedStatistic(REPELS_PER_DEATH) + getTotalWeightedKills() - getWeightedStatistic(DEATHS) - getTotalWeightedTeamKills()));
							return rating;

						default : //if errored
							rating = getIntStatistic(SCORE);
							return rating;

					}
				}

				private double getTotalWeightedKills()
				{
					return (getWeightedStatistic(WARBIRD_KILL)
							+ getWeightedStatistic(JAVELIN_KILL)
							+ getWeightedStatistic(SPIDER_KILL)
							+ getWeightedStatistic(TERRIER_KILL)
							+ getWeightedStatistic(WEASEL_KILL)
							+ getWeightedStatistic(LANCASTER_KILL)
							+ getWeightedStatistic(SHARK_KILL));
				}

				private double getTotalWeightedTeamKills()
				{
					return (getWeightedStatistic(WARBIRD_TEAMKILL)
							+ getWeightedStatistic(JAVELIN_TEAMKILL)
							+ getWeightedStatistic(SPIDER_TEAMKILL)
							+ getWeightedStatistic(TERRIER_TEAMKILL)
							+ getWeightedStatistic(WEASEL_TEAMKILL)
							+ getWeightedStatistic(LANCASTER_TEAMKILL)
							+ getWeightedStatistic(SHARK_TEAMKILL));
				}
			}
		}