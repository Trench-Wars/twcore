<!DOCTYPE html>
<html lang="en">
  	<head>
  	</head>
  	<body>
		<?if($this->player_security->isLoggedIn()) {?>
			Welcome <?=$this->session->userdata('player_name');?>! (<a href="<?=base_url();?>home/logout" />logout</a>)
		<?} else {?>
			Welcome Guest! (<a href="<?=base_url();?>home/login">login</a>)
		<?}?>
		<br />

		<?if($this->session->flashdata('message') != '') {?>
			<span style="font-weight: bold;"><?=$this->session->flashdata('message');?></span>
		<?}?>
	
  	
