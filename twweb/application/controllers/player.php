<?php
	class Player extends CI_Controller {
		public function __construct() {
			parent::__construct();
		}

		public function index() {
			if($this->input->post('player_name') != '') {
				$this->load->model('players');

				if($playerId = $this->players->getIdByName($this->input->post('player_name'))) {
					redirect('player/view/'.$playerId);
				} else {
					$this->session->set_flashdata('error_message', 'Could not locate player by the name of "'.$this->input->post('player_name').'"');
					redirect('player/index');
				}
			}
				
			$this->load->view('include/header');
			$this->load->view('player_index');
			$this->load->view('include/footer');
		}

		public function view($playerId) {
			$this->load->model('profiles');

			if(!$profileData = $this->profiles->findById($playerId)) {
				$this->session->set_flashdata('error_message', 'Player does not have a profile.');
				redirect('profile/index');
			}

			$this->load->view('include/header');
			$this->load->vieW('player_view', array('profileData' => $profileData));
			$this->load->vieW('include/footer');
		}

		public function edit() {
		}
	}
