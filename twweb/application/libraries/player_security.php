<?php
	/*	Basic security class o)-<| */
	class Player_security {
		public function __construct() {
			$this->ci =& get_instance();
		}

		public function isLoggedIn() {
			if($this->ci->session->userdata('logged_in') == 'true') return true;
			else return false;
		}

		public function login($playerData) {
			$this->ci->session->set_userdata(array(
				'logged_in'   => 'true', 
				'player_name' => $playerData['player_name'],
				'player_id'   => $playerData['id']
			));
		}

		public function logout() {
			$this->ci->session->set_userdata(array('logged_in' => ''));
			$this->ci->session->sess_destroy();
		}
	}
