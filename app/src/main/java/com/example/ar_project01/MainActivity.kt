package com.example.ar_project01

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import com.example.ar_project01.driodAR.Configuration.Companion.COL_NUM
import com.example.ar_project01.driodAR.Configuration.Companion.MAX_MOVE_DELAY_MS
import com.example.ar_project01.driodAR.Configuration.Companion.MAX_PULL_DOWN_DELAY_MS
import com.example.ar_project01.driodAR.Configuration.Companion.MIN_MOVE_DELAY_MS
import com.example.ar_project01.driodAR.Configuration.Companion.MIN_PULL_DOWN_DELAY_MS
import com.example.ar_project01.driodAR.Configuration.Companion.MOVES_PER_TIME
import com.example.ar_project01.driodAR.Configuration.Companion.ROW_NUM
import com.example.ar_project01.driodAR.Configuration.Companion.START_LIVES
import com.example.ar_project01.databinding.ActivityMainBinding
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.example.ar_project01.*
import com.example.ar_project01.driodAR.*
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode

import android.view.Gravity

import android.widget.Toast
import com.google.ar.core.Anchor

import com.google.ar.sceneform.ux.BaseArFragment.OnTapArPlaneListener
import java.util.function.Consumer
import java.util.function.Function


class MainActivity : AppCompatActivity() {


    private val TAG = "AppDebug MainActivity"


    private val MIN_OPENGL_VERSION = 3.0
    private var andyRenderable: ModelRenderable? = null



    private lateinit var arFragment: ArFragment
    private var droidRenderable: ModelRenderable? = null
    private var scoreboardRenderable: ViewRenderable? = null
    private var failLight: Light? = null
    private lateinit var scoreboard: ScoreboardView

    private var grid = Array(ROW_NUM) { arrayOfNulls<TranslatableNode>(COL_NUM) }
    private var initialized = false
    private var gameHandler = Handler()

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        setContentView(binding.root)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment



        initSingleDroid()

        binding.btnRefresh.setOnClickListener {


            initSingleDroid()

        }


