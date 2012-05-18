<?php
	class Home extends CI_Controller {
		public function __construct() {
			parent::__construct();
		}

		public function index() {
			$this->load->view('include/header');
			$this->load->view('home_index');
			$this->load->view('include/footer');
		}
	}
