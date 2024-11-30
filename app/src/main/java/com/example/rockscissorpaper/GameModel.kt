package com.example.rockscissorpaper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class GameModel (
    var gameId: String ="-1",
    var winner: String = "",
    var gameStatus: GameStatus=GameStatus.CREATED,
    var roomOwnerPlayerMove: Int= -1,
    var joinedPlayerMove: Int=-1,
    var roomOwnerPlayerName: String = "",
    var joinedPlayerName: String = "",
    var noOfRounds: Int=1,
    var roomOwnerPlayerWon:Int=0,
    var joinedPlayerWon:Int=0,
    var draw:Int=0,
    var maxRounds:Int=1
)


var _gameModel: MutableLiveData<GameModel> = MutableLiveData()
var gameModel: LiveData<GameModel> = _gameModel

var _offlineGameModel: MutableLiveData<GameModel> = MutableLiveData()
var offlineGameModel: LiveData<GameModel> = _offlineGameModel

enum class GameStatus{
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED,
    EXIT,
    COMPLETED
}