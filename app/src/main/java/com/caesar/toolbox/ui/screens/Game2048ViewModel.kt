package com.caesar.toolbox.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class Game2048ViewModel(context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("game2048", Context.MODE_PRIVATE)

    data class GameState(
        val board: List<List<Int>> = List(4) { List(4) { 0 } },
        val score: Int = 0,
        val bestScore: Int = 0,
        val history: List<Int> = emptyList(),
        val isGameOver: Boolean = false,
        val justMerged: Set<Pair<Int, Int>> = emptySet()
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    // --- 初始化 ---
    private fun loadState(): GameState {
        val json = prefs.getString("save", null) ?: return newGame()
        try {
            val obj = JSONObject(json)
            val board = (0 until 4).map { r ->
                val row = obj.getJSONArray("board").getJSONArray(r)
                (0 until 4).map { c -> row.getInt(c) }
            }
            val history = prefs.getString("history", "[]")!!.let {
                val arr = JSONArray(it); (0 until arr.length()).map { i -> arr.getInt(i) }
            }
            return GameState(
                board = board,
                score = obj.getInt("score"),
                bestScore = prefs.getInt("best", 0),
                history = history
            )
        } catch (_: Exception) { return newGame() }
    }

    private fun newGame(): GameState {
        val board = List(4) { MutableList(4) { 0 } }
        addRandom(board); addRandom(board)
        return GameState(
            board = board.map { it.toList() },
            bestScore = prefs.getInt("best", 0),
            history = prefs.getString("history", "[]")!!.let {
                val arr = JSONArray(it); (0 until arr.length()).map { i -> arr.getInt(i) }
            }
        )
    }

    fun reset() {
        val old = _state.value
        if (!old.isGameOver && old.score > 0) {
            saveHistory(old.score)
        }
        val board = List(4) { MutableList(4) { 0 } }
        addRandom(board); addRandom(board)
        _state.value = GameState(
            board = board.map { it.toList() },
            bestScore = prefs.getInt("best", 0),
            history = prefs.getString("history", "[]")!!.let {
                val arr = JSONArray(it); (0 until arr.length()).map { i -> arr.getInt(i) }
            }
        )
        save()
    }

    // --- 滑动操作 ---
    fun swipe(direction: Direction) {
        val cur = _state.value
        if (cur.isGameOver) return

        val board = cur.board.map { it.toMutableList() }
        val merged = mutableSetOf<Pair<Int, Int>>()
        var moved = false
        var gained = 0

        val traversals = when (direction) {
            Direction.LEFT -> (0 until 4).map { r -> (0 until 4).map { c -> r to c } }
            Direction.RIGHT -> (0 until 4).map { r -> (3 downTo 0).map { c -> r to c } }
            Direction.UP -> (0 until 4).map { c -> (0 until 4).map { r -> r to c } }
            Direction.DOWN -> (0 until 4).map { c -> (3 downTo 0).map { r -> r to c } }
        }

        for (line in traversals) {
            val (m, s, mg) = mergeLine(line.map { (r, c) -> board[r][c] }, line)
            for (i in line.indices) {
                val (r, c) = line[i]
                if (board[r][c] != m[i]) moved = true
                board[r][c] = m[i]
            }
            gained += s
            merged.addAll(mg)
        }

        if (!moved) {
            _state.value = _state.value.copy(justMerged = emptySet())
            return
        }

        addRandom(board)

        val newScore = cur.score + gained
        val newBest = maxOf(newScore, cur.bestScore)
        if (newBest > cur.bestScore) prefs.edit().putInt("best", newBest).apply()

        val over = isGameOver(board)
        if (over) saveHistory(newScore)

        _state.value = GameState(
            board = board.map { it.toList() },
            score = newScore,
            bestScore = newBest,
            history = cur.history,
            isGameOver = over,
            justMerged = merged
        )
        save()
    }

    // --- 合并逻辑 ---
    private fun mergeLine(
        values: List<Int>,
        positions: List<Pair<Int, Int>>
    ): Triple<List<Int>, Int, Set<Pair<Int, Int>>> {
        val nonZero = values.filter { it != 0 }.toMutableList()
        val result = mutableListOf<Int>()
        val merged = mutableSetOf<Pair<Int, Int>>()
        var score = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                result.add(nonZero[i] * 2)
                score += nonZero[i] * 2
                // 找最后一个非零位置做合并标记
                var cnt = 0
                for (j in positions.indices) {
                    if (values[j] != 0) {
                        if (cnt == i) { merged.add(positions[j]); break }
                        cnt++
                    }
                }
                i += 2
            } else {
                result.add(nonZero[i]); i++
            }
        }
        while (result.size < 4) result.add(0)
        return Triple(result, score, merged)
    }

    private fun addRandom(board: List<MutableList<Int>>) {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 4) for (c in 0 until 4) if (board[r][c] == 0) empty.add(r to c)
        if (empty.isEmpty()) return
        val (r, c) = empty[Random.nextInt(empty.size)]
        board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
    }

    private fun isGameOver(board: List<List<Int>>): Boolean {
        for (r in 0 until 4) for (c in 0 until 4) {
            if (board[r][c] == 0) return false
            if (c < 3 && board[r][c] == board[r][c + 1]) return false
            if (r < 3 && board[r][c] == board[r + 1][c]) return false
        }
        return true
    }

    // --- 持久化 ---
    private fun save() {
        val s = _state.value
        val json = JSONObject().apply {
            put("score", s.score)
            put("board", JSONArray().apply {
                for (row in s.board) put(JSONArray(row))
            })
        }
        prefs.edit().putString("save", json.toString()).apply()
    }

    private fun saveHistory(score: Int) {
        val arr = JSONArray(prefs.getString("history", "[]"))
        arr.put(score)
        // 只保留最近 20 条
        while (arr.length() > 20) arr.remove(0)
        prefs.edit().putString("history", arr.toString()).apply()
        _state.value = _state.value.copy(history = (0 until arr.length()).map { arr.getInt(it) })
    }

    enum class Direction { LEFT, RIGHT, UP, DOWN }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            Game2048ViewModel(context.applicationContext) as T
    }
}
