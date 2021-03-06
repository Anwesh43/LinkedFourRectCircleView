package com.anwesh.uiprojects.fourrectcircleview

/**
 * Created by anweshmishra on 18/01/19.
 */

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

val nodes : Int = 5
val rects : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.7f
val rSizeFactor : Float = 2.2f
val rGapFactor : Float = 1.9f
val foreColor : Int = Color.parseColor("#4CAF50")
val backColor : Int = Color.parseColor("#BDBDBD")
val strokeFactor : Int = 90

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawFRCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val size : Float = gap / sizeFactor
    val rSize : Float = size / rSizeFactor
    val rGap : Float = size / rGapFactor
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.color = foreColor
    save()
    translate(w/2, gap * (i + 1))
    rotate(90f * sc2)
    drawCircle(0f, 0f, size, paint)
    paint.style = Paint.Style.FILL
    for (j in 0..(rects - 1)) {
        val sc : Float = sc1.divideScale(j, rects)
        save()
        rotate(90f * j)
        translate(rGap, 0f)
        drawRect(RectF(-rSize / 3, -2 * rSize / 3, rSize / 3, -2 * rSize / 3 + (4 * rSize * sc) / 3), paint)
        restore()
    }
    restore()
}

class FourRectCircleView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, rects, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    class FRCNode(var i : Int, val state : State = State()) {

        private var next : FRCNode? = null
        private var prev : FRCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = FRCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawFRCNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : FRCNode {
            var curr : FRCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class FourRectCircle(var i : Int) {

        private val root : FRCNode = FRCNode(0)
        private var curr : FRCNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : FourRectCircleView) {

        private val animator : Animator = Animator(view)
        private var frc : FourRectCircle = FourRectCircle(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            frc.draw(canvas, paint)
            animator.animate {
                frc.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            frc.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : FourRectCircleView {
            val view : FourRectCircleView = FourRectCircleView(activity)
            activity.setContentView(view)
            return view
        }
    }
}