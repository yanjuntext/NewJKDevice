package com.tutk.IOTC.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.tutk.IOTC.AVIOCTRLDEFs
import com.tutk.IOTC.Camera
import com.tutk.IOTC.Liotc
import com.tutk.IOTC.camera.VideoQuality
import com.tutk.IOTC.listener.*
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.RecordStatus
import com.tutk.IOTC.status.VoiceType
import com.tutk.io.getAudioCodec
import com.tutk.io.getVideoQuality
import com.tutk.io.setVideoQuality
import com.tutk.utils.PermissionUtil
import kotlinx.coroutines.*
import java.io.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * @Author: wangyj
 * @CreateDate: 2021/10/9
 * @Description: 视频播放器
 */
class Monitor @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet?,
    defStyle: Int = 0
) : SurfaceView(context, attr, defStyle), LifecycleObserver, SurfaceHolder.Callback, OnIOCallback,
    OnSessionChannelCallback, OnFrameCallback, OnAudioListener {


    private var mPlayMode: PlayMode = PlayMode.PLAY_LIVE
    private var mVoiceType: VoiceType = VoiceType.ONE_WAY_VOICE

    private val DEFAULT_MAX_ZOOM_SCALE = 3.0F
    private val FLING_MIN_DISTANCE = 100
    private val FLING_MIN_VELOCITY = 0


    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2

    private val mPaint = Paint()
    private var nScreenWidth = 0
    private var nScreenHeight = 0

    private var mPinchedMode = NONE
    private val mStartPoint = PointF()
    private val mCurrentPoint = PointF()


    private var mOrigDist = 0f
    private var mLastZoomTime = 0L
    private var mCurrentScale = 1.0f
    private var mCurrentMaxScale = DEFAULT_MAX_ZOOM_SCALE

    private var mGestureDetector: GestureDetector? = null
    private var mSurHolder: SurfaceHolder? = null

    private var vLeft = 0
    private var vTop = 0
    private var vRight = 0
    private var vBottom = 0

    private val mRectCanvas = Rect()
    private val mRectMonitor = Rect()
    private val mMoveRect = Rect()

    private var mLastFrame: Bitmap? = null
    private var mCamera: Camera? = null
    private var mAvChannel = -1

    private var mCurVideoWidth = 0
    private var mCurVideoHeight = 0

    private var mRenderJob: Job? = null

    private val mStartClickPoint = PointF()

    private var mBitmapWidth = 0
    private var mBitmapHeight = 0


    private var isRunning = true

    //云台监听
    var onPTZListener: OnPtzListener? = null

    private var mOnClickListener: OnClickListener? = null
    private var mDownTime: Long = 0L

    private var isFullScreen = false

    private var mAVChannelRecordStatus: IAVChannelRecordStatus? = null

    private var mOnMonitorVideoQualityCallback: OnMonitorVideoQualityCallback? = null
    var mVideoQuality: VideoQuality? = null

    var isRecording = false

    /**宽高比例*/
    private var widthRation = 16
    private var heightRation = 9

    /**是否使用libyuv解析图片*/
    var withYuv = false

    private var canDraw = false

    private var mMonitorThread: MonitorThread? = null

    var onAudioListener: OnAudioListener? = null

    //surface 是否回调了destroy
    private var surfaceIsDestroy = true

    private val OPT_CHANGE_VIDEO_QUALITY = 10

    //是否切换了视频分辨率
    private var isChangeVideoQuality = false

    private var isRegisterEarphonesReceiver = false
    private var earPhoneReceiver: EarphonesReceiver? = null
    private lateinit var audioManager: AudioManager

    private val mHandler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == OPT_CHANGE_VIDEO_QUALITY) {
                changeQualityStopDecoderVideo(false)
            }
        }
    }

    init {
        mSurHolder = holder
        mSurHolder?.addCallback(this)
        isLongClickable = true
//        mGestureDetector = GestureDetector(this)
        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    Liotc.d("Monitor", "onFling 1")
                    if (mRectCanvas.left != vLeft || mRectCanvas.top != vTop || mRectCanvas.right != vRight || mRectCanvas.bottom != vBottom)
                        return false
                    Liotc.d("Monitor", "onFling 2")
                    when {
                        (e1?.x ?: 0f) - (e2?.x
                            ?: 0f) > FLING_MIN_DISTANCE && abs(velocityX) > FLING_MIN_VELOCITY -> {
                            //PTZ right
                            onPTZListener?.onPtz(PTZ.right)
                        }

                        (e2?.x ?: 0f) - (e1?.x
                            ?: 0f) > FLING_MIN_DISTANCE && abs(velocityX) > FLING_MIN_VELOCITY -> {
                            //PTZ left
                            onPTZListener?.onPtz(PTZ.left)
                        }

                        (e1?.y ?: 0f) - (e2?.y
                            ?: 0f) > FLING_MIN_DISTANCE && abs(velocityY) > FLING_MIN_VELOCITY -> {
                            //PTZ bottom
                            onPTZListener?.onPtz(PTZ.bottom)
                        }

                        (e2?.y ?: 0f) - (e1?.y
                            ?: 0f) > FLING_MIN_DISTANCE && abs(velocityY) > FLING_MIN_VELOCITY -> {
                            //PTZ top
                            onPTZListener?.onPtz(PTZ.top)
                        }
                    }
                    return false
                }

                //双击
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Liotc.d("Monitor", "onDoubleTap")
                    if (mRectCanvas.left > 0 || mRectCanvas.right < nScreenWidth || mRectCanvas.top > 0 || mRectCanvas.bottom < nScreenHeight) {
                        _setFullScreen()
                    } else {
                        scaleToOrigin()
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    Liotc.d("Monitor", "onDoubleTap--onSingleTapConfirmed")
                    mOnClickListener?.onClick(this@Monitor)
                    return true
                }
            })
    }

    /**设置充满全屏*/
    private fun _setFullScreen() {
        Liotc.d("Monitor", "_setFullScreen 1 wr=${widthRation} hr=${heightRation}")
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            Liotc.d("Monitor", "_setFullScreen 2")
            isFullScreen = false
            val cHeight = nScreenWidth * heightRation / widthRation
            if (cHeight >= nScreenHeight) {
                val ratio = cHeight * 1.0f / nScreenHeight
                val space = (cHeight - nScreenHeight) / 2
                mCurrentScale = ratio
                mRectCanvas.set(0, -space, nScreenWidth, cHeight - space)
            } else {
                val cWidth = nScreenHeight * widthRation / heightRation
                val ratio = cWidth * 1.0f / nScreenWidth
                val space = (cWidth - nScreenWidth) / 2
                mCurrentScale = ratio
                mRectCanvas.set(-space, 0, cWidth - space, nScreenHeight)
            }
        }
    }

    /**设置充满全屏*/
    fun setFullScreen() {
        isFullScreen = true
    }

    fun setNowFullScreen() {
        _setFullScreen()
    }

    /**缩放至原始大小*/
    private fun scaleToOrigin() {
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            val cHeight = nScreenWidth * heightRation / widthRation
            if (cHeight >= nScreenHeight) {
                val cWidth = nScreenHeight * widthRation / heightRation
                val space = (nScreenWidth - cWidth) / 2
                mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
            } else {
                val space = (nScreenHeight - cHeight) / 2
                mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
            }
            mCurrentScale = 1f
        }
    }

    /**设置最大缩放倍数*/
    fun setMaxZoom(max: Float) {
        mCurrentMaxScale = max
    }

    /**绑定Camera*/
    fun attachCamera(camera: Camera?, avChannel: Int) {
        mRenderJob?.cancel()
        mRenderJob = null
        mMonitorThread?.stopThread()
        mMonitorThread = null
        mCamera = camera
        mCamera?.registerSessionChannelCallback(this)
        mCamera?.registerIOCallback(this)
        mCamera?.registerFrameCallback(this)
        mCamera?.onAudioListener = this
        //先释放音频
//        mCamera?.releaseAudio(avChannel)

        mAvChannel = avChannel
        //释放音频
        releaseAudio()
        mCamera?.setPlayMode(mAvChannel, mPlayMode)
        mCamera?.setVoiceType(mAvChannel, mVoiceType)
        registerAVChannelRecordStatus(mAVChannelRecordStatus)
        Liotc.d("Monitor", "attachCamera")
        Liotc.d("Monitor", "renderJob--------->attachCamera")
        Liotc.d("Monitor", "renderJob  surfaceIsDestroy=$surfaceIsDestroy")
        if (!surfaceIsDestroy) {
            Liotc.d("Monitor", "renderJob  surfaceIsDestroy=$surfaceIsDestroy ---------")
            renderJob(holder)
        }
    }


    /**解绑Camera*/
    fun unAttachCamera() {
        mAvChannel = -1
        mCamera?.onAudioListener = null
        mCamera?.unregisterSessionChannelCallback(this)
        mCamera?.unregisterFrameCallback(this)
        mCamera?.unregisterIOCallback(this)
        mCamera = null

        destroyRendjob()
    }

    /**
     * 设置宽高比例
     */
    fun setWidthHeightRation(widthRation: Int, heightRation: Int) {
        this.widthRation = widthRation
        this.heightRation = heightRation
        Liotc.d("Monitor", "setWidthHeightRation wr=${this.widthRation} hr=${this.heightRation}")
    }

    /**
     * 设置播放类型
     * @param playMode 播放类型
     * */
    fun setPlayMode(playMode: PlayMode) {
        mPlayMode = playMode
        mCamera?.setPlayMode(mAvChannel, mPlayMode)
    }

    /**
     *音频类型
     * */
    fun setVoiceType(voiceType: VoiceType) {
        mVoiceType = voiceType
        mCamera?.setVoiceType(mAvChannel, mVoiceType)
        earPhoneReceiver?.voiceType = voiceType
    }

    /**播放视频*/
    fun startShow() {
        Liotc.d("Monitor", "send Audio Codec")
        mCamera?.registerSessionChannelCallback(this)
        mCamera?.registerIOCallback(this)
        mCamera?.registerFrameCallback(this)
        mCamera.getAudioCodec(mAvChannel)

        if (mPlayMode == PlayMode.PLAY_LIVE) {
            getMonitorVideoQuality()
        }
    }

    fun stopShow() {
        mCamera?.stopShow(mAvChannel)
    }

    //切换分辨率的时候，停止解码视频
    private fun changeQualityStopDecoderVideo(changeing: Boolean = false) {
//        mCamera?.changeQualityStopDecoderVideo(mAvChannel,changeing)
    }

    /**监听*/
    fun setAudioListener(audioListener: AudioListener) {
        if (mVoiceType == VoiceType.ONE_WAY_VOICE && audioListener == AudioListener.UNMUTE) {
            //如果是单向语音，开启了监听，必须要关闭通话
            setAudioTalker(AudioTalker.UNTALK)
        }
        mCamera?.setAudioTrackStatus(context, mAvChannel, audioListener == AudioListener.UNMUTE)
    }

    /**通话*/
    fun setAudioTalker(audioTalker: AudioTalker) {
        if (mVoiceType == VoiceType.ONE_WAY_VOICE && audioTalker == AudioTalker.TALK) {
            //如果是单向语音，开启了通话，必须要关闭监听
            setAudioListener(AudioListener.MUTE)
        }
        mCamera?.setAudioRecordStatus(context, mAvChannel, audioTalker == AudioTalker.TALK)
    }

    /**释放音频*/
    fun releaseAudio() {
        mCamera?.releaseAudio(mAvChannel)
    }

    fun registerAVChannelRecordStatus(listener: IAVChannelRecordStatus?) {
        mAVChannelRecordStatus = listener
        mCamera?.registerAVChannelRecordStatus(listener)
    }

    fun unRegisterAVChannelRecordStatus() {
        mCamera?.unRegisterAVChannelRecordStatus(mAVChannelRecordStatus)
        mAVChannelRecordStatus = null
    }

    fun startRecord(file: String?, callback: OnResultCallback<RecordStatus>?) {
        mCamera?.startRecord(
            context,
            mAvChannel,
            file,
            mBitmapWidth,
            mBitmapHeight
//            if (mBitmapWidth <= 1280) 1280 else mBitmapWidth,
//            if (mBitmapHeight <= 720) 720 else mBitmapHeight
        ) { result ->
            isRecording = result == RecordStatus.RECORDING
            callback?.onResult(result)
        }
    }


    fun stopRecord() {
        Liotc.d("stopRecord", "stopRecord")
        mCamera?.stopRecord()
        isRecording = false
    }

    fun registerOnMonitorVideoQualityCallback(callback: OnMonitorVideoQualityCallback?) {
        mOnMonitorVideoQualityCallback = callback
    }

    fun unRegisterOnMonitorVideoQualityCallback() {
        mOnMonitorVideoQualityCallback = null
    }

    fun getMonitorVideoQuality() {
        mCamera?.getVideoQuality(mAvChannel)
    }

    fun setMonitorVideoQuality(quality: VideoQuality, stopShow: Boolean = false) {
        if (mCamera?.isSessionConnected() == true) {
//            if(stopShow){
//                if (isRecording) {
//                    stopRecord()
//                }
////                stopShow()
//                changeQualityStopDecoderVideo(true)
//            }
//
            if (mCamera?.setVideoQuality(mAvChannel, quality) == true) {
                changeQualityStopDecoderVideo(true)
            }

        }
    }

    private fun renderJob(holder: SurfaceHolder) {
        runCatching {
            if (isRunning && mMonitorThread?.isThreadRunning() == true) {
                Liotc.d(
                    "Monitor",
                    "renderJob is Running return [$isRunning],[${mRenderJob?.isActive}]"
                )
                return
            }
            Liotc.d("Monitor", "surfaceDestroyed 1 renderJob=$canDraw")
            if (!canDraw) return
            Liotc.d("Monitor", "renderJob running")
            isRunning = true

            mMonitorThread = object : MonitorThread(surfaceHolder = holder) {
                override fun run() {

                    var videoCanvas: Canvas? = null

                    mPaint.isDither = true

                    while (isThreadRunning()) {
                        if (mCamera == null) break
                        Liotc.d(
                            "Monitor",
                            "renderJob -----[${mLastZoomTime != null}],[${mLastFrame?.isRecycled == false}],canDraw=$canDraw"
                        )
                        if (!isThreadRunning()) {
                            break
                        }
                        if (mLastFrame != null && mLastFrame?.isRecycled == false && !isChangeVideoQuality) {
                            this@Monitor.mSurHolder?.let { surfaceHolder ->
                                synchronized(surfaceHolder) {
                                    try {
                                        videoCanvas = mSurHolder?.lockCanvas()
                                        videoCanvas?.let { canvas ->

                                            mLastFrame?.let { bitmap ->
                                                Liotc.d("Monitor", "drawBitmap width=${bitmap.width} height=${bitmap.height}")
                                                if (isThreadRunning() && !bitmap.isRecycled) {
                                                    canvas.drawColor(Color.BLACK)
                                                    canvas.drawBitmap(
                                                        bitmap,
                                                        null,
                                                        mRectCanvas,
                                                        mPaint
                                                    )
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Liotc.d(
                                            "Monitor",
                                            "surfaceDestroyed renderJob error=${e.message}"
                                        )
                                    } finally {
                                        videoCanvas?.let {
                                            if (this@Monitor.canDraw) {
                                                mSurHolder?.unlockCanvasAndPost(it)
                                            }
                                        }

                                    }
                                }
                            }
                        }
                        Thread.sleep(33L)
                    }
                    Liotc.d("Monitor", "renderJob end")
                }
            }
            mMonitorThread?.start()


//            mRenderJob = GlobalScope.launch(Dispatchers.IO) {
//                var videoCanvas: Canvas? = null
//                mPaint.isDither = true
//                delay(200)
//                kotlin.runCatching {
//                    while (isRunning && isActive) {
//                        ensureActive()
//                        if (mCamera == null) break
//                        Liotc.d(
//                            "Monitor",
//                            "renderJob -----[${mLastZoomTime != null}],[${mLastFrame?.isRecycled == false}],canDraw=$canDraw"
//                        )
//                        if (!canDraw) {
//                            break
//                        }
//                        if (mLastFrame != null && mLastFrame?.isRecycled == false) {
//                            try {
//                                if (!canDraw) {
//                                    break
//                                }
//                                videoCanvas = mSurHolder?.lockCanvas()
//                                videoCanvas?.let { canvas ->
//                                    canvas.drawColor(Color.BLACK)
//                                    mLastFrame?.let { bitmap ->
//                                        Liotc.d("Monitor", "drawBitmap")
//                                        if(canDraw){
//                                            canvas.drawBitmap(bitmap, null, mRectCanvas, mPaint)
//                                        }
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                                Liotc.d("Monitor","surfaceDestroyed renderJob error=${e.message}")
//                            } finally {
//                                videoCanvas?.let {
//                                    if (canDraw) {
//                                        mSurHolder?.unlockCanvasAndPost(it)
//                                    }
//                                }
//                                videoCanvas = null
//                            }
//                        }
//                        delay(33L)
//                    }
//                }
//
//                Liotc.d("Monitor", "renderJob end")
//
//            }
        }.onFailure {
            Liotc.d("Monitor", "renderJob total error = ${it.message}")
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
    }


    fun onStart() {
        Liotc.d("Monitor", "onStart")
        startShow()
    }

    fun onStop() {
//        mCamera?.unregisterSessionChannelCallback(this)
//        mCamera?.unregisterFrameCallback(this)
//        mCamera?.unregisterIOCallback(this)


//        isRunning = false
//        mRenderJob?.cancel()
//        mRenderJob = null
    }

    fun onDestroy() {
        mHandler.removeMessages(OPT_CHANGE_VIDEO_QUALITY)
        Liotc.d("Monitor", "onDestroy")
        Liotc.d("Monitor", "onStop-------")
        stopShow()
        Liotc.d("Monitor", "onStop------- 111")
        //释放音频资源
        releaseAudio()
        Liotc.d("Monitor", "onStop------- 2222")
        if (isRecording) {
            stopRecord()
        }
//        mCamera = null
        Liotc.d("Monitor", "onStop------- 333")
        Liotc.d("Monitor", "surfaceDestroyed 3 onStop=$canDraw")
        unAttachCamera()
        unRegisterOnMonitorVideoQualityCallback()
        unRegisterAVChannelRecordStatus()
        unregisterEarPhoneChangeListener()
        Liotc.d("Monitor", "surfaceDestroyed 4 onDestroy")
    }

    private fun destroyRendjob() {
        isRunning = false
        mMonitorThread?.stopThread()
        kotlin.runCatching {
            mMonitorThread?.join()
        }
        mMonitorThread = null
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Liotc.d("Monitor", "motion event action [${event?.action}]")
        event?.let { _event ->
            when ((_event.action) and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mDownTime = System.currentTimeMillis()
                    if (mRectCanvas.left != vLeft || mRectCanvas.top != vTop
                        || mRectCanvas.right != vRight || mRectCanvas.bottom != vBottom
                    ) {
                        mPinchedMode = DRAG
                    }

                    mStartPoint.set(_event.x, _event.y)
                    mStartClickPoint.set(_event.x, _event.y)

                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    val dist = spacing(_event)
                    if (dist > 10f) {
                        mPinchedMode = ZOOM
                        mOrigDist = dist
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mPinchedMode == ZOOM) {
                        val result = scaleVideo(_event)
                        if (result) {
                            return true
                        }
                    } else if (mPinchedMode == DRAG) {
                        val result = moveVideo(_event)
                        if (result) {
                            return true
                        }
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (mCurrentScale == 1f) {
                        mPinchedMode = NONE
                    }
                    //点击事件
//                    if (_event.action == MotionEvent.ACTION_UP && System.currentTimeMillis() - mDownTime < 100) {
//                        mOnClickListener?.onClick(this)
//                    }

                    if (nScreenWidth != 0 && nScreenHeight != 0) {
                        val cHeight = nScreenWidth * heightRation / widthRation
                        var left = mRectCanvas.left
                        var top = mRectCanvas.top
                        var right = mRectCanvas.right
                        var bottom = mRectCanvas.bottom

                        val sWidth = mRectCanvas.width()
                        val sHeight = mRectCanvas.height()
                        if (cHeight >= nScreenHeight) {
                            //以高为基准
                            if (top > 0) {
                                top = 0
                                bottom = sHeight
                            }

                            if (bottom < nScreenHeight) {
                                top = nScreenHeight - sHeight
                                bottom = nScreenHeight

                            }

                            if (left > vLeft) {
                                left = vLeft
                                right = vLeft + sWidth
                            }

                            if (right < vRight) {
                                right = vRight
                                left = vRight - sWidth
                            }
                            mRectCanvas.set(left, top, right, bottom)

                        } else {
                            //以宽为基准
                            if (left > 0) {
                                left = 0
                                right = left + sWidth
                            }
                            if (right < nScreenWidth) {
                                right = nScreenWidth
                                left = nScreenWidth - left
                            }

                            if (top > vTop) {
                                top = vTop
                                bottom = vTop + sHeight
                            }
                            if (bottom < vBottom) {
                                bottom = vBottom
                                top = vBottom - sHeight
                            }
                            mRectCanvas.set(left, top, right, bottom)
                        }
                    }

                }
            }
        }
        event?.let { evt ->
            mGestureDetector?.onTouchEvent(evt)
        }
        return true
    }

    /**计算双指巨鹿*/
    private fun spacing(event: MotionEvent): Float {
        Liotc.d("Monitor", "spacing [${event.pointerCount}]")
        if (event.pointerCount > 1) {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return sqrt(x * x + y * y)
        }
        return 0f
    }


    /**滑动视频*/
    private fun moveVideo(event: MotionEvent): Boolean {
        if (System.currentTimeMillis() - mLastZoomTime < 33) {
            return true
        }
        mCurrentPoint.set(event.x, event.y)

        val offsetX = mCurrentPoint.x - mStartPoint.x
        val offsetY = mCurrentPoint.y - mStartPoint.y

        mStartPoint.set(mCurrentPoint.x, mCurrentPoint.y)

        mMoveRect.set(mRectCanvas)
        mMoveRect.offset(offsetX.toInt(), offsetY.toInt())

        val width = mMoveRect.width()
        val height = mMoveRect.height()

        var left = mMoveRect.left
        var top = mMoveRect.top
        var right = mMoveRect.right
        var bottom = mMoveRect.bottom
        if (left > vLeft) {
            left = vLeft
            right = vLeft + width
        }

        if (top > vTop) {
            top = vTop
            bottom = vTop + height
        }

        if (right < vRight) {
            right = vRight
            left = right - width
        }

        if (bottom < vBottom) {
            bottom = vBottom
            top = bottom - height
        }
        mRectCanvas.set(left, top, right, bottom)

        return false
    }

    /**缩放视频*/
    private fun scaleVideo(event: MotionEvent): Boolean {
        Liotc.d("Monitor", "scaleVideo 1 [${event.pointerCount}]")
        if (System.currentTimeMillis() - mLastZoomTime < 33 || event.pointerCount == 1) {
            return true
        }
        val newDist = spacing(event)
        Liotc.d("Monitor", "scaleVideo 2 [$newDist]")
        if (mOrigDist == 0f) {
            return true
        }
        val scale = newDist / mOrigDist
        mCurrentScale *= scale
        mOrigDist = newDist
        Liotc.d("Monitor", "scaleVideo 3 [$mCurrentScale],[$mCurrentMaxScale],[$scale]")
        if (mCurrentScale > mCurrentMaxScale) {
            mCurrentScale = mCurrentMaxScale
            return true
        }

        if (mCurrentScale < 1f) {
            mCurrentScale = 1f
        }

        val origWidth = vRight - vLeft
        val origHeight = vBottom - vTop

        val maxWidth = origWidth * mCurrentMaxScale.toInt()
        val maxHeight = origHeight * mCurrentMaxScale.toInt()

        val scaleWidth = (origWidth * mCurrentScale).toInt()
        val scaleHeight = (origHeight * mCurrentScale).toInt()

        var l =
            ((mRectMonitor.width() / 2) - ((mRectMonitor.width() / 2 - mRectCanvas.left) * scale)).toInt()
        var t =
            ((mRectMonitor.height() / 2) - ((mRectMonitor.height() / 2 - mRectCanvas.top) * scale)).toInt()

        var r = l + scaleWidth
        var b = t + scaleHeight
        Liotc.d("Monitor", "scaleVideo 4 ")
        if (scaleWidth <= origWidth || scaleHeight <= origHeight) {
            l = vLeft
            t = vTop
            r = vRight
            b = vBottom
        } else if (scaleWidth >= maxWidth || scaleHeight >= maxHeight) {
            l = mRectCanvas.left
            t = mRectCanvas.top
            r = l + maxWidth
            b = t + maxHeight
        }
        mRectCanvas.set(l, t, r, b)
        Liotc.d("Monitor", "scaleVideo 5 ")
        mLastZoomTime = System.currentTimeMillis()
        return false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceIsDestroy = false
        canDraw = true


        Liotc.d("Monitor", "surfaceDestroyed surfaceCreated")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Liotc.d(
            "Monitor",
            "surfaceChanged [screen[$nScreenWidth,$nScreenHeight]],[surface[$width,$height]],measured[$measuredWidth,$measuredHeight],wr=${widthRation} hr=${heightRation}"
        )
        synchronized(this) {
            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            mRectMonitor.set(0, 0, width, height)

            val cHeight = nScreenWidth * heightRation / widthRation
            if (cHeight > nScreenHeight) {
                //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                val cWidth = nScreenHeight * widthRation / heightRation
                val space = (nScreenWidth - cWidth) / 2
                mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
            } else {
                val space = (nScreenHeight - cHeight) / 2
                mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
            }

            vLeft = mRectCanvas.left
            vTop = mRectCanvas.top
            vRight = mRectCanvas.right
            vBottom = mRectCanvas.bottom

            mCurrentScale = 1f

            Liotc.d("Monitor", "_setFullScreen surfaceChanged[$isFullScreen]")

        }
        surfaceIsDestroy = false
        canDraw = true
        Liotc.d("Monitor", "surfaceDestroyed surfaceChanged")
        renderJob(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Liotc.d("Monitor", "surfaceDestroyed 1")
        this.mSurHolder?.let { surfaceHolder ->
            synchronized(surfaceHolder) {

            }
        }
        surfaceIsDestroy = true
        canDraw = false
        isRunning = false
        mMonitorThread?.stopThread()

        mRenderJob?.cancel()
    }

    override fun receiveFrameData(camera: Camera?, avChannel: Int, bmp: Bitmap?) {


    }

    override fun receiveFrameData(camera: Camera?, avChannel: Int, bmp: Bitmap?, time: Long) {
        if (avChannel != mAvChannel) {
            Liotc.d("Monitor", "receiveFrameData error [$avChannel],[$mAvChannel],[${bmp == null}]")
        }
        if (avChannel == mAvChannel) {
            Liotc.d(
                "Monitor",
                "receiveFrameData success [$avChannel],[$mAvChannel],[${bmp == null}]"
            )


            if (mRenderJob == null || mRenderJob?.isActive != true || !isRunning) {
                Liotc.d("Monitor", "restart render job")
                Liotc.d("Monitor", "renderJob--------->receiveFrameData")
//                renderJob()
            }

            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            if (mBitmapWidth != 0 && mBitmapHeight != 0) {
                isChangeVideoQuality = mBitmapWidth != bmp?.width || mBitmapHeight != bmp?.height
                if (isChangeVideoQuality) {
                    //视频大小不同，说明切换了分辨率，这时候请求当前分辨率
                    getMonitorVideoQuality()
                }
            }
            mBitmapWidth = bmp?.width ?: 0
            mBitmapHeight = bmp?.height ?: 0

            mLastFrame = bmp


            if ((bmp?.width ?: 0) > 0 && (bmp?.height
                    ?: 0) > 0 && (nScreenHeight != mCurVideoHeight || nScreenWidth != mCurVideoWidth)
            ) {
                Liotc.d(
                    "Monitor",
                    "screen[${nScreenWidth},${nScreenHeight}],video[${mCurVideoWidth},${mCurVideoHeight}]"
                )


                mCurVideoWidth = nScreenWidth
                mCurVideoHeight = nScreenHeight

                val cHeight = nScreenWidth * heightRation / widthRation
                if (cHeight > nScreenHeight) {
                    //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                    val cWidth = nScreenHeight * widthRation / heightRation
                    val space = (nScreenWidth - cWidth) / 2
                    mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
                } else {
                    val space = (nScreenHeight - cHeight) / 2
                    mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
                }

                vLeft = mRectCanvas.left
                vTop = mRectCanvas.top
                vRight = mRectCanvas.right
                vBottom = mRectCanvas.bottom

                mCurrentScale = 1f
                Liotc.d("Monitor", "_setFullScreen receiveFrameData[$isFullScreen]")
            }

            if (isFullScreen) {
                _setFullScreen()
            }
        }
    }

    override fun receiveFrameDataTime(time: Long) {

    }

    override fun receiveFrameInfo(
        camera: Camera?,
        avChannel: Int,
        bitRate: Long,
        frameRate: Int,
        onlineNm: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    ) {
    }

    override fun receiveSessionInfo(camera: Camera?, resultCode: Int) {

    }

    override fun receiveChannelInfo(camera: Camera?, avChannel: Int, resultCode: Int) {

    }


    override fun receiveIOCtrlData(
        camera: Camera?,
        avChannel: Int,
        avIOCtrlMsgType: Int,
        data: ByteArray?
    ) {
        when (avIOCtrlMsgType) {
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_RESP -> {
                Liotc.d("Monitor", " Audio Codec resp ")
                mCamera?.startShow(context, mAvChannel, withYuv = withYuv)
//                renderJob()
            }

            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_RESP -> {
                //获取清晰度
                data?.let {
                    if (it.size >= 5) {
                        when (it[4].toInt()) {
                            VideoQuality.FHD.value -> {
                                mVideoQuality = VideoQuality.FHD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.FHD)
                            }

                            VideoQuality.HD.value -> {
                                mVideoQuality = VideoQuality.HD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.HD)
                            }

                            VideoQuality.SMOOTH.value -> {
                                mVideoQuality = VideoQuality.SMOOTH
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SMOOTH)
                            }

                            VideoQuality.SSD.value -> {
                                mVideoQuality = VideoQuality.SSD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SSD)
                            }

                            else -> {
                                mVideoQuality = VideoQuality.SD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SD)
                            }
                        }
                    }
                }
            }

            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETSTREAMCTRL_RESP -> {
                //设置清晰度
                data?.let {
                    if (it.size >= 5) {
                        when (it[4].toInt()) {
                            VideoQuality.FHD.value -> {
                                mVideoQuality = VideoQuality.FHD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.FHD)
                            }

                            VideoQuality.HD.value -> {
                                mVideoQuality = VideoQuality.HD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.HD)
                            }

                            VideoQuality.SMOOTH.value -> {
                                mVideoQuality = VideoQuality.SMOOTH
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SMOOTH)
                            }

                            VideoQuality.SSD.value -> {
                                mVideoQuality = VideoQuality.SSD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SSD)
                            }

                            else -> {
                                mVideoQuality = VideoQuality.SD
                                mOnMonitorVideoQualityCallback?.onMonitorVideoQuality(VideoQuality.SD)
                            }
                        }
                    }
                }
