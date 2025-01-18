package com.example.matrixcalculator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Selection
import android.text.Spannable
import android.widget.TextView
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.widget.SeekBar
import android.view.MotionEvent
import android.view.View
import android.app.AlertDialog
import androidx.core.text.getSpans
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

class Stack<T> {
    private val deque = ArrayDeque<T>()
    fun push(x: T) {
        deque.addLast(x)
    }
    fun pop(): T {
        return deque.removeLast()
    }
    fun top(): T {
        return deque.last()
    }
    fun isEmpty(): Boolean {
        return deque.isEmpty()
    }
    fun clear() {
        while(deque.isNotEmpty()) {
            deque.removeLast()
        }
    }
}

class MatrixSign(con: Context, ind: Int): ClickableSpan() {
    private val index = ind
    private val context = con
    private fun goToMatrixInput() {
        MainActivity().startMatrixActivity(context, index)
    }

    override fun onClick(widget: View) {
        //System.out.println("clicked${index}")
        goToMatrixInput()
    }
}

//Resolve the issue of triggering the onClick() function of ClickableSpan when scrolling TextView
class LongClickLinkMovementMethod(): LinkMovementMethod() {
    private var lastClickTime = 0L
    private val clickDelay = 250
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            val x = event.x - widget.totalPaddingLeft + widget.scrollX
            val y = event.y - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val off = layout.getOffsetForHorizontal(layout.getLineForVertical(y.toInt()), x)
            val link = buffer.getSpans<ClickableSpan>(off, off)
            if(link.isNotEmpty()) {
                if(action == MotionEvent.ACTION_UP) {
                    if(System.currentTimeMillis() - lastClickTime < clickDelay) {
                        link[0].onClick(widget)
                    }
                }
                else {
                    Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]))
                    lastClickTime = System.currentTimeMillis()
                }
                return true
            }
            else {
                Selection.removeSelection(buffer)
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    companion object {
        private val obj = LongClickLinkMovementMethod()
        fun getInstance(): LongClickLinkMovementMethod {
            return obj
        }
    }
}

open class Button(con: Context, view: View, id: Int, syb: String) {
    companion object {
        var expression = ""
        var bracket = 0
        var indices = 0
    }
    val context: Context
    val expressionText: TextView
    val answerText: TextView
    private val button: View
    val symbol: String

    private fun onClick() {
        update()
        showExpression()
    }
    private fun onLongClick() {
        longUpdate()
        showExpression()
    }

    open fun update() {
        expression += symbol
    }
    open fun longUpdate() {}//Do nothing by default

    open fun showExpression() {
        var temp = ""
        var matrices = IntArray(0)
        for(i in expression.indices) {
            when (expression[i]) {
                '*' -> temp += "×"
                'd' -> temp += "det"
                't' -> temp += "T"
                //The beginning of a matrix symbol
                '[' -> {
                    temp += "A"
                    matrices = matrices.plus(temp.length)
                }
                //The ending of a matrix symbol
                ']' -> matrices = matrices.plus(temp.length)
                else -> temp += expression[i]
            }
        }
        expressionText.movementMethod = LongClickLinkMovementMethod.getInstance()
        //Process matrix subscript
        val span = SpannableString(temp)
        for(i in matrices.indices step 2){
            span.setSpan(MatrixSign(context,i / 2), matrices[i] - 1, matrices[i + 1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            //size: px
            span.setSpan(AbsoluteSizeSpan(75), matrices[i], matrices[i + 1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        expressionText.text = span
    }

    fun clearEntry(str: String): String {
        return str.substring(0 until str.length - 1)
    }

    init {
        expressionText = view.findViewById(R.id.text_exp)
        answerText = view.findViewById(R.id.text_ans)
        button = view.findViewById(id)
        symbol = syb
        context = con
        button.setOnClickListener {
            onClick()
        }
        button.setOnLongClickListener {
            onLongClick()
            false
        }
        showExpression()
    }
}

//For numbers like 1, 2, ...
class NumberButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(expression.isEmpty() || (expression.last() != ')' && expression.last() != ']' && expression.last() != 't')) {
            //Normal input
            expression += symbol
        }
        else {
            //Complete multiplication sign at the end of a subexpression
            expression += "*${symbol}"
        }
    }
}

//For the decimal point
class PointButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(expression.isEmpty() || expression.last() == '+' || expression.last() == '-' || expression.last() == '*' || expression.last() == '(') {
            //The beginning of a subexpression. Complete a 0
            expression += "0${symbol}"
        }
        else {
            val numbers = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            var i = expression.length - 1
            var flag = false
            while(i >= 0 && expression[i] != symbol[0] && !(flag)) {
                if(!(numbers.contains(expression[i]))) {
                    flag = true
                }
                i--
            }
            if(flag || i == -1) {//The last number does not contain a decimal point
                if(numbers.contains(expression.last())) {
                    //Normal input
                    expression += symbol
                }
                else if(expression.last() == ')' || expression.last() == ']' || expression.last() == 't') {
                    //The end of a subexpression. Complete a multiplication sign and 0
                    expression += "*0${symbol}"
                }
                else {
                    //A binary scalar operator. Complete a 0
                    expression += "0${symbol}"
                }
            }
        }
    }
}

//For operators like +, -, ...
class OperatorButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(expression.isEmpty() || expression.last() == '(') {
            //if(symbol == "+" || symbol == "-") {
                //The beginning of a subexpression. Complete 0 before the binary operator
                expression += "0${symbol}"
            //}
        }
        else if(expression.last() != '+' && expression.last() != '-' && expression.last() != '*' && expression.last() != '^' && expression.last() != '.') {
            //Normal input
            expression += symbol
        }
        else {
            //A scalar operator, replace it
            expression = clearEntry(expression) + symbol
        }
    }
}

