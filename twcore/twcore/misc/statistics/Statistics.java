package twcore.misc.statistics;

import java.util.Vector;

    /**
     * @author FoN
     *
     * This class is to congregate all the stats so they can be organised and added + removed easily
     */
    public class Statistics
    {
        //stats
        private int m_wbKill;
        private int m_javKill;
        private int m_spiderKill;
        private int m_levKill;
        private int m_terrKill;
        private int m_weaselKill;
        private int m_lancKill;
        private int m_sharkKill;

        private int m_deaths;
        private int m_score;
        private int m_flagClaimed;

        //teamkill
        private int m_wbTeamKill;
        private int m_javTeamKill;
        private int m_spiderTeamKill;
        private int m_levTeamKill;
        private int m_terrTeamKill;
        private int m_weaselTeamKill;
        private int m_lancTeamKill;
        private int m_sharkTeamKill;

        private int m_shipType;

        public Statistics(int shipType)
        {
            m_shipType = shipType;
            reset();
        }

            public int getStatistic(int statType)
            {
                switch (statType)
                {
                    case StatisticRequester.TOTAL_KILLS :
                        return m_wbKill + m_javKill + m_spiderKill + m_levKill + m_terrKill + m_weaselKill + m_lancKill + m_sharkKill;
                    case StatisticRequester.DEATHS :
                        return m_deaths;
                    case StatisticRequester.SCORE :
                        return m_score;
                    case StatisticRequester.WARBIRD_KILL:
                        return m_wbKill;
                    case StatisticRequester.JAVELIN_KILL:
                        return m_javKill;
                    case StatisticRequester.SPIDER_KILL:
                        return m_spiderKill;
                    case StatisticRequester.LEVIATHAN_KILL:
                        return m_levKill;
                    case StatisticRequester.TERRIER_KILL:
                        return m_terrKill;
                    case StatisticRequester.WEASEL_KILL:
                        return m_weaselKill;
                    case StatisticRequester.LANCASTER_KILL:
                        return m_lancKill;
                    case StatisticRequester.SHARK_KILL:
                        return m_sharkKill;
                    case StatisticRequester.WARBIRD_TEAMKILL:
                        return m_wbTeamKill;
                    case StatisticRequester.JAVELIN_TEAMKILL:
                        return m_javTeamKill;
                    case StatisticRequester.SPIDER_TEAMKILL:
                        return m_spiderTeamKill;
                    case StatisticRequester.LEVIATHAN_TEAMKILL:
                        return m_levTeamKill;
                    case StatisticRequester.TERRIER_TEAMKILL:
                        return m_terrTeamKill;
                    case StatisticRequester.WEASEL_TEAMKILL:
                        return m_weaselTeamKill;
                    case StatisticRequester.LANCASTER_TEAMKILL:
                        return m_lancTeamKill;
                    case StatisticRequester.SHARK_TEAMKILL:
                        return m_sharkTeamKill;
                    case StatisticRequester.FLAG_CLAIMED:
                        return m_flagClaimed;
                    case StatisticRequester.SHIP_TYPE:
                        return m_shipType;
                    case StatisticRequester.TOTAL_TEAMKILLS:
                        return m_wbTeamKill + m_javTeamKill + m_spiderTeamKill + m_levTeamKill + m_terrTeamKill + m_weaselTeamKill + m_lancTeamKill + m_sharkTeamKill;
                    case StatisticRequester.RATING:
                        return getRating();
                    default : //if errored
                        return 0;
                }

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
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        break;

                    case 2 : //jav
                        stats.add(
                            FORMULA,
                            "Jav: .5Points * (.05wb + .05jav + .06spid + 0.12terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .15terrTK + .06WeaselTK + .07LancTK + .09SharkTK))");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " TKs: "
                                + getStatistic(StatisticRequester.TOTAL_TEAMKILLS)
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        stats.add(
                            TEAMKILLS,
                            "Team Kills - Wtk: "
                                + m_wbTeamKill
                                + " Jtk: "
                                + m_javTeamKill
                                + " Stk: "
                                + m_spiderTeamKill
                                + " Ttk: "
                                + m_terrTeamKill
                                + " Xtk: "
                                + m_weaselTeamKill
                                + " Ltk: "
                                + m_lancTeamKill
                                + " ShtK: "
                                + m_sharkTeamKill);

                        break;

                    case 3 : //spider
                        stats.add(FORMULA, "Spider: .4points * (.06wb + .06jav + .04spid + .08terr + .05x + .05lanc + .09shark - .05deaths)");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        break;

                    case 4 : //lev
                        stats.add(FORMULA, "Leviathen Forumla: Points");
                        stats.add(STATS, "S: " + m_score);
                        break;

                    case 5 : //terr
                        stats.add(FORMULA, "Terr: .8points * (.06wb + .06jav + .08spid + .12terr + .1x + .06lanc + .09shark - .12deaths)");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        break;

                    case 6 : //weasel
                        stats.add(FORMULA, "Weasel: .8points * (sum(.09allships) - 0.05deaths)");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        break;

                    case 7 : //lanc
                        stats.add(FORMULA, "Lanc: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        break;

                    case 8 : //shark
                        stats.add(FORMULA, "Shark: 1.2points * (.12terr + sum(.09allotherships) - 0.005deaths - (.1(allothershipstks) + .15terrtk + .11sharkTK))");
                        stats.add(
                            STATS,
                            "K: "
                                + getStatistic(StatisticRequester.TOTAL_KILLS)
                                + " D: "
                                + m_deaths
                                + " TKs: "
                                + getStatistic(StatisticRequester.TOTAL_TEAMKILLS)
                                + " Wk: "
                                + m_wbKill
                                + " Jk: "
                                + m_javKill
                                + " Sk: "
                                + m_spiderKill
                                + " Tk: "
                                + m_terrKill
                                + " Xk: "
                                + m_weaselKill
                                + " Lk: "
                                + m_lancKill
                                + " ShK: "
                                + m_sharkKill
                                + " F: "
                                + m_flagClaimed
                                + " S: "
                                + m_score
                                + " R: "
                                + getRating());
                        stats.add(
                            TEAMKILLS,
                            "Team Kills - Wtk: "
                                + m_wbTeamKill
                                + " Jtk: "
                                + m_javTeamKill
                                + " Stk: "
                                + m_spiderTeamKill
                                + " Ttk: "
                                + m_terrTeamKill
                                + " Xtk: "
                                + m_weaselTeamKill
                                + " Ltk: "
                                + m_lancTeamKill
                                + " ShtK: "
                                + m_sharkTeamKill);
                        break;

                    default : //if errored
                        stats.add(FORMULA, "Default");
                        stats.add(STATS, "S: " + m_score);
                        break;

                }

                stats.trimToSize();
                return (String[]) stats.toArray(new String[stats.size()]);

            }

        /**
         * Method getRating.
         * This returns the rating for the player according to this:
         *
         * warbird: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
         *
         * jav: .5Points * (.05wb + .05jav + .06spid + 0.12terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .15terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
         *
         * spiders: .4points * (.06wb + .06jav + .04spid + .08terr + .05x + .05lanc + .09shark - .05deaths)
         *
         * terr: .8points * (.06wb + .06jav + .08spid + .12terr + .1x + .06lanc + .09shark - .12deaths)
         *
         * weasel: .8points * (sum(.09allships) - 0.05deaths)
         *
         * lanc: .6Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
         *
         * shark: 1.2points * (.12terr + sum(.09allotherships) - 0.005deaths - (.1(allothershipstks) + .15terrtk + .11sharkTK)))
         *
         * Original idea by Bleen and FoN
         *
         * @author FoN
         *
         * @return int which is the rating depending on the shiptype
         */
        private int getRating()
        {
            int rating = 0;

            switch (m_shipType)
            {
                case 1 : //warbird
                    rating =
                        (int) (m_score
                            * 0.6
                            * (m_wbKill * 0.07
                                + m_javKill * 0.07
                                + m_spiderKill * 0.05
                                + m_levKill
                                + m_terrKill * 0.12
                                + m_weaselKill * 0.05
                                + m_lancKill * 0.06
                                + m_sharkKill * 0.08
                                - m_deaths * 0.04));
                    return rating;

                case 2 : //jav
                    rating =
                        (int) (m_score
                            * 0.5
                            * (m_wbKill * 0.05
                                + m_javKill * 0.07
                                + m_spiderKill * 0.06
                                + m_levKill
                                + m_terrKill * 0.12
                                + m_weaselKill * 0.07
                                + m_lancKill * 0.05
                                + m_sharkKill * 0.09
                                - m_deaths * 0.05
                                - (m_wbTeamKill * 0.07
                                    + m_javTeamKill * 0.07
                                    + m_spiderTeamKill * 0.06
                                    + m_levTeamKill * 0.06
                                    + m_terrTeamKill * 0.15
                                    + m_weaselTeamKill * 0.06
                                    + m_lancTeamKill * 0.07
                                    + m_sharkTeamKill * .09)));
                    return rating;

                case 3 : //spider
                    rating =
                        (int) (m_score
                            * 0.4
                            * (m_wbKill * 0.06
                                + m_javKill * 0.06
                                + m_spiderKill * 0.04
                                + m_levKill
                                + m_terrKill * 0.08
                                + m_weaselKill * 0.05
                                + m_lancKill * 0.05
                                + m_sharkKill * 0.09
                                - m_deaths * 0.05));
                    return rating;

                case 4 : //lev
                    rating = m_score;
                    return rating;

                case 5 : //terr
                    rating =
                        (int) (m_score
                            * 0.8
                            * (m_wbKill * 0.06
                                + m_javKill * 0.06
                                + m_spiderKill * 0.08
                                + m_levKill
                                + m_terrKill * 0.12
                                + m_weaselKill * 0.1
                                + m_lancKill * 0.1
                                + m_sharkKill * 0.09
                                - m_deaths * 0.12));
                    return rating;

                case 6 : //weasel
                    rating =
                        (int) (m_score
                            * 0.8
                            * (m_wbKill * 0.09
                                + m_javKill * 0.09
                                + m_spiderKill * 0.09
                                + m_levKill
                                + m_terrKill * 0.09
                                + m_weaselKill * 0.09
                                + m_lancKill * 0.09
                                + m_sharkKill * 0.09
                                - m_deaths * 0.05));
                    return rating;

                case 7 : //lanc
                    rating =
                        (int) (m_score
                            * 0.6
                            * (m_wbKill * 0.07
                                + m_javKill * 0.07
                                + m_spiderKill * 0.05
                                + m_levKill
                                + m_terrKill * 0.12
                                + m_weaselKill * 0.05
                                + m_lancKill * 0.06
                                + m_sharkKill * 0.08
                                - m_deaths * 0.04));
                    return rating;

                case 8 : //shark
                    rating =
                        (int) (1.2
                            * m_score
                            * (m_wbKill * 0.09
                                + m_javKill * 0.09
                                + m_spiderKill * 0.09
                                + m_levKill
                                + m_terrKill * 0.12
                                + m_weaselKill * 0.09
                                + m_lancKill * 0.09
                                + m_sharkKill * 0.09
                                - m_deaths * 0.005
                                - (m_wbTeamKill * 0.1
                                    + m_javTeamKill * 0.1
                                    + m_spiderTeamKill * 0.1
                                    + m_levTeamKill * 0.1
                                    + m_terrTeamKill * 0.15
                                    + m_weaselTeamKill * 0.1
                                    + m_lancTeamKill * 0.1
                                    + m_sharkTeamKill * 0.11)));
                    return rating;

                default : //if errored
                    rating = m_score;
                    return rating;
            }

        }

        /**
         * sets all the stat variables to zero or their initial values
         */
        private void reset()
        {
            m_wbKill = 0;
            m_javKill = 0;
            m_spiderKill = 0;
            m_levKill = 0;
            m_terrKill = 0;
            m_weaselKill = 0;
            m_lancKill = 0;
            m_sharkKill = 0;
            m_deaths = 0;
            m_score = 0;
            m_flagClaimed = 0;
            m_wbTeamKill = 0;
            m_javTeamKill = 0;
            m_spiderTeamKill = 0;
            m_levTeamKill = 0;
            m_terrTeamKill = 0;
            m_weaselTeamKill = 0;
            m_lancTeamKill = 0;
            m_sharkTeamKill = 0;
        }



        /**
         * Sets the m_javKill.
         * Increments it by one
         */
        public void setJavKill()
        {
            m_javKill++;
        }

        /**
         * Sets the m_lancKill.
         * Increments it by one
         */
        public void setLancKill()
        {
            m_lancKill++;
        }

        /**
         * Sets the m_levKill.
         * Increments it by one
         */
        public void setLevKill()
        {
            m_levKill++;
        }

        /**
         * Sets the m_sharkKill.
         * Increments it by one
             */
        public void setSharkKill()
        {
            m_sharkKill++;
        }

        /**
         * Sets the m_spiderKill.
         * Increments it by one
         */
        public void setSpiderKill()
        {
            m_spiderKill++;
        }

        /**
         * Sets the m_terrKill.
         * Increments it by one
         */
        public void setTerrKill()
        {
            m_terrKill++;
        }

        /**
         * Sets the m_wbKill.
         * Increments it by one
         */
        public void setWbKill()
        {
            m_wbKill++;
        }

        /**
         * Sets the m_weaselKill.
         * Increments it by one
         */
        public void setWeaselKill()
        {
            m_weaselKill++;
        }

        /**
         * Sets the m_deaths.
         */
        public void setDeaths()
        {
            m_deaths++;
        }

        /**
         * Changes the death via the input
         * @ param deaths The deaths to be changed to
         */
        public void changeDeaths(int deaths)
        {
            m_deaths = deaths;
        }

        /**
         * Adds to the m_score.
         * @param score The m_score to set
         */
        public void setScore(int score)
        {
            m_score += score;
        }

        /**
         * Sets the m_flagClaimed.
         */
        public void setFlagClaimed()
        {
            m_flagClaimed++;
        }

        /**
         * Sets the m_javTeamKill.
         * @param m_javTeamKill The m_javTeamKill to set
         */
        public void setJavTeamKill()
        {
            m_javTeamKill++;
        }

        /**
         * Sets the m_lancTeamKill.
         * @param m_lancTeamKill The m_lancTeamKill to set
         */
        public void setLancTeamKill()
        {
            m_lancTeamKill++;
        }

        /**
         * Sets the m_levTeamKill.
         * @param m_levTeamKill The m_levTeamKill to set
         */
        public void setLevTeamKill()
        {
            m_levTeamKill++;
        }

        /**
         * Sets the m_sharkTeamKill.
         * @param m_sharkTeamKill The m_sharkTeamKill to set
         */
        public void setSharkTeamKill()
        {
            m_sharkTeamKill++;
        }

        /**
         * Sets the m_spiderTeamKill.
         * @param m_spiderTeamKill The m_spiderTeamKill to set
         */
        public void setSpiderTeamKill()
        {
            m_spiderTeamKill++;
        }

        /**
         * Sets the m_terrTeamKill.
         * @param m_terrTeamKill The m_terrTeamKill to set
         */
        public void setTerrTeamKill()
        {
            m_terrTeamKill++;
        }

        /**
         * Sets the m_wbTeamKill.
         * @param m_wbTeamKill The m_wbTeamKill to set
         */
        public void setWbTeamKill()
        {
            m_wbTeamKill++;
        }

        /**
         * Sets the m_weaselTeamKill.
         * @param m_weaselTeamKill The m_weaselTeamKill to set
         */
        public void setWeaselTeamKill()
        {
            m_weaselTeamKill++;
        }

        /**
         * Returns the m_shipType.
         * @return int
         */
        public int getShipType()
        {
            return m_shipType;
        }

        /**
         * Sets the m_shipType.
         * @param shipType The m_shipType to set
         */
        public void setShipType(int shipType)
        {
            m_shipType = shipType;
        }

    }
