<!DOCTYPE html>
<html lang="en">
  	<head>
		<script type="text/javascript" src="/js/ddtabmenu.js">
		/***********************************************
		* DD Tab Menu script- © Dynamic Drive DHTML code library (www.dynamicdrive.com)
		* This notice MUST stay intact for legal use
		* Visit Dynamic Drive at http://www.dynamicdrive.com/ for full source code
		***********************************************/
		</script>
		
		<link rel="stylesheet" type="text/css" href="/css/main.css" />

		<script type="text/javascript">
			ddtabmenu.definemenu("nav_menu", <?=isset($this->tab_num) ? $this->tab_num : -1;?>);
		</script>
  	</head>
  	<body>
		<div id="container">
			<div id="header" style="width: 100%; padding-top: 10px;">
				<div style="float:left; height: 50px;">
					<img src="/img/tw_icon.png" />
				</div>
				<div style="float: right; height: 20px; color: #FFF; padding-top:30px;">
			        <?if($this->player_security->isLoggedIn()) {?>
		    	        Welcome <?=$this->session->userdata('player_name');?>! (<a href="<?=base_url();?>home/logout" />logout</a>)
			        <?} else {?>
			        	<a href="/home/login">Login!</a>
					<?}?>	
				</div>
		  	</div>
				
			<div style="clear: both;"></div>
                <div id="nav_menu" class="chromemenu">
                    <ul>
                        <li><a href="<?=base_url();?>"      > Home    </a></li>
                        <li><a href="<?=base_url();?>player"> Players </a></li>
                        <li><a href="<?=base_url();?>squad" > Squads  </a></li>
                    </ul>
                </div>

			<div id="content">

				<div style="clear:both;"></div>

				


		<?if($this->session->flashdata('message') != '') {?>
			<span style="font-weight: bold;"><?=$this->session->flashdata('message');?></span>
		<?}?>

        <?if($this->session->flashdata('error_message') != '') {?>
            <span style="font-weight: bold; color: #FF0000;"><?=$this->session->flashdata('error_message');?></span>
        <?}?>

	
  	