//For matrix symbol input
class MatrixButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        //Subscript == index + 1
        GlobalVariables.setMatrix(Matrix(indices++, 3, 3))
        if(expression.isEmpty() || expression.last() == '+' || expression.last() == '-' || expression.last() == '*' || expression.last() == '^' || expression.last() == '(') {
            //Normal input
            expression += "[${indices}]"
        }
        else if(expression.last() == '.') {
            //expression += "0*[${matrixIndex++}]"
            //Remove redundant decimal point.
            expression = clearEntry(expression) + "*[${indices}]"
        }
        else {
            //Complete multiplication sign
            expression += "*[${indices}]"
        }
    }
}

//For postfix matrix operators like transposition
//Transposition is now using class OperationButton because scalars are considered as first-order matrices
class MatrixOperatorButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(expression.isNotEmpty() && (expression.last() == ']' || expression.last()==')' || expression.last()=='t')) {
            expression += symbol
        }
    }
}

//For prefix matrix operators like determinant
class DeterminantButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        bracket++
        if(expression.isEmpty() || expression.last() == '+' || expression.last() == '-' || expression.last() == '*' || expression.last() == '^' || expression.last() == '(') {
            expression += "d("
        }
        else if(expression.last() == '.') {
            //expression += "0*d("
            expression = clearEntry(expression) + "*d("
        }
        else {
            expression += "*d("
        }
    }
}

//For left and right brackets like ( and )
class BracketButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(symbol == "(") {
            bracket++
            if(expression.isEmpty() || expression.last() == '+' || expression.last() == '-' || expression.last() == '*' || expression.last() == '^' || expression.last() == '('){
                expression += "("
            }
            else if(expression.last() == '.') {
                //expression += "0*("
                expression = clearEntry(expression) + "*("
            }
            else {
                expression += "*("
            }
        }
        else if(symbol == ")") {
            if(bracket > 0) {
                bracket--
                if(expression.last() == '(') {
                    expression += "0)"
                }
                else if(expression.last() == '.') {
                    //expression += "0)"
                    expression = clearEntry(expression) + ")"
                }
                else {
                    expression += ")"
                }
            }
        }
    }
}