//                mHandler.sendEmptyMessageDelayed(OPT_CHANGE_VIDEO_QUALITY,5000)
//                changeQualityStopDecoderVideo(true)
//                mCamera?.startShow(context, mAvChannel)
            }
        }
    }


    //拍照
    fun takePhoto() = mLastFrame

    //拍照并保存到文件
    fun takePhoto(path: String, name: String): File? {
        val bitmap = mLastFrame ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //读写权限
            if (PermissionUtil.permissionIsGranted(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                val file = File(path, name)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                    fos.flush()
                }
                val contentResolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?.let { uri ->
                        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                            data = uri
                        })
                    }
                return file
            } else {
                throw SecurityException("requires Manifest.permission.READ_EXTERNAL_STORAGE and Manifest.permission.WRITE_EXTERNAL_STORAGE")
            }
        } else {
            val file = File(path, name)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            }
            val contentResolver = context.contentResolver
            val value = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.TITLE, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/")
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
                ?.let { uri ->
                    FileInputStream(file).use { fis ->
                        BufferedInputStream(fis).use { bis ->
                            contentResolver.openOutputStream(uri)?.use { os ->
                                BufferedOutputStream(os).use { bos ->

//                                    val buffer = ByteArray(1024)
                                    var length: Int
                                    while (bis.read().also { length = it } != -1) {
                                        bos.write(length)
//                                        bos.write(buffer, 0, length)
                                    }
                                }
                            }
                        }
                    }
                }
            return file
        }
    }


    //使用ContentResolver保存
    fun takePhotoUri(path: String, name: String, title: String?): Uri? {
        val bitmap = mLastFrame ?: return null

        val contentResolver = context.contentResolver
        val value = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.TITLE, title ?: name)
                put(MediaStore.Images.Media.RELATIVE_PATH, path)
            } else {
                put(MediaStore.Images.Media.DATA, "$path/$name")
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { os ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                }
                return it
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    enum class PTZ {
        left, top, right, bottom
    }

    fun interface OnPtzListener {
        fun onPtz(ptz: PTZ)
    }

    fun interface OnMonitorVideoQualityCallback {
        fun onMonitorVideoQuality(quality: VideoQuality)
    }

    override fun onListenerStatus(status: Boolean) {
        onAudioListener?.onListenerStatus(status)
    }

    override fun onTalkStatus(status: Boolean) {
        onAudioListener?.onTalkStatus(status)
    }

    override fun onAudioRecordVolume(volume: Double) {
        onAudioListener?.onAudioRecordVolume(volume)
    }

    //注册外放监听
    fun registerEarphoneChangeListener() {
        if (!this::audioManager.isInitialized) {
            val service = context.getSystemService(Context.AUDIO_SERVICE)
            if (service is AudioManager) {
                audioManager = service
            }
        }
        if (isRegisterEarphonesReceiver) return
        isRegisterEarphonesReceiver = true
        if (earPhoneReceiver == null) {
            earPhoneReceiver = EarphonesReceiver(audioManager, mVoiceType)
        }
        earPhoneReceiver?.voiceType = mVoiceType
        context?.registerReceiver(earPhoneReceiver, earPhoneReceiver?.getIntentFilter())
    }

    fun unregisterEarPhoneChangeListener() {
        kotlin.runCatching {
            if (isRegisterEarphonesReceiver) {
                isRegisterEarphonesReceiver = false
                earPhoneReceiver?.let { receiver ->
                    context?.unregisterReceiver(receiver)
                }
            }
        }
    }

}

/**
 * 监听开关
 * @param MUTE  静音
 * @param UNMUTE 开启
 */
enum class AudioListener {
    MUTE, UNMUTE
}

enum class AudioTalker {
    TALK, UNTALK
}


open class MonitorThread(
    var surfaceHolder: SurfaceHolder?,
    var isRunning: Boolean = true,
    var canDraw: Boolean = true
) : Thread() {

    fun stopThread() {
        isRunning = false
        canDraw = false
    }

    fun isThreadRunning() = isRunning && canDraw

}