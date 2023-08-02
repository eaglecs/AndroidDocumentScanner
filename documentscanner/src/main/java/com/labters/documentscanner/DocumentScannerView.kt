package com.labters.documentscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.labters.documentscanner.libraries.NativeClass
import com.labters.documentscanner.libraries.PolygonView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.opencv.core.MatOfPoint2f

class DocumentScannerView : FrameLayout{

    constructor(context: Context) : super(context){
        inflate(context, R.layout.document_scanner, this).run {
            doOnAttach {
                holder = findViewById(R.id.holder)
                image = findViewById(R.id.image)
                polygonView = findViewById(R.id.polygon_view)
                polygonView.setImageValidListener(PolygonView.OnImageValidListener { isValid ->
                    imageValidListener?.invoke(isValid)
                })
                isInitialized = true
                // handle Styleable
            }
        }
    }
    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ){
        handleStyleable(context, attrs, 0)
        inflate(context, R.layout.document_scanner, this).run {
            doOnAttach {
                holder = findViewById(R.id.holder)
                image = findViewById(R.id.image)
                polygonView = findViewById(R.id.polygon_view)
                polygonView.setImageValidListener(PolygonView.OnImageValidListener { isValid ->
                    imageValidListener?.invoke(isValid)
                })
                isInitialized = true
                // handle Styleable
                polygonView.initStyleable(
                    frameColor, frameColorError, handleSolidColor,
                    handleStrokeColor, mHandleStrokeSize, mHandleSize, mFrameSize, isEnableHandleMiddle
                )
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ){
        handleStyleable(context, attrs, defStyleAttr)
        inflate(context, R.layout.document_scanner, this).run {
            doOnAttach {
                holder = findViewById(R.id.holder)
                image = findViewById(R.id.image)
                polygonView = findViewById(R.id.polygon_view)
                polygonView.setImageValidListener(PolygonView.OnImageValidListener { isValid ->
                    imageValidListener?.invoke(isValid)
                })
                isInitialized = true
                // handle Styleable
                polygonView.initStyleable(
                    frameColor, frameColorError, handleSolidColor,
                    handleStrokeColor, mHandleStrokeSize, mHandleSize, mFrameSize, isEnableHandleMiddle
                )
            }
        }
    }

    private lateinit var holder: FrameLayout
    private lateinit var image: ImageView
    private lateinit var polygonView: PolygonView

    private lateinit var selectedImage: Bitmap
    private var isInitialized = false

    private var onLoad: OnLoadListener? = {
        Log.i(javaClass.simpleName, "loading = $it")
    }

    private var imageValidListener: OnImageValidListener? = null

    private val lifecycle: LifecycleOwner
        get() = findViewTreeLifecycleOwner()!!


    private val scope: LifecycleCoroutineScope
        get() = lifecycle.lifecycleScope

    private val nativeClass = NativeClass()

    private var frameColor = ActivityCompat.getColor(context, R.color.blue)
    private var frameColorError = ActivityCompat.getColor(context, R.color.red)
    private var handleSolidColor = ActivityCompat.getColor(context, R.color.polygonViewCircleBackground)
    private var handleStrokeColor = ActivityCompat.getColor(context, R.color.polygonViewCircleStrokeColor)
    private var mHandleStrokeSize = 0
    private var mHandleSize = 0
    private var mFrameSize = 0
    private var isEnableHandleMiddle = false


    private fun handleStyleable(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.documentScanner, defStyleAttr, 0)
        frameColor = ta.getColor(
            R.styleable.documentScanner_cds_frame_color,
            ActivityCompat.getColor(context, R.color.blue)
        )
        frameColorError = ta.getColor(
            R.styleable.documentScanner_cds_frame_color_error,
            ActivityCompat.getColor(context, R.color.red)
        )
        handleSolidColor = ta.getColor(
            R.styleable.documentScanner_cds_handle_solid_color,
            ActivityCompat.getColor(context, R.color.polygonViewCircleBackground)
        )
        handleStrokeColor = ta.getColor(
            R.styleable.documentScanner_cds_handle_stroke_color,
            ActivityCompat.getColor(context, R.color.polygonViewCircleStrokeColor)
        )
        val mDensity = getDensity()

        mHandleStrokeSize = ta.getDimensionPixelSize(
            R.styleable.documentScanner_cds_handle_stroke_size,
            (2 * mDensity).toInt()
        )

        mHandleSize = ta.getDimensionPixelSize(
            R.styleable.documentScanner_cds_handle_size,
            (20 * mDensity).toInt()
        )

        mFrameSize = ta.getDimensionPixelSize(
            R.styleable.documentScanner_cds_frame_size,
            (2 * mDensity).toInt()
        )
        isEnableHandleMiddle =
            ta.getBoolean(R.styleable.documentScanner_cds_handle_middle_enabled, true)
    }

    // Utility /////////////////////////////////////////////////////////////////////////////////////
    private fun getDensity(): Float {
        val displayMetrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            .getMetrics(displayMetrics)
        return displayMetrics.density
    }

    private fun initView() {
        scope.launch {
            onLoad?.invoke(true)
            setImageRotation()
            initializeCropping()
            onLoad?.invoke(false)
        }
    }

    fun setImage(image: Bitmap) {
        selectedImage = image
        doWhenInitialised { initView() }
    }

    private suspend fun setImageRotation() {
        var tempBitmap = selectedImage.copy(selectedImage.config, true)
        for (i in 1..4) {
            val point2f = nativeClass.getPoint(tempBitmap)
            if (point2f == null) {
                tempBitmap = rotateBitmap(tempBitmap, (90 * i).toFloat()).first()
            } else {
                selectedImage = tempBitmap.copy(selectedImage.config, true)
                break
            }
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Flow<Bitmap> = flow<Bitmap> {
        val matrix = Matrix()
        matrix.postRotate(angle)
        emit(Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true))
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int) = flow<Bitmap> {
        val m = Matrix()
        m.setRectToRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), RectF(
                0f, 0f,
                width.toFloat(),
                height.toFloat()
            ), Matrix.ScaleToFit.CENTER
        )
        emit(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true))
    }

    private suspend fun initializeCropping() {
        val scaledBitmap: Bitmap = scaledBitmap(
            selectedImage,
            holder.width,
            holder.height
        ).first()
        image.setImageBitmap(scaledBitmap)
        val tempBitmap = (image.drawable as BitmapDrawable).bitmap
        val pointFs = getEdgePoints(tempBitmap)
        polygonView.points = pointFs
        polygonView.visibility = VISIBLE
//        val padding = resources.getDimension(R.dimen.scanPadding).toInt() * 2
        val padding = mHandleSize
        val layoutParams =
            LayoutParams(tempBitmap.width + padding, tempBitmap.height + padding)
        layoutParams.gravity = Gravity.CENTER
        polygonView.layoutParams = layoutParams
        polygonView.setPointColor(ContextCompat.getColor(context, R.color.blue))
    }

    private fun getEdgePoints(tempBitmap: Bitmap): Map<Int, PointF>? {
        val pointFs: List<PointF> = getContourEdgePoints(tempBitmap)
        return orderedValidEdgePoints(tempBitmap, pointFs)
    }

    private fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
        var point2f = nativeClass.getPoint(tempBitmap)
        if (point2f == null) point2f = MatOfPoint2f()
        val points = listOf(*point2f.toArray())
        val result: MutableList<PointF> = ArrayList()
        for (i in points.indices) {
            result.add(PointF(points[i].x.toFloat(), points[i].y.toFloat()))
        }
        return result
    }

    private fun getOutlinePoints(tempBitmap: Bitmap): Map<Int, PointF> {
        val outlinePoints: MutableMap<Int, PointF> = HashMap()
        outlinePoints[0] = PointF(0f, 0f)
        outlinePoints[1] = PointF(tempBitmap.width.toFloat(), 0f)
        outlinePoints[2] = PointF(0f, tempBitmap.height.toFloat())
        outlinePoints[3] = PointF(tempBitmap.width.toFloat(), tempBitmap.height.toFloat())
        return outlinePoints
    }

    private fun orderedValidEdgePoints(
        tempBitmap: Bitmap,
        pointFs: List<PointF>
    ): Map<Int, PointF>? {
        var orderedPoints: Map<Int, PointF>? = polygonView.getOrderedPoints(pointFs)
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap)
        }
        return orderedPoints
    }

    @Throws
    fun getCroppedImage(): Bitmap {
        val points: Map<Int, PointF> = polygonView.points
        val xRatio: Float = selectedImage.width.toFloat() / image.width
        val yRatio: Float = selectedImage.height.toFloat() / image.height
        val x1 = points[0]!!.x * xRatio
        val x2 = points[1]!!.x * xRatio
        val x3 = points[2]!!.x * xRatio
        val x4 = points[3]!!.x * xRatio
        val y1 = points[0]!!.y * yRatio
        val y2 = points[1]!!.y * yRatio
        val y3 = points[2]!!.y * yRatio
        val y4 = points[3]!!.y * yRatio
        val finalBitmap: Bitmap = selectedImage.copy(selectedImage.config, true)
        return nativeClass.getScannedBitmap(finalBitmap, x1, y1, x2, y2, x3, y3, x4, y4)
    }

    private fun doWhenInitialised(function: () -> Unit) {
        scope.launch {
            while (isInitialized.not()) {
                delay(500L)
            }
            function()
        }
    }

    fun setOnLoadListener(listener: OnLoadListener?) {
        onLoad = listener
    }

    fun setImageValidListener(listener: OnImageValidListener) {
        this.imageValidListener = listener
    }


}
