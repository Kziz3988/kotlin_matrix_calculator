package com.example.matrixcalculator

import java.io.Serializable

class Matrix (ind: Int, len: Int, wid: Int):Serializable {
    var index: Int
    private var length: Int
    private var width: Int
    private var listLength: Int
    private var listWidth: Int
    private var elements = mutableListOf<MutableList<Double>>()
    private var newness: Boolean

    fun getLength(): Int {
        return length
    }
    fun getWidth(): Int {
        return width
    }
    fun getElement(row: Int, col: Int): Double {
        return elements[row][col]
    }

    fun setLength(len: Int) {
        if(listLength < len) {
            for(i in 0 until len - listLength) {
                val row = mutableListOf<Double>()
                for(j in 0 until listWidth) {
                    row += 0.0
                }
                elements += row
            }
            listLength = len
        }
        length = len
    }
    fun setWidth(wid: Int) {
        if(listWidth < wid) {
            for(i in 0 until listLength) {
                for (j in 0 until wid - listWidth) {
                    elements[i] += 0.0
                }
            }
            listWidth = wid
        }
        else {
            for(i in 0 until listLength) {
                for (j in wid until listWidth) {
                    elements[i][j] = 0.0
                }
            }
        }
        width = wid
    }
    fun setElement(row: Int, col: Int, value: Double) {
        if((row in 0 until length) && (col in 0 until width)) {
            elements[row][col] = value
        }
    }

    fun getNewness(): Boolean {
        return newness
    }
    fun setNewness(n: Boolean) {
        newness = n
    }

    fun showMatrix() {
        for(i in 0 until listLength) {
            for(j in 0 until listWidth) {
                System.out.print("${elements[i][j]} ")
            }
            System.out.print("\n")
        }
    }

    init {
        index = ind
        length = len
        width = wid
        listLength = len
        listWidth = wid
        for(i in 0 until len) {
            val row = mutableListOf<Double>()
            for(j in 0 until wid) {
                row += 0.0
            }
            elements += row
        }
        newness = true
    }
}