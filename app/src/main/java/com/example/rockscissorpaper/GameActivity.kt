package com.example.rockscissorpaper

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rockscissorpaper.databinding.ActivityGameBinding
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.log
import kotlin.random.Random

var ROCK:Int=2
var PAPER:Int=3
var SCISSOR:Int=1


class GameActivity : AppCompatActivity() {

    lateinit var binding:ActivityGameBinding
    var db= Firebase.firestore
    lateinit var gameRef: DocumentReference
    var gameCompleted:Boolean=false

    var joinedPlayer=false
    var isBottomImageSet=false
    var isTopImageSet=false

    var isFinalWinnerSet=false
    var noOfGamesCompleted:Int=0

    var handlerAnimation= Handler()

    var TAG="GAME_ACTIVITY"

    var runnable = object : Runnable {
        override fun run() {

            binding.pulseView2.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(1000)
                .withEndAction {
                    binding.pulseView2.scaleX = 1f
                    binding.pulseView2.scaleY = 1f
                    binding.pulseView2.alpha = 1f
                }

            binding.pulseView3.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(700)
                .withEndAction {
                    binding.pulseView3.scaleX = 1f
                    binding.pulseView3.scaleY = 1f
                    binding.pulseView3.alpha = 1f
                }

            handlerAnimation.postDelayed(this, 1500)
        }
    }

    var time:Int=6
    var handlerTimer:Handler= Handler()
    var exitRunnable= object : Runnable {
        override fun run() {
            if(time==0){
                handlerTimer.removeCallbacks(this)
                finish()
            }
            time--
            binding.gameStatusTv.text= "Other Player Quit, Returning to HomeScreen in $time sec"
            handlerTimer.postDelayed(this,1000)
        }
    }

