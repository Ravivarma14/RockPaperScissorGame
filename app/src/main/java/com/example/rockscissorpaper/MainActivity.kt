package com.example.rockscissorpaper

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rockscissorpaper.databinding.ActivityMainBinding
import com.example.rockscissorpaper.databinding.GameidNameDialogBinding
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class MainActivity : AppCompatActivity() {


    lateinit var binding:ActivityMainBinding
    var db=Firebase.firestore
    lateinit var gameRef:DocumentReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setObservers()
        setListenres()
    }

    fun setObservers(){
        gameModel.observe(this,{
            setUI()
        })
    }

    fun setUI(){
        Log.d("TAG", "setUI: "+ gameModel.value.toString())
    }

    fun setListenres(){

        binding.offlineBtn.setOnClickListener({

            var gameId = "-1"

            var gameModel = GameModel(
                gameId = gameId,
                winner = "",
                gameStatus = GameStatus.JOINED,
                roomOwnerPlayerMove = -1,
                joinedPlayerMove = -1,
                roomOwnerPlayerName = "Ravivarma",
                joinedPlayerName = "Computer"
            )

            saveofflineGameModel(gameModel)

            val intent=Intent(this, GameActivityOffline::class.java)
            startActivity(intent)
        })

        binding.onlineCreateBtn.setOnClickListener({
            createDialogFor(true)

        })

        binding.onlineJoinBtn.setOnClickListener({
            createDialogFor(false)
        })
    }

    ///////////// ALERT DIALOG //////////////////
    lateinit var dialogBinding:GameidNameDialogBinding

    fun createDialogFor(toCreate:Boolean){

        // Inflate the custom layout
        dialogBinding= GameidNameDialogBinding.inflate(layoutInflater)

        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if(toCreate){
            dialogBinding.roomIdCv.visibility=View.GONE

            // Handle button click inside the custom layout
            dialogBinding.dialogStartBtn.setOnClickListener {
                val maxRounds= dialogBinding.etMaxRounds.text.toString()
                val playerName= dialogBinding.etPlayerName.text


                var gameId = Random.nextInt(1000, 9999).toString()

                //Creating and storing game object into firebase
                var gameModel = GameModel(
                    gameId = gameId,
                    winner = "",
                    gameStatus = GameStatus.CREATED,
                    roomOwnerPlayerMove = -1,
                    joinedPlayerMove = -1,
                    roomOwnerPlayerName = playerName.toString(),
                    joinedPlayerName = "",
                    roomOwnerPlayerWon = 0,
                    joinedPlayerWon = 0,
                    maxRounds = maxRounds.toInt()
                )

                saveGameModel(gameModel)

                //firebase data listener
                fetchGameModel(gameModel.gameId)
                val intent=Intent(this,GameActivity::class.java)
                startActivity(intent)
                alertDialog.dismiss()
            }
        }
        else{

            dialogBinding.maxRoundsCv.visibility=View.GONE

            // Handle button click inside the custom layout
            dialogBinding.dialogStartBtn.setOnClickListener {

                val gameId= dialogBinding.etRoom.text.toString()
                val playerName= dialogBinding.etPlayerName.text.toString()


                if(gameId.isEmpty() || playerName.isEmpty()){
                        dialogBinding.etRoom.setError("Please enter game ID and player name")
                    } else{
                    showProgressDialog(this)

                        db.collection("games")
                            .document(gameId).get().addOnSuccessListener {
                                val model= it?.toObject(GameModel::class.java)

                                if(model==null){
                                    stopLoader()
                                    Toast.makeText(this,"Please enter valid Game ID", Toast.LENGTH_SHORT).show()
                                } else{
                                    model.gameStatus=GameStatus.JOINED
                                    model.joinedPlayerName=playerName
                                    saveGameModel(model)

                                    //firebase data listener
                                    fetchGameModel(gameId)

                                    stopLoader()
                                    val intent=Intent(this,GameActivity::class.java)
                                    intent.putExtra("Joined",true)
                                    startActivity(intent)
                                }
                            }
                            .addOnFailureListener { Toast.makeText(this,"Can't connect to room right now.", Toast.LENGTH_SHORT).show()
                                stopLoader()
                            }

                    }

                alertDialog.dismiss()
            }
        }

        alertDialog.show()
    }

    lateinit var dialog: Dialog
    fun showProgressDialog(context: Context) {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_circular)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun stopLoader(){
        if(dialog!=null)
            dialog.dismiss()
    }

    ///////////// GAME MODELS FIREBASE /////////////////////

    fun saveofflineGameModel(offlineGameModel: GameModel){
        _offlineGameModel.value=offlineGameModel
    }

    fun saveGameModel(gameModel: GameModel){
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
            }
        }


    }
}