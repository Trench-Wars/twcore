package twcore.misc.statistics;

import java.util.Vector;

		/**
		 * @author FoN
		 *
		 * This class is to congregate all the stats so they can be organised and added + removed easily
		 */
		public class Statistics
		{
			private Vector m_statistics;


			//stats
			//dont forget to increment the totals accordingly
			//make a class and add it to the vector
			public static final int TOTAL_NORMAL_NUMBER = 23;
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
			public static final int RATING = 22;
			
			private int m_shipType;

			public Statistics(int shipType)
			{
				m_shipType = shipType;
				m_statistics = new Vector(TOTAL_NORMAL_NUMBER);
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
				m_statistics.add(RATING, new Rating(m_shipType));
			}



			public int getIntStatistic(int statType)
			{
					return ((Statistic) m_statistics.get(statType)).getIntValue();
			}
			
			public double getDoubleStatistic(int statType)
			{
				return ((Statistic) m_statistics.get(statType)).getDoubleValue();
			}

			public Statistic getStatistic(int statType)
			{
				return (Statistic)m_statistics.get(statType);
			}
			
			public double getWeightedStatistic(int statType)
			{
				return ((Statistic)m_statistics.get(statType)).getWeightedValue();
			}
			
			public void setStatistic(int statType)
			{
				((Statistic)m_statistics.get(statType)).setValue();
			}
			
			public void setStatistic(int statType, int value)
			{
				((Statistic)m_statistics.get(statType)).setValue(value);
			}
			
			public void setStatistic(int statType, double value)
			{
				((Statistic)m_statistics.get(statType)).setValue(value);
			}
			
			public void changeStatistic(int statType, int value)
			{
				((Statistic)m_statistics.get(statType)).changeValue(value);
			}
			
			public void changeStatistic(int statType, double value)
			{
				((Statistic)m_statistics.get(statType)).changeValue(value);
			}
			
			public void decrementStatistic(int statType)
			{
				((Statistic)m_statistics.get(statType)).decrement();
			}
	
			/**
			 * Method getStatistics.
			 * @return String depending on the ship type
			 */
			public String[] getStatisticsSummary()
			{
				Vector stats = new Vector(3);

				final int FORMULA = 0;
				final int STATS = 1;
				final int TEAMKILLS = 2;

				switch (m_shipType)
				{
					case 1 : //warbird
						stats.add(FORMULA, "Warbird: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)");
										
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
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 2 : //jav
						stats.add(
							FORMULA,
							"Jav: .5Points * (.05wb + .05jav + .06spid + 0.12terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .15terrTK + .06WeaselTK + .07LancTK + .09SharkTK))");
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
						stats.add(FORMULA, "Spider: .4points * (.06wb + .06jav + .04spid + .08terr + .05x + .05lanc + .09shark - .05deaths)");
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
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 4 : //lev
						stats.add(FORMULA, "Leviathen Forumla: Points");
						stats.add(STATS, getStatistic(SCORE));
						break;

					case 5 : //terr
						stats.add(FORMULA, "Terr: .8points * (.06wb + .06jav + .08spid + .12terr + .1x + .06lanc + .09shark - .12deaths)");
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
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 6 : //weasel
						stats.add(FORMULA, "Weasel: .8points * (sum(.09allships) - 0.05deaths)");
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
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 7 : //lanc
						stats.add(FORMULA, "Lanc: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)");
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
								+ getStatistic(SCORE)
								+ getStatistic(RATING));
						break;

					case 8 : //shark
						stats.add(
							FORMULA,
							"Shark: 0.5points(RepelUsedPerDeath) * (.12terr + sum(.06allotherships) - 0.006deaths - (.07(allothershipstks) + .15terrtk + ..09sharkTK))");
						stats.add(STATS,
								""
								+ getStatistic(TOTAL_KILLS)
								+ getStatistic(DEATHS)
								+ getStatistic(TOTAL_TEAMKILLS)
								+ getStatistic(REPELS_USED)
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
						stats.add(FORMULA, "Default");
						stats.add(STATS, getStatistic(SCORE));
						break;

				}

				stats.trimToSize();
				return (String[]) stats.toArray(new String[stats.size()]);

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
				 * warbird: .5Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
				 *
				 * jav: .6Points * (.05wb + .05jav + .06spid + 0.12terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .15terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
				 *
				 * spiders: .4points * (.06wb + .06jav + .04spid + .08terr + .05x + .05lanc + .09shark - .05deaths)
				 *
				 * terr: .8points * (.06wb + .06jav + .08spid + .12terr + .1x + .06lanc + .09shark - .12deaths)
				 *
				 * weasel: .8points * (sum(.09allships) - 0.05deaths)
				 *
				 * lanc: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
				 *
				 * shark: 0.5(repelsUsed/deaths)points * (.12terr + sum(.06allotherships) - 0.006deaths - (.07(allothershipstks) + .15terrtk + .09sharkTK)))
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
							((Statistic)m_statistics.get(SCORE)).setWeight(0.5);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.08);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.04);
							break;
						case 2: //jav
							((Statistic)m_statistics.get(SCORE)).setWeight(0.6);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.05);
														
							//teamkills
							((Statistic)m_statistics.get(WARBIRD_TEAMKILL)).setWeight(0.07);
							((Statistic)m_statistics.get(JAVELIN_TEAMKILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SPIDER_TEAMKILL)).setWeight(0.06);
							((Statistic)m_statistics.get(TERRIER_TEAMKILL)).setWeight(0.15);
							((Statistic)m_statistics.get(WEASEL_TEAMKILL)).setWeight(0.06);
							((Statistic)m_statistics.get(LANCASTER_TEAMKILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SHARK_TEAMKILL)).setWeight(0.09);							
							break;
						case 3: //spider
							((Statistic)m_statistics.get(SCORE)).setWeight(0.4);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.04);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.08);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.05);
							break;
						case 4: //lev
							break;
						case 5: //terrier
							((Statistic)m_statistics.get(SCORE)).setWeight(0.8);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.08);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.12);
							break;
						case 6: //x
							((Statistic)m_statistics.get(SCORE)).setWeight(0.8);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.9);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.09);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.05);			
							break;
						case 7: //lanc
							((Statistic)m_statistics.get(SCORE)).setWeight(0.6);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.08);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.04);						
							break;
						case 8: //shark
							((Statistic)m_statistics.get(SCORE)).setWeight(0.5);
							((Statistic)m_statistics.get(WARBIRD_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(JAVELIN_KILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SPIDER_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(TERRIER_KILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_KILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_KILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SHARK_KILL)).setWeight(0.08);
							((Statistic)m_statistics.get(DEATHS)).setWeight(0.04);											
							
							//teamkills
							((Statistic)m_statistics.get(WARBIRD_TEAMKILL)).setWeight(0.07);
							((Statistic)m_statistics.get(JAVELIN_TEAMKILL)).setWeight(0.07);
							((Statistic)m_statistics.get(SPIDER_TEAMKILL)).setWeight(0.05);
							((Statistic)m_statistics.get(TERRIER_TEAMKILL)).setWeight(0.12);
							((Statistic)m_statistics.get(WEASEL_TEAMKILL)).setWeight(0.05);
							((Statistic)m_statistics.get(LANCASTER_TEAMKILL)).setWeight(0.06);
							((Statistic)m_statistics.get(SHARK_TEAMKILL)).setWeight(0.08);							
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
							rating = (int) (getWeightedStatistic(SCORE) * getRepelsUsedPerDeath() * (getTotalWeightedKills() - getWeightedStatistic(DEATHS) - getTotalWeightedTeamKills()));
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
				
				private double getRepelsUsedPerDeath()
				{
					if (getIntStatistic(DEATHS) == 0)
						return getDoubleStatistic(REPELS_USED);
					else
						return getIntStatistic(REPELS_USED) / getDoubleStatistic(DEATHS);
				}
				


			}
		}