    override fun onStop() {
        super.onStop()
        var game= gameModel.value
        game?.gameStatus=GameStatus.EXIT
        if (game != null) {
            Log.d(TAG, "onStop: saveGameModel")
            saveGameModel(game)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        joinedPlayer=intent.getBooleanExtra("Joined",false)

        setObservers()
        setListeners()
    }

    fun setListeners(){

        binding.rockBtnCv.setOnClickListener({
            Log.d(TAG, "testing setListeners: gamecompleted: "+ gameCompleted)
            if(gameCompleted){
                restartGame(ROCK)
            }
            else {
                var game = gameModel.value
                game?.gameStatus = GameStatus.INPROGRESS
                if (joinedPlayer) {
                    game?.joinedPlayerMove = ROCK
                } else {
                    game?.roomOwnerPlayerMove = ROCK
                }
                setBtnColrs(ROCK)
                if (game != null) {
                    Log.d(TAG, "setListeners: savegamemodel rock")
                    saveGameModel(game)
                }
            }
        })

        binding.paperBtnCv.setOnClickListener({
            if(gameCompleted){
                restartGame(PAPER)
            }
            else {
                var game = gameModel.value
                game?.gameStatus = GameStatus.INPROGRESS
                if (joinedPlayer) {
                    game?.joinedPlayerMove = PAPER
                } else {
                    game?.roomOwnerPlayerMove = PAPER
                }
                setBtnColrs(PAPER)
                if (game != null) {
                    Log.d(TAG, "setListeners: savegamemodel paper")
                    saveGameModel(game)
                }
            }
        })

        binding.scissorBtnCv.setOnClickListener({
            if(gameCompleted){
                restartGame(SCISSOR)
            }
            else {
                var game = gameModel.value
                game?.gameStatus = GameStatus.INPROGRESS
                if (joinedPlayer) {
                    game?.joinedPlayerMove = SCISSOR
                } else {
                    game?.roomOwnerPlayerMove = SCISSOR
                }
                setBtnColrs(SCISSOR)
                if (game != null) {
                    Log.d(TAG, "setListeners: savegamemodel scissor")
                    saveGameModel(game)
                }
            }
        })

        binding.randomIv.setOnClickListener({
            var i = Random.nextInt(1, 4)
            if(gameCompleted){
                restartGame(i)
            }
            else {
                var game = gameModel.value
                game?.gameStatus = GameStatus.INPROGRESS
                if (joinedPlayer)
                    game?.joinedPlayerMove = i
                else
                    game?.roomOwnerPlayerMove = i
                setBtnColrs(i)
                if (game != null) {
                    Log.d(TAG, "setListeners: savegamemodel random")
                    saveGameModel(game)
                }
            }
        })

        /*binding.playAgainBtn.setOnClickListener({
            restartGame()
        })*/

    }

    fun restartGame(move:Int){

        setBtnColrs(move)
        Log.d(TAG, "testing restartGame: move: "+ move)
        var gameModel= gameModel.value
        gameModel?.gameStatus=GameStatus.INPROGRESS
        gameModel?.winner=""
        gameModel?.noOfRounds = gameModel?.noOfRounds!! + 1
        if(joinedPlayer) {
            gameModel?.joinedPlayerMove = move
            gameModel?.roomOwnerPlayerMove=-1
        }
        else {
            gameModel?.roomOwnerPlayerMove = move
            gameModel?.joinedPlayerMove=-1
        }


        Log.d(TAG, "restartGame: savegamemodel")
        saveGameModel(gameModel)

    }

    fun setObservers(){
        gameModel.observe(this,{
            setUI()
        })

        fetchGameModel(gameModel.value!!.gameId)
    }

    fun setUI(){
        Log.d("TAG", "setUI: in game: "+ gameModel.value.toString())
        if(gameModel.value?.gameStatus?.equals(GameStatus.INPROGRESS) == true ) {
            binding.joinedPlayerImageContainer.visibility=View.INVISIBLE
            binding.roomOwnerPlayerImageContainer.visibility=View.INVISIBLE
            isBottomImageSet=false
            isTopImageSet=false
            gameCompleted = false
        }
        if(gameModel.value?.gameStatus?.equals(GameStatus.EXIT) == true)
            exitGame()

        if(noOfGamesCompleted >= gameModel.value?.maxRounds!!) {
            setCompletedWinner()
        }


        if(!gameCompleted) {
            if (gameModel.value?.roomOwnerPlayerMove != -1) {
                Log.d(TAG, "from setUI: setRoomPlayerMove")
                setRoomPlayerMove(gameModel.value?.roomOwnerPlayerMove)
            }
            if(gameModel.value?.joinedPlayerMove!=-1)
                setJoinedPlayerMove(gameModel.value?.joinedPlayerMove)
//            if(gameModel.value?.winner != null && gameModel.value?.winner?.isNotEmpty() == true)
//                setWinner()


            binding.gameStatusTv.text= when(gameModel.value?.gameStatus){
                    GameStatus.CREATED->{
                        setBtnsEnabled(false)
                        setBtnColrs(0)
                        "Game ID: "+ gameModel.value?.gameId
                    }
                    GameStatus.JOINED->{
                        binding.playAgainBtn.visibility=View.INVISIBLE
                        binding.joinedPlayerImageContainer.visibility=View.INVISIBLE
                        binding.roomOwnerPlayerImageContainer.visibility=View.INVISIBLE
                        binding.loadingAnimationContainer.visibility=View.INVISIBLE


                        isTopImageSet=false
                        isBottomImageSet=false
                        gameCompleted=false

                        setBtnsEnabled(true)
                        "Choose any to start game"
                    }
                    GameStatus.INPROGRESS->{
                        "waiting for the move"
                    }
                    GameStatus.FINISHED->{
                        Log.d(TAG, "testing setUI: finished")
                        gameCompleted=true
                        setScoreProgress()
                        setBtnColrs(0)
                        setBtnsEnabled(true)

                        //binding.playAgainBtn.visibility=View.VISIBLE

//                        Handler().postDelayed({restartGame()},1000)
                        setCompletedWinner()

                    }

                GameStatus.EXIT->{
                    exitGame()
                    ""
                }

                else -> {""}

            }

            binding.roundNoTv.text="Round "+ gameModel.value?.noOfRounds
            binding.playerScorecardTv.text= gameModel.value?.roomOwnerPlayerName+": "+ gameModel.value?.roomOwnerPlayerWon+ "\n"+ gameModel.value?.joinedPlayerName+": "+ gameModel.value?.joinedPlayerWon+"\nDraw: "+ gameModel.value?.draw

        }

    }

    fun setCompletedWinner():String{
        var completeWinner=""

        if((joinedPlayer && gameModel.value?.winner.equals(gameModel.value?.joinedPlayerName)
                    || !joinedPlayer && gameModel.value?.winner.equals(gameModel.value?.roomOwnerPlayerName)) )
            completeWinner=gameModel.value?.winner +"(you) WON"
        else if(gameModel.value?.winner.equals("It's Draw"))
            completeWinner=gameModel.value?.winner.toString()
        else
            completeWinner=gameModel.value?.winner+ " WON"


        var gamesdone= gameModel.value?.roomOwnerPlayerWon!! + gameModel.value?.joinedPlayerWon!! + gameModel.value?.draw!!
        Log.d(TAG, "setCompletedWinner: gamesDone: "+ gamesdone+ " max: "+ gameModel.value?.maxRounds)
        if(!isFinalWinnerSet && (gamesdone >= gameModel.value?.maxRounds!!)) {
//            var completeWinner = binding.gameStatusTv.text.toString()

            if (gameModel.value?.roomOwnerPlayerWon!! > gameModel.value?.joinedPlayerWon!!)
                completeWinner += "\nFinal winner: " + gameModel.value?.roomOwnerPlayerName
            else if (gameModel.value?.roomOwnerPlayerWon!! < gameModel.value?.joinedPlayerWon!!)
                completeWinner += "\nFinal winner: " + gameModel.value?.joinedPlayerName
            else
                completeWinner += "\nDraw"

            setBtnsEnabled(false)
            isFinalWinnerSet=true
//            binding.gameStatusTv.text = completeWinner
        }

        return completeWinner
    }

    fun exitGame(){
        exitRunnable.run()
    }

    fun computeResult(){

        var game= gameModel.value!!
        var roomOwnerPlayerMove= game.roomOwnerPlayerMove
        var joinedPlayerMove= game.joinedPlayerMove
        var whoWon=0 //0 draw 1 creator 2 joined

            game.winner= if(roomOwnerPlayerMove==joinedPlayerMove) {
                game.draw++; "It's Draw"
            }
            else if(joinedPlayerMove== PAPER && roomOwnerPlayerMove== SCISSOR) {
                game.roomOwnerPlayerWon++
                game.roomOwnerPlayerName
//                game.roomOwnerPlayerName+ ( if(!joinedPlayer) "(you) WON" else " WON")
            }
            else if(joinedPlayerMove== SCISSOR && roomOwnerPlayerMove== PAPER) {
                game.joinedPlayerWon++
                game.joinedPlayerName
//                game.joinedPlayerName+ if(joinedPlayer) "(you) WON" else " WON"
            }
            else if(joinedPlayerMove > roomOwnerPlayerMove) {
                game.joinedPlayerWon++;
                game.joinedPlayerName
//                game.joinedPlayerName+ if(joinedPlayer) "(you) WON" else " WON"
            }
            else {
                game.roomOwnerPlayerWon++;
                game.roomOwnerPlayerName
//                game.roomOwnerPlayerName+ if(!joinedPlayer) "(you) WON" else " WON"
            }


        game.gameStatus=GameStatus.FINISHED
        Log.d(TAG, "computeResult: savegamemodel")
        saveGameModel(game)

    }

    fun setScoreProgress(){
        Log.d(TAG, "setScoreProgress: gameModel: "+ gameModel.value)
        var roomOwnerPlayerWon = gameModel.value?.roomOwnerPlayerWon
        var joinedPlayerWon= gameModel.value?.joinedPlayerWon
        var draw= gameModel.value?.draw
        var noOfRounds= gameModel.value?.noOfRounds

            val wonPercentage = if(joinedPlayer) (joinedPlayerWon!! * 100 / noOfRounds!!) else ( roomOwnerPlayerWon!! * 100 / noOfRounds!!)
            val drawPercentage = (draw!! * 100 / noOfRounds)
            val losePercentage = if(joinedPlayer) (roomOwnerPlayerWon!! * 100 /noOfRounds) else (joinedPlayerWon!! * 100 / noOfRounds)

            if (losePercentage > 0)
                binding.scoreProgressBar.setBackgroundColor(getColor(R.color.lose_color))
            else
                binding.scoreProgressBar.setBackgroundColor(getColor(R.color.progress_bar_bg_color))

            Log.d("TAG", "updateUI: won:" + wonPercentage + " draw: " + drawPercentage + " lose: " + losePercentage
            )

            binding.scoreProgressBar.setMax(100)
            binding.scoreProgressBar.setSecondaryProgress(wonPercentage + drawPercentage)
            binding.scoreProgressBar.setProgress(wonPercentage)
    }

    fun setRoomPlayerMove(move:Int?){
        Log.d(TAG, "from in setRoomPlayerMove: "+  move)
        if(joinedPlayer && isBottomImageSet){
            stopPulseAnimation()
            when (move) {
                1 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.scissors1))
                }

                2 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.rock1))
                }

                3 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.paper1))
                }
            }
            animateImageView(true)
            isTopImageSet=true
        }
        else if(!joinedPlayer && !isBottomImageSet){
//            binding.roomOwnerPlayerImageContainer.visibility=View.VISIBLE
            when (move) {
                1 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.scissors2))
                }

                2 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.rock2))
                }

                3 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.paper2))
                }
            }
            isBottomImageSet=true
            animateImageView(false)
            if(gameModel.value?.joinedPlayerMove!=-1 && !isTopImageSet){
                setJoinedPlayerMove(gameModel.value?.joinedPlayerMove)
            }
            else{
                startPulseAnimation()
            }
