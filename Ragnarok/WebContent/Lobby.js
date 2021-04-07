window.addEventListener("load", function (event) {

	// Initialisation de tous les IFrames
	var iFrameLobby = document.getElementById("ifram-lobby");
	var iFrameGame = document.getElementById("ifram-game");
	var iFrameMission = document.getElementById("ifram-gamebis-mission");
	var iFrameResultat = document.getElementById("ifram-gamebis-resultat");

    let pseudo = prompt("Veuillez saisir votre pseudo :");
    let wsChat = new WebSocket( "ws://localhost:8080/Ragnarok/chatroom/" + pseudo);
    let wsGame = new WebSocket( "ws://localhost:8080/Ragnarok/game/" + pseudo);
	
	let listeJoueurs = $("#ifram-lobby").contents().find('#player-grid span p');
    let txtHistory = iFrameLobby.contentWindow.document.getElementById( "history" );
    let txtMessage = iFrameLobby.contentWindow.document.getElementById( "txtMessage" );
    let startButton = iFrameLobby.contentWindow.document.getElementById("start-btn");
	let validerButton = iFrameGame.contentWindow.document.getElementById("btn-valider-choix");
	let validation = iFrameGame.contentWindow.document.getElementById("validation-part");
	let btnVoteOui = iFrameGame.contentWindow.document.getElementById("btn-oui");
	let btnVoteNon = iFrameGame.contentWindow.document.getElementById("btn-non");
	let annonceRes = $("#ifram-gamebis-resultat").contents().find('#resultat h1');
	let cptS = id = nbJoueurs = nbChoisis = nbCoches = 0;
	let premiereConnexion = true;
	let isCapitaine = sabote = false;
	let sc = $("#ifram-game").contents().find('#score-part span');
	let roundBox = $("#ifram-game").contents().find("#round-box");
	roundBox.text("Manche 1");
	
	btnVoteOui.addEventListener("click", function(){
		console.log("Oui");
		wsGame.send("Oui");
		affichageVote();
	});
	
	btnVoteNon.addEventListener("click", function(){
		console.log("Non");
		wsGame.send("Non");
		affichageVote();
	});
	
	let btnSend = iFrameLobby.contentWindow.document.getElementById( "btnSend" );
    initSend(btnSend);
	validation.style.display = "none";
	validerButton.style.display = "none";
	startButton.style.visibility = "hidden";
	afficherIFrame(iFrameLobby);
	txtMessage.focus();
	
	function hidel() {
	    $("#ifram-game").contents().find('.chat-game').slideToggle(1000);
 	}
    
    wsChat.addEventListener("open", function (evt) {
        console.log("Connection established");
    });

    wsChat.addEventListener( "message", function( evt ) {
        let message = evt.data;
        console.log( "Receive new message: " + message );
        txtHistory.value += message + "\n";
    });

    wsChat.addEventListener( "close", function( evt ) {
        console.log( "Connection closed" );
    });
	
	wsGame.addEventListener( "message", function( evt ) {
		if(evt.data === "Start"){
			afficherIFrame(iFrameGame);
			cacherIFrame(iFrameLobby);
			txtHistory = iFrameGame.contentWindow.document.getElementById( "history" );
    	    txtMessage = iFrameGame.contentWindow.document.getElementById( "txtMessage" );
			btnSend = iFrameGame.contentWindow.document.getElementById( "btnSend" );
			initSend(btnSend);
			let listeGame = $("#ifram-game").contents().find('#game-player-grid span label p');
			let listeImg = $("#ifram-game").contents().find('#game-player-grid span label img');
            let cpt=0;
            for(let i = 0; i<listeJoueurs.length;i++){
                listeGame.eq(i).text(listeJoueurs.eq(i).text());
                if(listeJoueurs.eq(i).text() === " "){
                    cpt++;
                }
            }
            for(let i = (10-cpt); i<listeJoueurs.length;i++){
                listeImg.eq(i).css("display","none");
            }
			nbJoueurs = 10-cpt;
			manche1();
		}
		
		if(evt.data.startsWith("role")){
			console.log("role");
			getListeRole(evt);
		}
		if(evt.data.startsWith("capitaine")){
			getCapitaine(evt);
		}
		if(evt.data.startsWith("composition")){
			let choisis = evt.data.split("/");
			afficherChoisis(choisis);
		}
		if(evt.data === "Restart"){
			renouvelleAura();
			SabotageYes.style.display = "";
			SabotageNo.style.display = "";
		}
		missionGentil = iFrameMission.contentWindow.document.getElementById( "mission-gentil" );
        missionTraitre = iFrameMission.contentWindow.document.getElementById( "mission-traitre" );

        if (evt.data === "TraitreChoixPage") {
	        cacherIFrame(iFrameGame);
            afficherIFrame(iFrameMission);
            missionTraitre.style.visibility = "";
            missionGentil.style.visibility = "hidden";
			lancerTimer();
        }

        if (evt.data === "GentilsChoixPage") {
			cacherIFrame(iFrameGame);
            afficherIFrame(iFrameMission);
            missionGentil.style.visibility = "";
            missionTraitre.style.visibility = "hidden";
			SabotageYes.style.display = "none";
			SabotageNo.style.display = "none";
			lancerTimer();
            
        }
        //affichage resultat d un round
        if(evt.data === "ReturnGameSabote"){
        	cacherIFrame(iFrameMission);
            afficherIFrame(iFrameGame);
			cptS++;
			nextManche(cptS);
			sc.eq(cptS).html("&#10005;");
		}
		if(evt.data === "ReturnGamePasSabote"){
			cacherIFrame(iFrameMission);
            afficherIFrame(iFrameGame);
			cptS++;
			nextManche(cptS);
			sc.eq(cptS).html("&#10003;");
		}
        //cas affichage resultat
		if(evt.data === "GentilWin"){
			afficherIFrame(iFrameResultat);
			cacherIFrame(iFrameGame);
			cacherIFrame(iFrameMission);
			annonceRes.html("Victoire<br>Vous avez reussi vos missions");
		}
		if(evt.data === "GentilLose"){
			afficherIFrame(iFrameResultat);
			cacherIFrame(iFrameGame);
			cacherIFrame(iFrameMission);
			annonceRes.html("Defaite <br> Les traitres ont sabotes vos missions");
		}
		if(evt.data === "TraitreWin"){
			afficherIFrame(iFrameResultat);
			cacherIFrame(iFrameGame);
			cacherIFrame(iFrameMission);
			annonceRes.html("Victoire <br> Vous avez sabote assez de missions");
		}
		if(evt.data === "TraitreLose"){
			afficherIFrame(iFrameResultat);
			cacherIFrame(iFrameGame);
			cacherIFrame(iFrameMission);
			annonceRes.html("Defaite <br> Les gentils ont reussi leurs missions");
		}
		else{
			updateListeJoueurs(evt);
			premiereConnexion = false;
		}
	});
	
	// Ma partie de code (William)
    let SabotageYes = iFrameMission.contentWindow.document.getElementById('saboter-oui');
	let SabotageNo = iFrameMission.contentWindow.document.getElementById('saboter-non');
	
    SabotageYes.addEventListener("click", function() {
        wsGame.send("Sabotage");
		SabotageYes.style.display = "none";
		SabotageNo.style.display = "none";
		sabote = true;
    })

    SabotageNo.addEventListener("click", function() {
        wsGame.send("PasSabotage");
		SabotageYes.style.display = "none";
		SabotageNo.style.display = "none";
		sabote = true;
    })
	
	function nextManche(cptS){
		switch(cptS){
			case 1 : 
				manche2();
				break;
			case 2 : 
				manche3();
			    break;
			case 3 :
				manche4();
				break;
			case 4 : 
				manche5();
				break;
			default :
				break;
		}
	}
	
	function lancerTimer(){
		var index = 0;
            var timeleft = 10;
            const slides = iFrameMission.contentWindow.document.querySelectorAll(".slides");
            const classHide = "slides-hidden", count = slides.length;
            nextSlide();

            function nextSlide() {
                slides[(index ++) % count].classList.add(classHide);
                slides[index % count].classList.remove(classHide);
                         
                if (index==2){
                    return false;
                }
                setTimeout(nextSlide, 5000);
            }

            var downloadTimer = setInterval(function(){
                if(timeleft <= 0){
                  clearInterval(downloadTimer);
                  iFrameMission.contentWindow.document.getElementById("countdown").innerHTML = "Finished";
				  if(!sabote){
					SabotageNo.click();
				  }
				  sabote = false;
                } else {
                  iFrameMission.contentWindow.document.getElementById("countdown").innerHTML = timeleft ;
                }
                timeleft -= 1;
            }, 1000 + (id*5));

	}
	
	function updateListeJoueurs(evt){
		let listePseudos = evt.data.split('/');
			let compteur = 0;
			for(let i = 0; i<listePseudos.length ; i++){
				listeJoueurs.eq(i).text(listePseudos[i]);
				compteur = i;
			}
			for(let j = compteur + 1; j <listeJoueurs.length; j++){
				listeJoueurs.eq(j).text(" ");
			}
		    if (listePseudos.length >= 5) { // mettre 10 quand le projet est fini
	            startButton.style.visibility = "visible";
	        }
	        else{
	        	startButton.style.visibility = "hidden"; 
			}
			if(premiereConnexion){
				id = listePseudos.length;
			}

	}
	
	//obtenir le(s) role(s) et les afficher sur page web 
	let listeRolesJoueurs = $("#ifram-game").contents().find('#game-player-grid span label h1');
	let listeAura = $("#ifram-game").contents().find('#game-player-grid span label .game-player-picture');
	function getListeRole(evt){
		let listeRole = evt.data.split('/');
		if(listeRole[1]=="Gentil"){
			listeRolesJoueurs.eq(parseInt(listeRole[2])-1).text(listeRole[1]);
			listeAura.eq(parseInt(listeRole[2])-1).css("filter","drop-shadow(0 0 10px #bbbbbb)");
		}else{
			let k = 2;
			let a = 1;
			for(let i=0;i<listeRole.length;i++){
				listeRolesJoueurs.eq(parseInt(listeRole[k])-1).text(listeRole[a]);
				listeAura.eq(parseInt(listeRole[k])-1).css("filter","drop-shadow(0 0 10px #be2424)");
				k=k+2;
				a=a+2;
			}
		}
	}
	
	validerButton.addEventListener( "click", function() {
		let listeChoisis = $("#ifram-game").contents().find("input:checked");
		let composition = "composition/"
		listeChoisis.each(function(){
			let parent = $(this).attr("value");
			composition += parent + "/";
			$(this).prop( "checked", false );
		});		
		wsGame.send(composition);
		nbCoches = 0;
		boutonValider();
    });
    
	// Permet de rejouer une partie

    let btnRejouer = iFrameResultat.contentWindow.document.getElementById('btn-rejouer');
	btnRejouer.addEventListener("click", function(){
		location.reload();
	});
	
	function manche1(){
		switch(nbJoueurs){
			case 5: 
				nbChoisis = 2;
				break;
			case 6:
				nbChoisis = 2; 
				break;
			case 7: 
				nbChoisis = 2;
				break;
			case 8: 
				nbChoisis = 3;
				break;
			case 9: 
				nbChoisis = 3;
				break;
			case 10: 
				nbChoisis = 3;
				break;
			default:
				break;
			
		}
	}
	
	function manche2(){
		roundBox.text("Manche 2");
		switch(nbJoueurs){
			case 5: 
				nbChoisis = 3;
				break;
			case 6: 
				nbChoisis = 3;
				break;
			case 7: 
				nbChoisis = 3;	
				break;
			case 8: 
				nbChoisis = 4;
				break;
			case 9: 
				nbChoisis = 4;
				break;
			case 10: 
				nbChoisis = 4;;
			default:
				break;
			
		}
	}
	
	function manche3(){
		roundBox.text("Manche 3");
		switch(nbJoueurs){
			case 5: 
				nbChoisis = 2;
				break;
			case 6: 
				nbChoisis = 4;
				break;
			case 7:
				nbChoisis = 3; 
				break;
			case 8: 
				nbChoisis = 4;
				break;
			case 9: 
				nbChoisis = 4;
				break;
			case 10: 
				nbChoisis = 4;
				break;
			default:
				break;
			
		}
	}
	
	function manche4(){
		roundBox.text("Manche 4");
		switch(nbJoueurs){
			case 5: 
				nbChoisis = 3; 
				break;
			case 6: 
				nbChoisis = 3; 
				break;
			case 7: 
				nbChoisis = 4; 
				break;
			case 8:
				nbChoisis = 5; 
				break;
			case 9: 
				nbChoisis = 5; 
				break;
			case 10: 
				nbChoisis = 5; 
				break;
			default:
				break;
			
		}
	}
	
	function manche5(){
		roundBox.text("Manche 5");
		switch(nbJoueurs){
			case 5: 
				nbChoisis = 3; 
				break;
			case 6: 
				nbChoisis = 4; 
				break;
			case 7: 
				nbChoisis = 4; 
				break;
			case 8: 
				nbChoisis = 5; 
				break;
			case 9: 
				nbChoisis = 5; 
				break;
			case 10: 
				nbChoisis = 5; 
				break;
			default:
				break;
			
		}
	}	
	
	
	// Empeche que le capitaine selectionne plus de personnes que nécessaire
	
	$("#ifram-game").contents().find(":checkbox").on('change', function(){
		if(isCapitaine){
			if(this.checked){
				nbCoches++;
				if(nbCoches>nbChoisis){
					nbCoches--;
					$(this).prop( "checked", false );
				}
			}
			else nbCoches--;
			boutonValider();
		}
		else $(this).prop( "checked", false );
	});
	
	//supprimer Aura de tous les joueurs
	function renouvelleAura(){
		listeAura.each(function() {
			$(this).css("filter","none");
		});
	}
	
	// Affiche le bouton valider pour le capitaine quand il a selectionné assez de personnes
	function boutonValider(){
		if(nbCoches==nbChoisis){
			validerButton.style.display = ""; 
		}
		else validerButton.style.display = "none";
	}
	
	// Affiche pour tout le monde la composition choisie en vert
	function afficherChoisis(choisis){
		for(let i = 1; i<choisis.length ; i++){
			if( 0 < parseInt(choisis[i]) <= 10){
				listeAura.eq(parseInt(choisis[i]-1)).css("filter","drop-shadow(0 0 10px #338121)");
			}
		}
		affichageVote();
	} 
	
	// Change la visibilité du bloc de validation
	function affichageVote(){
		if(validation.style.getPropertyValue('display') == ""){
			validation.style.display = "none";
		}
		else validation.style.display = "";
	}
		
	// Affiche le capitaine pour tout le monde et permet à la session en question de s'identifier en tant que tel
	function getCapitaine(evt){
		let capitaine = evt.data.split('/');
		console.log(capitaine[1]);
		if(capitaine[1] == id){
			isCapitaine = true;
		}
		else isCapitaine = false;
		listeAura.eq(parseInt(capitaine[1]-1)).css("filter","drop-shadow(0 0 10px #ffd103)");
		console.log(isCapitaine);
	}
	
	
	function initSend(btnSend){
		btnSend.addEventListener( "click", function() {
	        wsChat.send( txtMessage.value );
	        txtMessage.value = "";
	        txtMessage.focus();
    	});
	}
	
	let btnStart = iFrameLobby.contentWindow.document.getElementById('start-btn');
	btnStart.addEventListener("click", function(){
		wsGame.send("Start");
	});
	
});

	function afficherIFrame(iframe){
		iframe.style.display = "";	
	}
	
	function cacherIFrame(iframe){
		iframe.style.display = "none";
	}
