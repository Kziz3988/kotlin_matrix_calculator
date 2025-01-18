package com.example.matrixcalculator

object GlobalVariables {
    private var matrices = mutableListOf<Matrix>()
    var indices = 0
    var matrixModifying = Matrix(0, 3, 3)

    fun setMatrix(mat: Matrix) {
        if(mat.index >= indices) {
            mat.index = indices
            indices++
        }
        val ind = mat.index
        if(ind < matrices.size) {
            matrices[ind] = mat
        }
        else {
            matrices.add(mat)
        }
    }

    fun getMatrix(ind: Int): Matrix {
        if(ind < indices) {
            return matrices[ind]
        }
        else {
            val mat = Matrix(ind, 3, 3)
            if(ind < matrices.size) {
                matrices[ind] = mat
            }
            else {
                matrices.add(mat)
            }
            return mat
        }
    }
    fun getMatrices(): MutableList<Matrix> {
        return matrices
    }
}