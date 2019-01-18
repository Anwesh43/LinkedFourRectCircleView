package com.anwesh.uiprojects.linkedfourrectcircleview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.fourrectcircleview.FourRectCircleView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FourRectCircleView.create(this)
    }
}
