package com.example.matrixcalculator

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView = window.decorView
        val context = this
        //Instantiate button class
        val button_0 = NumberButton(context, rootView, R.id.button_0,"0")
        val button_1 = NumberButton(context, rootView, R.id.button_1,"1")
        val button_2 = NumberButton(context, rootView, R.id.button_2,"2")
        val button_3 = NumberButton(context, rootView, R.id.button_3,"3")
        val button_4 = NumberButton(context, rootView, R.id.button_4,"4")
        val button_5 = NumberButton(context, rootView, R.id.button_5,"5")
        val button_6 = NumberButton(context, rootView, R.id.button_6,"6")
        val button_7 = NumberButton(context, rootView, R.id.button_7,"7")
        val button_8 = NumberButton(context, rootView, R.id.button_8,"8")
        val button_9 = NumberButton(context, rootView, R.id.button_9,"9")
        val button_pt = PointButton(context, rootView, R.id.button_pt,".")
        val button_plus = OperatorButton(context, rootView, R.id.button_plus,"+")
        val button_minus = OperatorButton(context, rootView, R.id.button_minus,"-")
        val button_times = OperatorButton(context, rootView, R.id.button_times,"*")
        val button_pow = OperatorButton(context, rootView, R.id.button_pow,"^")
        val button_tr = OperatorButton(context, rootView, R.id.button_tr,"t")
        val button_det = DeterminantButton(context, rootView, R.id.button_det,"d(")
        val button_mat = MatrixButton(context, rootView, R.id.button_mat,"[")
        val button_l_brac = BracketButton(context, rootView, R.id.button_l_brac,"(")
        val button_r_brac = BracketButton(context, rootView, R.id.button_r_brac,")")
        val button_ans = ComputeButton(context, rootView, R.id.button_ans, "=")
        val button_ac = ClearButton(context, rootView, R.id.button_ac, "")
    }

    //Forbid scrolling left or right to leave the current page which results in page confusion
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    fun startMatrixActivity(con: Context, req: Int) {
        val intent = Intent(con, MatrixActivity::class.java)
        intent.putExtra("index", req)
        con.startActivity(intent)
    }
}