<?php	
	class Players extends MY_Model {
		public $tableName = 'players';

		public function __construct() {
			parent::__construct();
		}

		public function checkPassword($playerName, $password) {
			$res = $this->db->query('SELECT * FROM tblUser u LEFT JOIN tblUserAccount ua ON ua.fnUserID=u.fnUserID WHERE ua.fcPassword = PASSWORD(?) AND u.fcUserName = ?', array($password, $playerName));
			if($row = $res->row_array()) return $row;
			else return false;
		}
	}