//For the equal sign
class ComputeButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    private val operands = Stack<Pair<Char, Double>>()
    private val operators = Stack<Char>()

    override fun update() {
        if(expression.isNotEmpty()) {
            if(expression.last() == '.') {
                expression = clearEntry(expression)
            }
            val binaryOperator = arrayOf('+', '-', '*', '^')
            if(binaryOperator.contains(expression.last())) {
                expression += '0'
            }
            if(bracket > 0) {//Complete brackets
                for(i in 1 .. bracket) {
                    if(expression.last() == '(') {
                        expression += "0)"
                    }
                    else {
                        expression += ")"
                    }
                }
                bracket = 0
            }
            compute()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun compute() {
        GlobalVariables.indices = indices
        val numbers = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
        val opt = arrayOf('+', '-', '*', '^', 't', 'd')
        val priority = arrayOf(0, 0, 1, 2, 3, 3)
        var i = 0
        while(i < expression.length) {
            if(numbers.contains(expression[i])) {
                //Number
                var temp = ""
                while(i < expression.length && numbers.contains(expression[i])) {
                    temp += expression[i]
                    i++
                }
                operands.push(Pair('n',temp.toDouble()))
                i--
            }
            else if(expression[i] == '[') {
                //Matrix
                var temp = ""
                i++
                while(i < expression.length && numbers.contains(expression[i])) {
                    temp += expression[i]
                    i++
                }
                operands.push(Pair('m',temp.toDouble() - 1))
            }
            else if(expression[i] == '(') {
                operators.push('(')
            }
            else if(expression[i] == ')') {
                while(operators.top() != '(') {
                    calculate()
                }
            }
            else {
                //Operator
                while(!operators.isEmpty()) {
                    if(!opt.contains(operators.top())) {
                        //Bracket
                        operators.push(expression[i])
                        break
                    }
                    else {
                        val inComingPriority = priority[opt.indexOf(expression[i])]
                        val inStackPriority = priority[opt.indexOf(operators.top())]
                        if(inComingPriority > inStackPriority) {
                            operators.push(expression[i])
                            break
                        }
                        else {
                            calculate()
                        }
                    }
                }
                if(operators.isEmpty()) {
                    operators.push(expression[i])
                }
            }
            i++
        }
        while(!operators.isEmpty()) {
            calculate()
        }
        //System.out.println(operands.top())
        for(j in 0 until GlobalVariables.indices) {
            val mat = GlobalVariables.getMatrix(j)
            mat.setNewness(false)
            GlobalVariables.setMatrix(mat)
        }
        var ans = operands.top()
        while(!operands.isEmpty()) {
            ans = operands.pop()
        }
        if(ans.first == 'n') {
            if(ans.second == floor(ans.second) && ans.second < 1e7) {
                answerText.text = "=${ans.second.toInt()}"
            }
            else if(ans.second >= 1e7){
                answerText.text = "=${String.format("%.7f", ans.second / 10.0.pow(floor(log(ans.second, 10.0))))}E${log(ans.second, 10.0).toInt()}"
            }
            else {
                answerText.text = "=${String.format("%.7f", ans.second)}"
            }
        }
        else {
            answerText.movementMethod = LongClickLinkMovementMethod.getInstance()
            val span = SpannableString("=A${ans.second.toInt() + 1}")
            span.setSpan(MatrixSign(context, ans.second.toInt()), 1, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            //size: px
            span.setSpan(AbsoluteSizeSpan(75), 2, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            answerText.text = span
            System.out.println("ans:${ans.second}")
            GlobalVariables.getMatrix(1).showMatrix()
            //GlobalVariables.getMatrix(ans.second.toInt()).showMatrix()
        }
    }
    private fun calculate() {
        val opt = operators.pop()
        if(opt == '+') {
            val b = operands.pop()
            val a = operands.pop()
            if((a.first == 'm' && b.first == 'm') || (a.first == 'n' && b.first == 'n')) {
                plus(a, b)
            }
            else {
                val ind: Int
                if(a.first == 'm') {
                    ind = a.second.toInt() + 1
                }
                else {
                    ind = b.second.toInt() + 1
                }
                error("不支持矩阵A${ind}与标量做加法运算")
            }
        }
        else if(opt == '-') {
            val b = operands.pop()
            val a = operands.pop()
            if((a.first == 'm' && b.first == 'm') || (a.first == 'n' && b.first == 'n')) {
                minus(a, b)
            }
            else {
                val ind: Int
                if(a.first == 'm') {
                    ind = a.second.toInt() + 1
                }
                else {
                    ind = b.second.toInt() + 1
                }
                error("不支持矩阵A${ind}与标量做减法运算")
            }
        }
        else if(opt == '*') {
            val b = operands.pop()
            val a = operands.pop()
            if(b.first == 'm') {
                times(a, b)
            }
            else {
                times(b, a)
            }
        }
        else if(opt == '^') {
            val b = operands.pop()
            val a = operands.pop()
            if(b.first == 'm') {
                //Matrix exponents require the application of power series, which is beyond the scope of this App
                error("不支持将矩阵A${b.second.toInt() + 1}作为指数")
            }
            else if(a.first == 'm' && b.first == 'n' && floor(b.second) != b.second) {
                //The fractional power of a general matrix is not unique and is not well defined
                error("不支持矩阵A${a.second.toInt() + 1}的分数幂运算")
            }
            else {
                power(a, b)
            }
        }
        else if(opt == 't') {
            val a = operands.pop()
            if(a.first == 'm') {
                val matrix = GlobalVariables.getMatrix(a.second.toInt())
                val index = GlobalVariables.indices
                val tMatrix = transposition(matrix, index)
                GlobalVariables.setMatrix(tMatrix)
                operands.push(Pair('m', index.toDouble()))
            }
            else {
                //Consider scalars as one-dimensional square matrix
                operands.push(a)
            }
        }
        else if(opt == 'd') {
            val a = operands.pop()
            if(a.first == 'm') {
                val det = determinant(GlobalVariables.getMatrix(a.second.toInt()))
                if(det.isNaN()) {
                    error("矩阵A${a.second.toInt() + 1}不是方阵，无法计算行列式")
                }
                else {
                    operands.push(Pair('n', det))
                }
            }
            else {
                //Consider scalars as one-dimensional square matrix
                operands.push(a)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun error(log: String) {
        operators.clear()
        operands.clear()
        operands.push(Pair('n', Double.NaN))

        //Toast.makeText(context, log, Toast.LENGTH_SHORT).show()
        val builder = AlertDialog.Builder(context)
        builder.setMessage(log)
        builder.setPositiveButton("确认") {
             _, _ ->
        }
        val alert = builder.create()
        alert.show()
    }

    //For scalar addition and matrix addition
    private fun plus(a: Pair<Char, Double>, b: Pair<Char, Double>) {
        if(a.first == 'm') {
            val mat1 = GlobalVariables.getMatrix(a.second.toInt())
            val mat2 = GlobalVariables.getMatrix(b.second.toInt())
            val length = mat1.getLength()
            val width = mat1.getWidth()
            val index = GlobalVariables.indices
            if(length == mat2.getLength() && width == mat2.getWidth()) {
                val mat3 = Matrix(index, length, width)
                for(i in 0 until length) {
                    for(j in 0 until width) {
                        mat3.setElement(i, j, mat1.getElement(i, j) + mat2.getElement(i, j))
                    }
                }
                GlobalVariables.setMatrix(mat3)
                operands.push(Pair('m', index.toDouble()))
            }
            else {
                error("不支持长宽不等的矩阵A${a.second.toInt() + 1}和A${b.second.toInt() + 1}做加法运算")
            }
        }
        else {
            operands.push(Pair('n', a.second + b.second))
        }
    }

    //For scalar subtraction and matrix subtraction
    private fun minus(a: Pair<Char, Double>, b: Pair<Char, Double>) {
        if(a.first == 'm') {
            val mat1 = GlobalVariables.getMatrix(a.second.toInt())
            val mat2 = GlobalVariables.getMatrix(b.second.toInt())
            val length = mat1.getLength()
            val width = mat1.getWidth()
            val index = GlobalVariables.indices
            if(length == mat2.getLength() && width == mat2.getWidth()) {
                val mat3 = Matrix(index, length, width)
                for(i in 0 until length) {
                    for(j in 0 until width) {
                        mat3.setElement(i, j, mat1.getElement(i, j) - mat2.getElement(i, j))
                    }
                }
                GlobalVariables.setMatrix(mat3)
                operands.push(Pair('m', index.toDouble()))
            }
            else {
                error("不支持长宽不等的矩阵A${a.second.toInt() + 1}和A${b.second.toInt() + 1}做减法运算")
            }
        }
        else {
            operands.push(Pair('n', a.second - b.second))
        }
    }

    private  fun matrixMultiplication(a: Matrix, b: Matrix, ind: Int): Matrix {
        val length = a.getLength()
        val width = b.getWidth()
        val mid = a.getWidth()
        val c = Matrix(ind, length, width)
        for(i in 0 until length) {
            for(j in 0 until width) {
                var temp = 0.0
                for(k in 0 until mid) {
                    temp += (a.getElement(i, k) * b.getElement(k, j))
                }
                c.setElement(i, j, temp)
            }
        }
        return c
    }

    //For scalar multiplication, scalar multiplication of matrices and matrix multiplication
    private fun times(a: Pair<Char, Double>, b: Pair<Char, Double>) {
        if(a.first == 'm' && b.first == 'm') {
            val mat1 = GlobalVariables.getMatrix(a.second.toInt())
            val mat2 = GlobalVariables.getMatrix(b.second.toInt())
            val index = GlobalVariables.indices
            if(mat1.getWidth() == mat2.getLength()) {
                GlobalVariables.setMatrix(matrixMultiplication(mat1, mat2, index))
                operands.push(Pair('m', index.toDouble()))
            }
            else {
                error("矩阵乘法中左矩阵A${a.second.toInt() + 1}的列数必须等于右矩阵A${b.second.toInt() + 1}的行数")
            }
        }
        else if(b.first == 'm'){
            val mat = GlobalVariables.getMatrix(b.second.toInt())
            val index = GlobalVariables.indices
            mat.index = index
            for(i in 0 until mat.getLength()) {
                for(j in 0 until mat.getWidth()) {
                    mat.setElement(i, j, mat.getElement(i, j) * a.second)
                }
            }
            GlobalVariables.setMatrix(mat)
            operands.push(Pair('m', index.toDouble()))
        }
        else {
            operands.push(Pair('n', a.second * b.second))
        }
    }

    //Determine whether the matrix is invertible by LU factorization
    //Ensure the matrix is square before calling
    private fun invertible(mat: Matrix): Triple<Boolean, Matrix, Matrix> {
        val length = mat.getLength()
        val lMatrix = Matrix(-1, length, length)
        val uMatrix = Matrix(-1, length, length)
        for(i in 0 until length) {
            uMatrix.setElement(0, i, mat.getElement(0, i))
            //System.out.println("u[0][$i]:${uMatrix.getElement(0, i)}")
            val dn = uMatrix.getElement(0, 0)
            if(dn != 0.0) {
                lMatrix.setElement(i, 0, mat.getElement(i, 0) / dn)
                //System.out.println("l[$i][0]:${lMatrix.getElement(i, 0)}")
            }
            else {
                //The matrix cannot be LU factorized, so it is not invertible
                return Triple(false, Matrix(-1, 1, 1), Matrix(-1, 1, 1))
            }
        }
        for(i in 1 until length) {
            for(j in i until  length) {
                var elem = mat.getElement(i, j)
                for(k in 0 until i) {
                    elem -= (lMatrix.getElement(i, k) * uMatrix.getElement(k, j))
                }
                uMatrix.setElement(i, j, elem)
                //System.out.println("u[$i][$j]:${uMatrix.getElement(i, j)}")
            }
            val dn = uMatrix.getElement(i, i)
            if(dn != 0.0) {
                for(j in i until length) {
                    var elem = mat.getElement(j, i)
                    for(k in 0 until  i) {
                        elem -= (lMatrix.getElement(j, k) * uMatrix.getElement(k, i))
                    }
                    elem /= dn
                    lMatrix.setElement(j, i, elem)
                    //System.out.println("l[$j][$i]:${lMatrix.getElement(j ,i)}")
                }
            }
            else {
                //The matrix cannot be LU factorized, so it is not invertible
                return Triple(false, Matrix(-1, 1, 1), Matrix(-1, 1, 1))
            }
        }
        return Triple(true, lMatrix, uMatrix)
    }

    //Find the inverse matrix of a lower triangular matrix
    private fun inverseLowerTriangularMatrix(mat: Matrix): Matrix {
        val length = mat.getLength()
        val width = mat.getWidth()
        val inverseMatrix = Matrix(-1, length, width)
        for(i in 0 until length) {
            for(j in 0 until width) {
                if(i == j) {
                    inverseMatrix.setElement(i, j, 1 / mat.getElement(i, j))
                }
                else if(i > j) {
                    var elem = 0.0
                    for(k in j until i) {
                        elem -= (mat.getElement(i, k) * inverseMatrix.getElement(k, j))
                    }
                    inverseMatrix.setElement(i, j, elem / mat.getElement(i, i))
                }
            }
        }
        return inverseMatrix
    }

    //Find the inverse matrix of a general invertible matrix
    private fun inverse(mat: Matrix, ind: Int): Pair<Boolean, Matrix> {
        val length = mat.getLength()
        val width = mat.getWidth()
        if(length > 2) {
            val res = invertible(mat)
            if(res.first) {
                //LU factorization for determinants above 2nd order
                val lMatrix = res.second
                val uMatrix = res.third
                //lMatrix.showMatrix()
                //uMatrix.showMatrix()

                //The inverse of transposition is equal to the transposition of inverse
                val inverseUMatrix = transposition(inverseLowerTriangularMatrix(transposition(uMatrix, -1)), -1)
                val inverseLMatrix = inverseLowerTriangularMatrix(lMatrix)

                //The inverse of product is equal to the product of inverses
                return Pair(true, matrixMultiplication(inverseUMatrix, inverseLMatrix, ind))
            }
            else {
                //The matrix is not invertible
                return Pair(false, Matrix(-1, 1, 1))
            }
        }
        else {
            //Directly calculate the inverse matrix of 1st and 2nd order matrices
            val det = determinant(mat)
            if(det != 0.0) {
                val ans = Matrix(ind, length, width)
                if(length == 1) {
                    ans.setElement(0, 0, 1 / ans.getElement(0, 0))
                }
                else if(length == 2) {
                    ans.setElement(0, 0, ans.getElement(1, 1) / det)
                    ans.setElement(1, 1, ans.getElement(0, 0) / det)
                    ans.setElement(0, 1, -ans.getElement(0, 1) / det)
                    ans.setElement(1, 0, -ans.getElement(1, 0) / det)
                }
                return Pair(true, ans)
            }
            else {
                //The matrix is not invertible
                return Pair(false, Matrix(-1, 1, 1))
            }
        }
    }

    //Find the determinant by LU factorization
    private fun determinant(mat: Matrix): Double {
        val length = mat.getLength()
        val width = mat.getWidth()
        if(length == width) {
            if(length == 1) {
                return mat.getElement(0, 0)
            }
            else if(length == 2) {
                return mat.getElement(0, 0) * mat.getElement(1, 1) - mat.getElement(1, 0) * mat.getElement(0, 1)
            }
            else if(length == 3) {
                return mat.getElement(0, 0) * mat.getElement(1, 1) * mat.getElement(2, 2) + mat.getElement(0, 1) * mat.getElement(1, 2) * mat.getElement(2, 0) + mat.getElement(0, 2) * mat.getElement(1, 0) * mat.getElement(2, 1) - mat.getElement(0, 2) * mat.getElement(1, 1) * mat.getElement(2, 0) - mat.getElement(0, 0) * mat.getElement(1, 2) * mat.getElement(2, 1) - mat.getElement(0, 1) * mat.getElement(1, 0) * mat.getElement(2, 2)
            }
            else {
                //For determinants above third order
                val res = invertible(mat)
                if(res.first) {
                    //The determinant is equal to the product of the main diagonal elements of the U matrix
                    val uMatrix = res.third
                    var det = 1.0
                    for(i in 0 until length) {
                        det *= uMatrix.getElement(i, i)
                    }
                    return det
                }
                else {
                    //The matrix cannot be LU factorized, so its determinant is 0
                    return 0.0
                }
            }
        }
        else {
            return Double.NaN
        }
    }

    //Finding the power of a square matrix to the 2nd and higher powers
    private fun fastExponentiation(mat: Matrix, exp: Double, ind: Int): Matrix {
        var product = mat
        val fastExp = floor(log(exp, 2.0)).toInt()
        for(i in 1 until fastExp) {
            product = matrixMultiplication(product , product, -1)
        }
        for(i in fastExp.toDouble().pow(2.0).toInt() until exp.toInt()) {
            product = matrixMultiplication(product, mat, -1)
        }
        product.index = ind
        return product
    }

    //For scalar exponentiation and matrix exponentiation
    private fun power(a: Pair<Char, Double>, b: Pair<Char, Double>) {
        if(a.first == 'm') {
            val matrix = GlobalVariables.getMatrix(a.second.toInt())
            val index = GlobalVariables.indices
            val length = matrix.getLength()
            if(length == matrix.getWidth()) {
                if(b.second > 1.0) {
                    //Use fast exponentiation algorithm instead of simple accumulation
                    GlobalVariables.setMatrix(fastExponentiation(matrix, b.second, index))
                }
                else if(b.second == 1.0) {
                    val mat = Matrix(index, length, length)
                    for(i in 0 until length) {
                        for(j in 0 until length) {
                            mat.setElement(i, j, matrix.getElement(i, j))
                        }
                    }
                    GlobalVariables.setMatrix(mat)
                }
                else {
                    //Equivalent to finding the power of the inverse matrix
                    val inverse = inverse(matrix, index)
                    if(inverse.first) {
                        when (b.second) {
                            0.0 -> {
                                //Identity matrix
                                val mat = Matrix(index, length, length)
                                for(i in 0 until length) {
                                    for(j in 0 until length) {
                                        if(i == j) {
                                            mat.setElement(i, j, 1.0)
                                        } else {
                                            mat.setElement(i, j, 0.0)
                                        }
                                    }
                                }
                                GlobalVariables.setMatrix(mat)
                            }
                            -1.0 -> GlobalVariables.setMatrix(inverse.second)
                            else -> GlobalVariables.setMatrix(fastExponentiation(inverse.second, (0.0 - b.second), index))
                        }
                    }
                    else {
                        error("矩阵A${a.second.toInt() + 1}不可逆，无法计算其非正数幂")
                    }
                }
                System.out.println("power:")
                GlobalVariables.getMatrix(1).showMatrix()
                operands.push(Pair('m', index.toDouble()))
            }
            else {
                error("矩阵A${a.second.toInt() + 1}不是方阵，无法进行幂运算")
            }
        }
        else {
            val power = a.second.pow(b.second)
            if(power.isNaN()) {
                error("不支持复数运算:${a.second}^${b.second}")
            }
            else {
                operands.push(Pair('n', power))
            }
        }
    }

    private fun transposition(matrix: Matrix, index: Int): Matrix {
        val length = matrix.getLength()
        val width = matrix.getWidth()
        val mat = Matrix(index, width, length)
        for(i in 0 until length) {
            for(j in 0 until width) {
                mat.setElement(j, i, matrix.getElement(i, j))
            }
        }
        return mat
    }

    init {
        answerText.text = ""
    }
}

//For the clear button
//Short click is CE (Clear Entry), while long click is AC (All Clear)
class ClearButton(con: Context, view: View, id: Int, syb: String): Button(con, view, id, syb) {
    override fun update() {
        answerText.text = ""
        GlobalVariables.indices = indices
        if(expression.isNotEmpty()) {
            if(expression.last() == ')') {
                bracket++
                expression = clearEntry(expression)
            }
            else if(expression.last() == '(') {
                bracket--
                expression = clearEntry(expression)
                if(expression.isNotEmpty() && expression.last() == 'd') {
                    expression = clearEntry(expression)
                }
            }
            else if(expression.last() == ']') {
                var i = expression.length - 1
                while(expression[i--] != '[') {
                    expression = clearEntry(expression)
                }
                expression = clearEntry(expression)
                indices--
                GlobalVariables.indices--
            }
            else {
                expression = clearEntry(expression)
            }
        }
    }

    override fun longUpdate() {
        answerText.text = ""
        expression = ""
        indices = 0
        GlobalVariables.indices = 0
        bracket = 0
    }
}

//for matrix elements input
open class ElementButton(con: Context, view: View, id: Int, syb: String, mid: Int): Button(con, view, id, syb) {
    companion object {
        var expression = ""
        var elements = 0
    }
    //When the app is on MatrixActivity, the matrix can be dynamically modified, while the index of the matrix is fixed.
    //Therefore, it is necessary to pass in the index of the matrix being modified, not the matrix itself.
    val matrixIndex: Int

    override fun showExpression() {
        var temp = ""
        var elem = 0
        var count = 0
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        for(i in expression.indices) {
            if(expression[i] == ' ') {
                count++
                if(elem < width - 1) {
                    elem++
                    temp += " "
                }
                else {
                    elem = 0
                    temp += "\n"
                }
            }
            else {
                temp += expression[i]
            }
            if(count == length * width) {
                break
            }
        }
        //Cursor
        temp += '|'
        val span = SpannableString(temp)
        span.setSpan(ForegroundColorSpan(Color.parseColor("#E5AD00")), temp.length - 1, temp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        expressionText.movementMethod = ScrollingMovementMethod.getInstance()
        expressionText.text = span
    }

    init {
        matrixIndex = mid
        //May be a bug: defining horizontally scrolling in XML files is ineffective. It must be set in kotlin files.
        expressionText.setHorizontallyScrolling(true)
        expressionText.isVerticalScrollBarEnabled = false

        val matrix = GlobalVariables.matrixModifying
        if(matrix.getNewness()) {
            expression = ""
            elements = 0
        }
        else {
            val length = matrix.getLength()
            val width = matrix.getWidth()
            elements = length * width
            var temp = ""
            for(i in 0 until length) {
                for(j in 0 until width) {
                    val element = matrix.getElement(i, j)
                    if(element == floor(element) && element < 1e7) {
                        temp += "${element.toInt()} "
                    }
                    else if(element >= 1e7){
                        temp += "${String.format("%.7f", element / 10.0.pow(floor(log(element, 10.0))))}E${log(element, 10.0).toInt()} "
                    }
                    else {
                        temp += "${String.format("%.7f", element)} "
                    }
                }
            }
            expression = temp
            showExpression()
        }
    }
}

//For numbers in matrix elements
class NumberElementButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        if(expression.isEmpty() || !(expression.last() == ' ' && elements >= length * width)) {
            expression += symbol
        }
    }
}

//For the decimal point in matrix elements
class PointElementButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        if(expression.isEmpty() || expression.last() == ' ') {
            expression += "0${symbol}"
        }
        else {
            var i = expression.length - 1
            var flag = false
            while(i >= 0 && expression[i] != symbol[0] && !(flag)) {
                if(expression[i] == ' ') {
                    flag = true
                }
                i--
            }
            if(flag || i == -1) {
                if(expression.last() == ' ') {
                    expression += "0${symbol}"
                }
                else {
                    expression += symbol
                }
            }
        }
    }
}

//For minus sign button
class MinusSignButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        if(expression.isEmpty() || (expression.last() == ' ' && elements < length * width)) {
            expression += symbol
        }
        else if(expression.last() == '-') {
            expression = clearEntry(expression)
        }
    }
}

//For the finish element input button
open class FinishElementButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    fun newElement() {
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        if(elements < length * width) {
            val row: Int = elements / width
            val column = elements % width
            if(expression.isEmpty() || expression.last() == ' ' || expression.last() == '-') {
                if(expression.isNotEmpty() && expression.last() == '-') {
                    expression = clearEntry(expression)
                }
                //Complete a 0 element
                expression += "0"
                GlobalVariables.matrixModifying.setElement(row, column, 0.0)
            }
            else{
                if(expression.last() == '.') {
                    expression = clearEntry(expression)
                }
                var i = expression.length - 1
                var temp = ""
                while(i >= 0 && expression[i] != ' ') {
                    temp = expression[i] + temp
                    i--
                }
                //System.out.println(temp)
                GlobalVariables.matrixModifying.setElement(row, column, temp.toDouble())
            }
            expression += " "
            elements++
        }
        //System.out.println(expression)
        //GlobalVariables.matrixModifying.showMatrix()
    }
    override fun update() {
        newElement()
    }
}

