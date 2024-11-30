package com.example.rockscissorpaper

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.location.GnssMeasurement
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
import kotlin.random.Random


class GameActivityOffline : AppCompatActivity() {

    lateinit var binding:ActivityGameBinding
    var db= Firebase.firestore
    lateinit var gameRef: DocumentReference
    var gameCompleted:Boolean=false

    var joinedPlayer=false
    var isBottomImageSet=false
    var isTopImageSet=false

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


        setObservers()
        setListeners()
    }

    fun setListeners(){

        binding.rockBtnCv.setOnClickListener({
            var game=offlineGameModel.value
            game?.gameStatus=GameStatus.INPROGRESS
            game?.roomOwnerPlayerMove= ROCK

            setBtnColrs(ROCK)
            if (game != null) {
                saveofflineGameModel(game)
            }
        })

        binding.paperBtnCv.setOnClickListener({
            var game=offlineGameModel.value
            game?.gameStatus=GameStatus.INPROGRESS
            game?.roomOwnerPlayerMove= PAPER

            setBtnColrs(PAPER)
            if (game != null) {
                saveofflineGameModel(game)
            }
        })

        binding.scissorBtnCv.setOnClickListener({
            var game=offlineGameModel.value
            game?.gameStatus=GameStatus.INPROGRESS
            game?.roomOwnerPlayerMove= SCISSOR

            setBtnColrs(SCISSOR)
            if (game != null) {
                saveofflineGameModel(game)
            }
        })

        binding.randomIv.setOnClickListener({
            var i= Random.nextInt(1,4)
            var game=offlineGameModel.value
            game?.gameStatus=GameStatus.INPROGRESS
            game?.roomOwnerPlayerMove=i

            setBtnColrs(i)
            if (game != null) {
                saveofflineGameModel(game)
            }
        })

        binding.playAgainBtn.setOnClickListener({
            restartGame()
        })

    }

    fun restartGame(){

        var offlineGameModel= offlineGameModel.value
        offlineGameModel?.gameStatus=GameStatus.JOINED
        offlineGameModel?.winner=""
        offlineGameModel?.noOfRounds = offlineGameModel?.noOfRounds!! + 1
        offlineGameModel?.roomOwnerPlayerMove=-1
        offlineGameModel?.joinedPlayerMove=-1

        saveofflineGameModel(offlineGameModel)

    }

    fun setObservers(){
        offlineGameModel.observe(this,{
            setUI()
        })

    }

    fun setUI(){
        Log.d("TAG", "setUI: in game: "+ offlineGameModel.value.toString())
        if(offlineGameModel.value?.gameStatus?.equals(GameStatus.JOINED) == true)
            gameCompleted=false

        if(!gameCompleted) {
            if (offlineGameModel.value?.roomOwnerPlayerMove != -1 && !isBottomImageSet) {
                Log.d(TAG, "from setUI: setRoomPlayerMove")
                setRoomPlayerMove(offlineGameModel.value?.roomOwnerPlayerMove)
            }
            if(offlineGameModel.value?.joinedPlayerMove!=-1)
                setJoinedPlayerMove(offlineGameModel.value?.joinedPlayerMove)

            binding.gameStatusTv.text= when(offlineGameModel.value?.gameStatus){
                    GameStatus.CREATED->{
                        "Game ID: "+ offlineGameModel.value?.gameId
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
                        ""
                    }
                    GameStatus.FINISHED->{
                        setScoreProgress()
                        setBtnsEnabled(false)
                        setBtnColrs(0)

                        binding.playAgainBtn.visibility=View.VISIBLE

                        if(offlineGameModel.value?.winner.equals(offlineGameModel.value?.roomOwnerPlayerName))
                            offlineGameModel.value?.winner +"(you) WON"
                        else if(offlineGameModel.value?.winner.equals("It's Draw"))
                            offlineGameModel.value?.winner
                        else
                            offlineGameModel.value?.winner+ " WON"
                    }
                GameStatus.EXIT->{
                    ""
                }

                else -> {""}

            }

            binding.roundNoTv.text="Round "+ offlineGameModel.value?.noOfRounds

            binding.playerScorecardTv.text= offlineGameModel.value?.roomOwnerPlayerName+": "+ offlineGameModel.value?.roomOwnerPlayerWon+ "\n"+ offlineGameModel.value?.joinedPlayerName+": "+ offlineGameModel.value?.joinedPlayerWon+"\nDraw: "+ offlineGameModel.value?.draw

        }

    }

    fun computeResult(){
        gameCompleted=true

        var game= offlineGameModel.value!!
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

        saveofflineGameModel(game)
    }

    fun setScoreProgress(){
        Log.d(TAG, "setScoreProgress: offlineGameModel: "+ offlineGameModel.value)
        var roomOwnerPlayerWon = offlineGameModel.value?.roomOwnerPlayerWon
        var joinedPlayerWon= offlineGameModel.value?.joinedPlayerWon
        var draw= offlineGameModel.value?.draw
        var noOfRounds= offlineGameModel.value?.noOfRounds

            val wonPercentage = ( roomOwnerPlayerWon!! * 100 / noOfRounds!!)
            val drawPercentage = (draw!! * 100 / noOfRounds)
            val losePercentage = (joinedPlayerWon!! * 100 / noOfRounds)

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

        startPulseAnimation()

        Handler().postDelayed({
            var i= Random.nextInt(1,4)
            var game= offlineGameModel.value
            game?.gameStatus= GameStatus.FINISHED
            if (move != null) {
                game?.joinedPlayerMove= i
            }

            if (game != null) {
                saveofflineGameModel(game)
            }
            },2000)
    }

    fun setJoinedPlayerMove(move:Int?){

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
            animateImageView(true)
            isTopImageSet=true

        computeResult()
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
        if(!isTop) binding.roomOwnerPlayerImageContainer.visibility=View.VISIBLE else binding.joinedPlayerImageContainer.visibility=View.VISIBLE
        // Fade in and out
        val fadeInOut = ObjectAnimator.ofFloat(if(!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage, "alpha", 0f, 0f, 1f)
        fadeInOut.setDuration(2000) // 2 seconds for a full fade in and out

        // Move from bottom to top
        val moveUp = ObjectAnimator.ofFloat(if(!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage, "translationY", if(!isTop) 800f else -800f, 0f)
        moveUp.setDuration(2000) // 2 seconds to move up

        // Scale up the image
        val scaleX = ObjectAnimator.ofFloat(if(!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(if(!isTop) binding.roomOwnerPlayerImage else binding.joinedPlayerImage, "scaleY", 0f, 1f)
        scaleX.setDuration(2000)
        scaleY.setDuration(2000)

        // Combine animations
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeInOut, moveUp, scaleX, scaleY)
        animatorSet.start()

        if(offlineGameModel.value?.roomOwnerPlayerMove!=-1 && offlineGameModel.value?.joinedPlayerMove!=-1 && isBottomImageSet && isTopImageSet)
            computeResult()
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
    }

    fun setBtnsEnabled(enable:Boolean){
        binding.paperBtnCv.isEnabled=enable
        binding.rockBtnCv.isEnabled=enable
        binding.scissorBtnCv.isEnabled=enable
        binding.randomIv.isEnabled=enable
    }


    ///////////////////   FIRESTORE   //////////////////////////////////
    fun saveofflineGameModel(offlineGameModel: GameModel){
        _offlineGameModel.value=offlineGameModel
    }
}