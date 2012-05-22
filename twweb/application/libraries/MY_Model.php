<?php
	/* Model override for common tasks o)-<| */
	class MY_Model extends CI_Model {
		public $keyName   = NULL;
		public $tableName = NULL;

		public function __construct() {
			parent::__construct();
		}

		public function findAllBy($field, $value, $limit=50, $offset=0) {
			$res = $this->db->get_where($this->tableName, array($field => $value), $limit, $offset);
			return $res->result_array();
		}

		public function findOneBy($field, $value) {
			$res = $this->db->get_where($this->tableName, array($field => $value));
			return $res->row_array();
		}

		public function save($data) {
			if(isset($data[$this->keyName]) && trim($data[$this->keyName]) != '') {
				// we have a primary key value so this is an update
				$this->db->where($this->keyName, $data[$this->keyName]);
				$this->db->update($this->tableName, $data);
			} else {	
				// we do not have a key value so we create the a record
				if(isset($data[$this->keyName])) unset($data[$this->keyName]); // make sure no blank key is set.
				$this->db->insert($this->tableName, $data);
				return $this->db->insert_id();
			}
		}

		public function update($data) {
			return $this->save($data);
		}

		public function delete($id) {
			$this->db->where($this->keyName, $id);
			$this->db->delete($this->tableName);
		}
	}