//            startPulseAnimation()
        }
    }

    fun setJoinedPlayerMove(move:Int?){
        if(joinedPlayer && !isBottomImageSet){
//            binding.roomOwnerPlayerImageContainer.visibility=View.VISIBLE
            when (move) {
                1 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.scissors2))
                }

                2 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.rock2))
                }

                3 -> {
                    binding.roomOwnerPlayerImage.setImageDrawable(getDrawable(R.drawable.paper2))
                }
            }

            animateImageView(false)
//            startPulseAnimation()
            isBottomImageSet=true

            if(gameModel.value?.roomOwnerPlayerMove!=-1){
                setRoomPlayerMove(gameModel.value?.roomOwnerPlayerMove)
            }
            else{
                startPulseAnimation()
            }
        }
        else if(!joinedPlayer && isBottomImageSet){
            stopPulseAnimation()
//            binding.joinedPlayerImageContainer.visibility=View.VISIBLE
            when (move) {
                1 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.scissors1))
                }

                2 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.rock1))
                }

                3 -> {
                    binding.joinedPlayerImage.setImageDrawable(getDrawable(R.drawable.paper1))
                }
            }
            isTopImageSet=true
            animateImageView(true)
        }
    }

    fun startPulseAnimation() {
        binding.loadingAnimationContainer.visibility=View.VISIBLE
        binding.joinedPlayerImageContainer.visibility=View.INVISIBLE
        runnable.run()
    }

    private fun stopPulseAnimation() {
        binding.loadingAnimationContainer.visibility=View.INVISIBLE

        handlerAnimation.removeCallbacks(runnable)

    }

    fun animateImageView(isTop: Boolean){
        if(isTop && !isTopImageSet || !joinedPlayer || !isTop) {
            if (!isTop) binding.roomOwnerPlayerImageContainer.visibility =
                View.VISIBLE else binding.joinedPlayerImageContainer.visibility = View.VISIBLE
            // Fade in and out
            val fadeInOut = ObjectAnimator.ofFloat(
                if (!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage,
                "alpha",
                0f,
                0f,
                1f
            )
            fadeInOut.setDuration(1000) // 2 seconds for a full fade in and out

            // Move from bottom to top
            val moveUp = ObjectAnimator.ofFloat(
                if (!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage,
                "translationY",
                if (!isTop) 800f else -800f,
                0f
            )
            moveUp.setDuration(2000) // 2 seconds to move up

            // Scale up the image
            val scaleX = ObjectAnimator.ofFloat(
                if (!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage,
                "scaleX",
                0f,
                1f
            )
            val scaleY = ObjectAnimator.ofFloat(
                if (!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage,
                "scaleY",
                0f,
                1f
            )
            scaleX.setDuration(1000)
            scaleY.setDuration(1000)

            // Combine animations
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeInOut, moveUp, scaleX, scaleY)
            animatorSet.start()


                if (gameModel.value?.roomOwnerPlayerMove != -1 && gameModel.value?.joinedPlayerMove != -1 && isBottomImageSet && isTopImageSet && gameModel.value?.gameStatus != GameStatus.FINISHED)
                    computeResult()
        }
    }

    fun setBtnColrs(i:Int){
        binding.paperBtnCv.setCardBackgroundColor(resources.getColor(R.color.transparent_color))
        binding.rockBtnCv.setCardBackgroundColor(resources.getColor(R.color.transparent_color))
        binding.scissorBtnCv.setCardBackgroundColor(resources.getColor(R.color.transparent_color))

        when(i){
            1-> binding.scissorBtnCv.setCardBackgroundColor(resources.getColor(R.color.btn_clicked_color))
            2-> binding.rockBtnCv.setCardBackgroundColor(resources.getColor(R.color.btn_clicked_color))
            3-> binding.paperBtnCv.setCardBackgroundColor(resources.getColor(R.color.btn_clicked_color))
        }

        setBtnsEnabled(false)
    }

    fun setBtnsEnabled(enable:Boolean){
        Log.d(TAG, "testing: setBtnsEnabled: "+ enable)

        binding.paperBtnCv.isEnabled=enable
        binding.rockBtnCv.isEnabled=enable
        binding.scissorBtnCv.isEnabled=enable
        binding.randomIv.isEnabled=enable
    }


    ///////////////////   FIRESTORE   //////////////////////////////////
    fun saveGameModel(gameModel: GameModel){
        Log.d(TAG, "saveGameModel: save: "+ gameModel.toString())

        _gameModel.value=gameModel

        if(!gameModel.gameId.equals("-1")){
            db.collection("games")
                .document(gameModel.gameId).set(gameModel)
        }
    }

    fun fetchGameModel(gameID:String){
        gameRef = db.collection("games").document(gameID)

        gameRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                // Handle the error
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Get the updated game state
                val game = documentSnapshot.toObject(GameModel::class.java)

                // Update the UI accordingly
                _gameModel.value=game

                Log.d(TAG, "fetchGameModel: gameModel: "+ game)
            }
        }


    }
}