     //   initDriodGame()



    }

    private fun initSingleDroid() {
        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept(Consumer { renderable: ModelRenderable ->
                Log.i(TAG, "initSingleDroid renderable: "+renderable)
                andyRenderable = renderable
            })
            .exceptionally { throwable: Throwable? ->
                Log.i(TAG, "initSingleDroid throwable: " + throwable)
                val toast =
                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (andyRenderable == null) {
                return@setOnTapArPlaneListener
            }

            Log.i(TAG, "initSingleDroid hitResult distance: "+hitResult.distance)
            Log.i(TAG, "initSingleDroid hitResult hitPose: "+hitResult.hitPose)
            Log.i(TAG, "initSingleDroid hitResult trackable: "+hitResult.trackable)


            Log.i(TAG, "initSingleDroid plane : "+plane)
            Log.i(TAG, "initSingleDroid motionEvent : "+motionEvent)

            // Create the Anchor.
            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode =
                AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            Log.i(TAG, "initSingleDroid anchor pose: "+anchor.pose)
            Log.i(TAG, "initSingleDroid trackingState: "+anchor.trackingState)
            Log.i(TAG, "initSingleDroid cloudAnchorId: "+anchor.cloudAnchorId)
            Log.i(TAG, "initSingleDroid cloudAnchorState: "+anchor.cloudAnchorState)

            Log.i(TAG, "initSingleDroid anchorNode anchor: "+anchorNode.anchor)
            Log.i(TAG, "initSingleDroid anchorNode isSmoothed: "+anchorNode.isSmoothed)
            Log.i(TAG, "initSingleDroid anchorNode isTracking: "+anchorNode.isTracking)

            // Create the transformable andy and add it to the anchor.
            val andy = TransformableNode(arFragment.transformationSystem)
            andy.setParent(anchorNode)
            andy.renderable = andyRenderable
            andy.select()

            Log.i(TAG, "initSingleDroid andy rotationController rotationRateDegrees: "+andy.rotationController.rotationRateDegrees)


        }
    }

    private fun initDriodGame() {

        initResources()


        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _: MotionEvent ->
            if (initialized) {
                // Already inizialized!
                // When the game is initialized and user touches without
                // hitting a droid, remove 50 points
                failHit()
                return@setOnTapArPlaneListener
            }

            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                // Only HORIZONTAL_UPWARD_FACING planes are good to play the game
                // Notify the user and return
                "Find an HORIZONTAL and UPWARD FACING plane!".toast(this)
                return@setOnTapArPlaneListener
            }

            if(droidRenderable == null || scoreboardRenderable == null || failLight == null){
                // Every renderable object must be initialized
                // On a real world/complex application
                // it can be useful to add a visual loading
                return@setOnTapArPlaneListener
            }

            val spacing = 0.3F

            val anchorNode = AnchorNode(hitResult.createAnchor())

            anchorNode.setParent(arFragment.arSceneView.scene)

            // Add N droid to the plane (N = COL x ROW)
            grid.matrixIndices { col, row ->
                val renderableModel = droidRenderable?.makeCopy() ?: return@matrixIndices
                TranslatableNode().apply {
                    setParent(anchorNode)
                    renderable = renderableModel
                    addOffset(x = row * spacing, z = col * spacing)
                    grid[col][row] = this

                    this.setOnTapListener { _, _ ->
                        if (this.position != DroidPosition.DOWN) {
                            // Droid hitted! assign 100 points
                            scoreboard.score += 100
                            this.pullDown()
                        } else {
                            // When player hits a droid that is not up
                            // it's like a "miss", so remove 50 points
                            failHit()
                        }
                    }
                }
            }

            // Add the scoreboard view to the plane
            val renderableView = scoreboardRenderable ?: return@setOnTapArPlaneListener
            TranslatableNode()
                .also {
                    it.setParent(anchorNode)
                    it.renderable = renderableView
                    it.addOffset(x = spacing, y = .6F)
                }

            // Add a light
            Node().apply {
                setParent(anchorNode)
                light = failLight
                localPosition = Vector3(.3F, .3F, .3F)
            }

            initialized = true
        }

    }


    private val pullUpRunnable: Runnable by lazy {
        Runnable {
            if (scoreboard.life > 0) {
                grid.flatMap { it.toList() }
                    .filter { it?.position == DroidPosition.DOWN }
                    .run { takeIf { size > 0 }?.getOrNull((0..size).random()) }
                    ?.apply {
                        pullUp()
                        val pullDownDelay = (MIN_PULL_DOWN_DELAY_MS..MAX_PULL_DOWN_DELAY_MS).random()
                        gameHandler.postDelayed({ pullDown() }, pullDownDelay)
                    }

                // Delay between this move and the next one
                val nextMoveDelay = (MIN_MOVE_DELAY_MS..MAX_MOVE_DELAY_MS).random()
                gameHandler.postDelayed(pullUpRunnable, nextMoveDelay)
            }
        }
    }

    private fun failHit() {
        scoreboard.score -= 50
        scoreboard.life -= 1
        failLight?.blink()
        if (scoreboard.life <= 0) {
            // Game over
            gameHandler.removeCallbacksAndMessages(null)
            grid.flatMap { it.toList() }
                .filterNotNull()
                .filter { it.position != DroidPosition.DOWN && it.position != DroidPosition.MOVING_DOWN }
                .forEach { it.pullDown() }
        }
    }

    private fun initResources() {
        // Create a droid renderable (asynchronous operation,
        // result is delivered to `thenAccept` method)
        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept { droidRenderable = it }
            .exceptionally { it.toast(this) }

        scoreboard = ScoreboardView(this)

        scoreboard.onStartTapped = {
            // Reset counters
            scoreboard.life = START_LIVES
            scoreboard.score = 0
            // Start the game!
            gameHandler.post {
                repeat(MOVES_PER_TIME) {
                    gameHandler.post(pullUpRunnable)
                }
            }
        }

        // create a scoreboard renderable (asynchronous operation,
        // result is delivered to `thenAccept` method)
        ViewRenderable.builder()
            .setView(this, scoreboard)
            .build()
            .thenAccept {
                it.isShadowReceiver = true
                scoreboardRenderable = it
            }
            .exceptionally { it.toast(this) }

        // Creating a light is NOT asynchronous
        failLight = Light.builder(Light.Type.POINT)
            .setColor(Color(android.graphics.Color.RED))
            .setShadowCastingEnabled(true)
            .setIntensity(0F)
            .build()

    }



}
