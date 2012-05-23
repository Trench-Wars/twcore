<!DOCTYPE html>
<html lang="en">
  	<head>
		<link rel="stylesheet" type="text/css" href="/css/main.css" />
  	</head>
  	<body>
		<div id="container">
			<div id="header" style="width: 100%; padding-top: 10px;">
				<div style="float:left; height: 50px;">
					<img src="/img/tw_icon.png" />
				</div>
				<div style="float: right; height: 50px; color: #FFF;">
			        <?if($this->player_security->isLoggedIn()) {?>
		    	        Welcome <?=$this->session->userdata('player_name');?>! (<a href="<?=base_url();?>home/logout" />logout</a>)
			        <?} else {?>
			        	<a href="/home/login">Login!</a>
					<?}?>	
				</div>
		  	</div>
				
			<div style="clear: both;"></div>

			<div id="content">


		<?if($this->session->flashdata('message') != '') {?>
			<span style="font-weight: bold;"><?=$this->session->flashdata('message');?></span>
		<?}?>
	
  	
