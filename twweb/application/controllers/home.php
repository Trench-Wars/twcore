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

		public function login() {
			if($this->input->get_post('player_name') != '' && $this->input->get_post('password') != '') {
				$this->load->model('players');

				// Lets check the login. If valid, set the session info
				if($playerData = $this->players->checkPassword($this->input->get_post('player_name'), $this->input->get_post('password'))) {
					$this->player_security->login($playerData);
					redirect('');
				} else {
					$this->session->set_flashdata('message', 'INVALID LOGIN.');
					redirect('home/login');
				}
			}

			$this->load->view('include/header');
			$this->load->view('home_login');
			$this->load->view('include/footer');
		}

		public function logout() {
			$this->player_security->logout();
			redirect('');
		}
	}