//For the clear element button
class ClearElementButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        if(expression.isNotEmpty()) {
            if(expression.last() == ' ') {
                val width = GlobalVariables.matrixModifying.getWidth()
                GlobalVariables.matrixModifying.setElement(elements / width, elements % width, 0.0)
                elements--
            }
            expression = clearEntry(expression)
        }
    }

    override fun longUpdate() {
        expression = ""
        elements = 0
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        for(i in 0 until length) {
            for(j in 0 until width) {
                GlobalVariables.matrixModifying.setElement(i, j, 0.0)
            }
        }
    }
}

//For the identity matrix button
class IdentityMatrixButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        var temp = ""
        for(i in 0 until length) {
            for(j in 0 until width) {
                if(i == j) {
                    temp += "1 "
                    GlobalVariables.matrixModifying.setElement(i, j, 1.0)
                }
                else {
                    temp += "0 "
                    GlobalVariables.matrixModifying.setElement(i, j, 0.0)
                }
            }
        }
        elements = length * width
        expression = temp
    }
}

//For the zero matrix button
class ZeroMatrixButton(con: Context, view: View, id: Int, syb: String, mid: Int): ElementButton(con, view, id, syb, mid) {
    override fun update() {
        val length = GlobalVariables.matrixModifying.getLength()
        val width = GlobalVariables.matrixModifying.getWidth()
        var temp = ""
        for(i in 0 until length) {
            for(j in 0 until width) {
                temp += "0 "
                GlobalVariables.matrixModifying.setElement(i, j, 0.0)
            }
        }
        elements = length * width
        expression = temp
    }
}

