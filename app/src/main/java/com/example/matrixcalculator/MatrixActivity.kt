package com.example.matrixcalculator

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class MatrixActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matrix)

        val rootView = window.decorView
        val context = this
        val intent = intent
        val matrixIndex = intent.getIntExtra("index", 0)
        GlobalVariables.matrixModifying = GlobalVariables.getMatrix(matrixIndex)
        //System.out.println(matrixIndex)

        val button_0 = NumberElementButton(context, rootView, R.id.button_0,"0", matrixIndex)
        val button_1 = NumberElementButton(context, rootView, R.id.button_1,"1", matrixIndex)
        val button_2 = NumberElementButton(context, rootView, R.id.button_2,"2", matrixIndex)
        val button_3 = NumberElementButton(context, rootView, R.id.button_3,"3", matrixIndex)
        val button_4 = NumberElementButton(context, rootView, R.id.button_4,"4", matrixIndex)
        val button_5 = NumberElementButton(context, rootView, R.id.button_5,"5", matrixIndex)
        val button_6 = NumberElementButton(context, rootView, R.id.button_6,"6", matrixIndex)
        val button_7 = NumberElementButton(context, rootView, R.id.button_7,"7", matrixIndex)
        val button_8 = NumberElementButton(context, rootView, R.id.button_8,"8", matrixIndex)
        val button_9 = NumberElementButton(context, rootView, R.id.button_9,"9", matrixIndex)
        val button_pt = PointElementButton(context, rootView, R.id.button_pt,".", matrixIndex)
        val button_aij = FinishElementButton(context, rootView, R.id.button_aij,"", matrixIndex)
        val button_minus = MinusSignButton(context, rootView, R.id.button_minus,"-", matrixIndex)
        val button_i = IdentityMatrixButton(context, rootView, R.id.button_i,"i", matrixIndex)
        val button_o = ZeroMatrixButton(context, rootView, R.id.button_o,"o", matrixIndex)
        val button_ac = ClearElementButton(context, rootView, R.id.button_ac,"", matrixIndex)
        val seekbar_row = Seekbar(rootView, matrixIndex, R.id.seekbar_row, R.id.text_row, "行：", context)
        val seekbar_col = Seekbar(rootView, matrixIndex, R.id.seekbar_col, R.id.text_col, "列：", context)
        val button_back = JumpButton(context, rootView, R.id.button_back,"back", matrixIndex)
        val button_check = JumpButton(context, rootView, R.id.button_check,"check", matrixIndex)
    }

    //Forbid scrolling left or right to leave the current page which results in page confusion
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    fun startMainActivity(con: Context) {
        val intent = Intent(con, MainActivity::class.java)
        con.startActivity(intent)
    }
}