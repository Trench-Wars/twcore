<?php
    /* profile class o)-<| */
    class Profiles extends MY_Model {
        public function __construct() {
            parent::__construct();
        }

        public function findById($playerId) {
			$res = $this->db->query('
				SELECT 
					p.fnUserProfileId 			AS id,
					p.fnUserID					AS player_id,
					p.fcCountryCode				AS country_code,
					p.fcLocation				AS location,
					p.fnGender					AS gender,
					p.fdBirthdate				AS birthday,
					p.fcUrl						AS site_url,
					p.fnShip					AS ship_id,
					p.fcArena					AS arenas,
					p.fcBook					AS books,
					p.fcMovie					AS movies,
					p.fcSong					AS songs,
					p.fcInfo					AS info,
					p.fcQuote					AS quote,
					p.fdLastProfileCheck		AS last_check,
					p.ftUpdated					AS last_update,
                    u.fnUserID          		AS id,
                    u.fcUserName        		AS player_name,
                    u.fdSignedUp        		AS signup,
                    u.fdDeleted         		AS is_deleted,
                    u.ftUpdated         		AS last_update,
                    ua.fcPassword       		AS password,
                    ua.fcIP             		AS ip,
                    ua.fdLastLoggedIn   		AS last_login 				
				FROM tblUserProfile      p
				LEFT JOIN tblUser        u  ON u.fnUserID =p.fnUserID  
				LEFT JOIN tblUserAccount ua ON ua.fnUserID=p.fnUserID
				WHERE p.fnUserID = ?', array($playerId));
			return $res->row_array();
        }

		public function findCommentsForPlayerId($playerId) {
			$res = $this->db->query('
				SELECT
					fnCommentID 	AS id,
					fnUserID		AS player_id,
					fnUserPosterID	AS commenter_player_id,
					fcComment		AS comment,
					fcIP			AS commenter_ip,
					fdDeleted		AS is_deleted,
					fdCreated		AS created,
					ftUpdated		AS last_update
				FROM tblUserProfileComments WHERE fnUserID = ?', array($playerId));
		}

    }