class Seekbar (view: View, mId: Int, sId: Int, tId: Int, txt: String, con: Context): FinishElementButton(con, view, sId, txt, mId) {
    private val seekbar: SeekBar
    private val info: TextView
    private val text: String
    private val identity: View

    private fun setText(i: Int) {
        val concat = text + i
        info.text = concat
    }

    //Dynamically adjust text size
    private fun setTextSize(lines: Int) {
        if(lines < 5) {
            expressionText.textSize = 40F
        }
        else {
            expressionText.textSize = (200F / (lines + 1))
        }
    }

    init {
        seekbar = view.findViewById(sId)
        info = view.findViewById(tId)
        text = txt
        identity = view.findViewById(R.id.button_i)
        if(GlobalVariables.matrixModifying.getLength() != GlobalVariables.matrixModifying.getWidth()) {
            identity.visibility = View.INVISIBLE
        }
        else {
            identity.visibility = View.VISIBLE
        }

        if(text == "行：") {
            seekbar.setProgress(GlobalVariables.matrixModifying.getLength() - 1)
        }
        else {
            seekbar.setProgress(GlobalVariables.matrixModifying.getWidth() - 1)
        }
        setTextSize(GlobalVariables.matrixModifying.getLength())
        setText(seekbar.progress + 1)
        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if(expression.isNotEmpty() && expression.last() != ' ') {
                    //Complete the last element input
                    newElement()
                    showExpression()
                }
                setText(progress + 1)
                val oldLength = GlobalVariables.matrixModifying.getLength()
                val oldWidth = GlobalVariables.matrixModifying.getWidth()
                var i = 0
                var temp = ""
                if(text == "行：") {
                    GlobalVariables.matrixModifying.setLength(progress + 1)
                    setTextSize(progress + 1)
                    while(i < minOf((progress + 1) * oldWidth, elements)) {
                        val e = GlobalVariables.matrixModifying.getElement(i / oldWidth, i % oldWidth)
                        if(e == floor(e)) {
                            temp += "${e.toInt()}"
                        }
                        else {
                            temp += e
                        }
                        temp += ' '
                        i++
                    }
                }
                else {
                    GlobalVariables.matrixModifying.setWidth(progress + 1)
                    while(i < minOf(oldLength * (progress + 1), elements / oldWidth * (progress + 1) + elements % oldWidth)) {
                        val e = GlobalVariables.matrixModifying.getElement(i / (progress + 1), i % (progress + 1))
                        if(e == floor(e)) {
                            temp += "${e.toInt()}"
                        }
                        else {
                            temp += e
                        }
                        temp += ' '
                        i++
                    }

                }
                elements = i
                expression = temp
                if(GlobalVariables.matrixModifying.getLength() != GlobalVariables.matrixModifying.getWidth()) {
                    identity.visibility = View.INVISIBLE
                }
                else {
                    identity.visibility = View.VISIBLE
                }
                showExpression()
            }
            //Must override these functions below along with onProgressChanged
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}

//For page jump buttons
class JumpButton(con: Context, view: View, id: Int, syb: String, mid: Int): FinishElementButton(con, view, id, syb, mid) {
    override fun update() {
        if(symbol == "check") {
            while(elements < GlobalVariables.matrixModifying.getLength() * GlobalVariables.matrixModifying.getWidth()) {
                newElement()
            }
            GlobalVariables.matrixModifying.setNewness(false)
            GlobalVariables.setMatrix(GlobalVariables.matrixModifying)
        }
        MatrixActivity().startMainActivity(context)
    }
}