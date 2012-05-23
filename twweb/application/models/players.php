<?php
	/* These functions should change the selected fields once the schema is updated. Then functions can be normalized.. o)-<| */
	class Players extends MY_Model {
		public $tableName = 'players';

		public function __construct() {
			parent::__construct();
		}

		public function getIdByName($name) {
			$res = $this->db->query('SELECT fnUserID AS id FROM tblUser WHERE fcUserName = ?', array($name));
			if($row = $res->row_array()) return $row['id'];
			else return false;
		}

        public function findByName($playerId) {
			// TODO: This should use Sphinx or something similar to translate text names to IDs, then
			// the ID should be passed to the findById function.. 
			$res = $this->db->query('SELECT fnUserID FROM tblUser WHERE fcUserName = ?', array($playerId));
			if($row = $res->row_array()) return $this->findById($row['fnUserID']);
			else return false;
        }

		public function findById($playerId) {
            $res = $this->db->query('
                SELECT 
                    u.fnUserID          AS id,
                    u.fcUserName        AS player_name,
                    u.fdSignedUp        AS signup,
                    u.fdDeleted         AS is_deleted,
                    u.ftUpdated         AS last_update,
                    ua.fcPassword       AS password,
                    ua.fcIP             AS ip,
                    ua.fdLastLoggedIn   AS last_login       
                FROM tblUser u 
                LEFT JOIN tblUserAccount ua ON ua.fnUserID=u.fnUserID 
                WHERE u.fnUserID = ?', array($playerId));
            return $res->row_array();
        }

		public function checkPassword($playerName, $password) {
			$res = $this->db->query('
				SELECT	u.fnUserID AS player_id
				FROM tblUser u 
				LEFT JOIN tblUserAccount ua ON ua.fnUserID=u.fnUserID 
				WHERE ua.fcPassword = PASSWORD(?) AND u.fcUserName = ?', array($password, $playerName));

			if($row = $res->row_array()) return $this->findById($row['player_id']);
			else return false;
		}
	}
