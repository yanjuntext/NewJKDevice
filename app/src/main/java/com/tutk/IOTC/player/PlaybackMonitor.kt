package com.tutk.IOTC.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import com.tutk.IOTC.AudioListener
import com.tutk.IOTC.Camera
import com.tutk.IOTC.Liotc
import com.tutk.IOTC.listener.*
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.PlaybackStatus
import com.tutk.IOTC.status.RecordStatus
import com.tutk.IOTC.status.VoiceType
import com.tutk.bean.TEvent
import com.tutk.io.getAudioCodec
import com.tutk.io.parsePlayBack
import com.tutk.io.playback
import com.tutk.io.playbackSeekToPercent
import com.tutk.utils.PermissionUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.*
import kotlin.math.sqrt

/**
 * @Author: wangyj
 * @CreateDate: 2021/12/8
 * @Description:回放播放器
 */
class PlaybackMonitor @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet?,
    defStyle: Int = 0
) : SurfaceView(context, attr, defStyle), LifecycleObserver, SurfaceHolder.Callback, OnIOCallback,
    OnSessionChannelCallback, OnFrameCallback {

    private val TAG = PlaybackMonitor::class.java.simpleName

    private var mPlayMode: PlayMode = PlayMode.PLAY_BACK
    private var mVoiceType: VoiceType = VoiceType.ONE_WAY_VOICE

    private val DEFAULT_MAX_ZOOM_SCALE = 3.0F


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


    private var mOnClickListener: OnClickListener? = null
    private var mDownTime: Long = 0L

    private var isFullScreen = false

    private var mAVChannelRecordStatus: IAVChannelRecordStatus? = null


    var isRecording = false

    private var mRecordEvent: TEvent? = null

    private var mPlaybackStatus: PlaybackStatus? = null

    private var mOnPlaybackCallback: OnPlayBackCallback? = null

    //总时长
    private var mVideoTotalTime = 0

    //已播放时长
    private var mVideoPlayTime = 0

    //音频监听开关
    private var mAudioTrackStatus: Boolean = true

    private var mPlayTimeJob: Job? = null

    /**是否使用libyuv解析图片*/
    var withYuv = false

    init {
        mSurHolder = holder
        mSurHolder?.addCallback(this)
        isLongClickable = true
//        mGestureDetector = GestureDetector(this)
        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    Liotc.d("Monitor", "onFling 1")
                    if (mRectCanvas.left != vLeft || mRectCanvas.top != vTop || mRectCanvas.right != vRight || mRectCanvas.bottom != vBottom)
                        return false
                    Liotc.d("Monitor", "onFling 2")
                    return false
                }

                //双击
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    Liotc.d("Monitor", "onDoubleTap")
                    if (mRectCanvas.left > 0 || mRectCanvas.right < nScreenWidth || mRectCanvas.top > 0 || mRectCanvas.bottom < nScreenHeight) {
                        _setFullScreen()
                    } else {
                        scaleToOrigin()
                    }
                    return true
                }
            })
    }

    /**设置充满全屏*/
    private fun _setFullScreen() {
        Liotc.d("Monitor", "_setFullScreen 1")
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            Liotc.d("Monitor", "_setFullScreen 2")
            isFullScreen = false
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight >= nScreenHeight) {
                val ratio = cHeight * 1.0f / nScreenHeight
                val space = (cHeight - nScreenHeight) / 2
                mCurrentScale = ratio
                mRectCanvas.set(0, -space, nScreenWidth, cHeight - space)
            } else {
                val cWidth = nScreenHeight * 16 / 9
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

    /**缩放至原始大小*/
    private fun scaleToOrigin() {
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight >= nScreenHeight) {
                val cWidth = nScreenHeight * 16 / 9
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

    /**
     * 必须调用此方法 回复才能正常运行
     */
    fun startPlayback(camera: Camera?, event: TEvent?, playBackCallback: OnPlayBackCallback?) {
        mRecordEvent = event
        mOnPlaybackCallback = playBackCallback
        mCamera = camera
        mCamera?.registerSessionChannelCallback(this)
        mCamera?.registerIOCallback(this)
        mCamera?.registerFrameCallback(this)
        registerAVChannelRecordStatus(mAVChannelRecordStatus)
        renderJob()
//        if (mAvChannel < 0) {
//            mRecordEvent?.let {
//                if (mPlaybackStatus != PlaybackStatus.STOP && mPlaybackStatus != PlaybackStatus.START) {
//                    mCamera.playback(type = PlaybackStatus.START, time = it)
//                }
//            }
//        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PLAYING) {
            mRecordEvent?.let {
                mCamera.playback(type = PlaybackStatus.PAUSE, time = it)
            }
        }
    }

    /**
     * 暂停播放后回复播放
     */
    fun resume() {
        if (mAvChannel < 0) {
            mRecordEvent?.let {
                if (mPlaybackStatus != PlaybackStatus.STOP && mPlaybackStatus != PlaybackStatus.START) {
                    mCamera.playback(type = PlaybackStatus.START, time = it)
                }
            }
            return
        }
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PAUSE) {
            mRecordEvent?.let {
                mCamera.playback(type = PlaybackStatus.PAUSE, time = it)
            }
        }
    }

    fun seekTo(percent: Int) {
        if (mAvChannel >= 0) {
            mRecordEvent?.let {
                mCamera.playbackSeekToPercent(time = it, percent = percent)
            }
        }
    }

    /**
     * 停止播放
     * 释放播放资源
     */
    fun stop() {
        if (mAvChannel >= 0 && mPlaybackStatus != PlaybackStatus.STOP) {
            releaseAudio()
            mRecordEvent?.let {
                mCamera.playback(type = PlaybackStatus.STOP, time = it)
            }
            if (isRecording) {
                stopRecord()
            }
//            setAudioTrackStatus(false)
            stopShow()
            mCamera?.stop(mAvChannel)
        }
    }

    private fun startPlayTime() {
        mPlayTimeJob?.cancel()
        mPlayTimeJob = null

        mPlayTimeJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                while (mPlayTimeJob?.isActive == true) {
                    emit(1)
                    delay(1000)
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    if (mPlayTimeJob?.isActive == true) {
                        mVideoPlayTime++
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                    }
                }
        }
    }

    private fun stopPlayTime() {
        mPlayTimeJob?.cancel()
        mPlayTimeJob = null
    }

    /**解绑Camera*/
    fun unAttachCamera() {
        mAvChannel = -1

        mCamera?.unregisterSessionChannelCallback(this)
        mCamera?.unregisterFrameCallback(this)
        mCamera?.unregisterIOCallback(this)
        mCamera = null

        isRunning = false
        mRenderJob?.cancel()
        mRenderJob = null
    }

    /**
     * 开始播放回放
     */
    private fun startShow() {
        renderJob()
        mCamera?.setPlayMode(mAvChannel, mPlayMode)
        mCamera?.setVoiceType(mAvChannel, mVoiceType)
        mCamera?.start(mAvChannel, mCamera?.viewAccount ?: "admin", mCamera?.psw ?: "")
        mCamera.getAudioCodec(mAvChannel)
        mCamera?.startShow(context, mAvChannel,withYuv = withYuv)
        setAudioTrackStatus(mAudioTrackStatus)

    }


    private fun stopShow() {
        mCamera?.stopShow(mAvChannel)
    }

    fun setAudioTrackStatus(status: Boolean) {
        mAudioTrackStatus = status
        Liotc.d("RecvAudioJob","motion setAudioListener=$status")
        setAudioListener(if (mAudioTrackStatus) AudioListener.UNMUTE else AudioListener.MUTE)
    }

    /**监听*/
    private fun setAudioListener(audioListener: AudioListener) {
        if (mAvChannel >= 0) {
            Liotc.d("RecvAudioJob","motion setAudioListener=$audioListener")
            mCamera?.setAudioTrackStatus(context, mAvChannel, audioListener == AudioListener.UNMUTE)
        }
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
        mCamera?.startRecord(context, mAvChannel, file, mBitmapWidth, mBitmapHeight) { result ->
            isRecording = result == RecordStatus.RECORDING
            callback?.onResult(result)
        }
    }


    fun stopRecord() {
        Liotc.d("stopRecord", "stopRecord")
        mCamera?.stopRecord()
        isRecording = false
    }


    private fun renderJob() {
        if (isRunning && mRenderJob?.isActive == true) {
            Liotc.d("Monitor", "renderJob is Running return [$isRunning],[${mRenderJob?.isActive}]")
            return
        }
        Liotc.d("Monitor", "renderJob running")
        isRunning = true
        mRenderJob = GlobalScope.launch(Dispatchers.IO) {

            var videoCanvas: Canvas? = null
            mPaint.isDither = true

            while (isRunning && mRenderJob?.isActive == true) {
                Liotc.d(
                    "Monitor",
                    "renderJob -----[${mLastZoomTime != null}],[${mLastFrame?.isRecycled == false}]"
                )
                if (mLastFrame != null && mLastFrame?.isRecycled == false) {
                    try {
                        videoCanvas = mSurHolder?.lockCanvas()
                        videoCanvas?.let { canvas ->
                            canvas.drawColor(Color.BLACK)
                            mLastFrame?.let { bitmap ->
                                Liotc.d("Monitor", "drawBitmap")
                                canvas.drawBitmap(bitmap, null, mRectCanvas, mPaint)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        videoCanvas?.let {
                            mSurHolder?.unlockCanvasAndPost(it)
                        }
                        videoCanvas = null
                    }
                }
                delay(33L)
            }
            Liotc.d("Monitor", "renderJob end")
            isRunning = false
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Liotc.d("Monitor", "onStart")

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PLAYING) {
            pause()
        }
        if (isRecording) {
            stopRecord()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Liotc.d("Monitor", "onDestroy")

        stop()
        //释放音频资源
        releaseAudio()
        unAttachCamera()
        unRegisterAVChannelRecordStatus()
        stopPlayTime()
        mOnPlaybackCallback = null
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
                    if (_event.action == MotionEvent.ACTION_UP && System.currentTimeMillis() - mDownTime < 100) {
                        mOnClickListener?.onClick(this)
                    }

                    if (nScreenWidth != 0 && nScreenHeight != 0) {
                        val cHeight = nScreenWidth * 9 / 16
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
        mGestureDetector?.onTouchEvent(event)
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
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Liotc.d(
            "Monitor",
            "surfaceChanged [screen[$nScreenWidth,$nScreenWidth]],[surface[$width,$height]],measured[$measuredWidth,$measuredHeight]"
        )
        synchronized(this) {
            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            mRectMonitor.set(0, 0, width, height)
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight > nScreenHeight) {
                //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                val cWidth = nScreenHeight * 16 / 9
                val space = (nScreenWidth - cWidth) / 2
                mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
            } else {
                val space = (nScreenHeight - cHeight) / 2
                mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
            }

//            if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//                mRectCanvas.set(0, 0, width, height)
//            } else {
//                mRectCanvas.set(0, 0, nScreenWidth, nScreenHeight)
//            }

//            if (mCurVideoWidth == 0 || mCurVideoHeight == 0) {
//                if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//                    mRectCanvas.right = 4 * height / 3
//                    mRectCanvas.offset((width - mRectCanvas.right) / 2, 0)
//                } else {
//                    mRectCanvas.bottom = 3 * width / 4
//                    mRectCanvas.offset(0, (height - mRectCanvas.bottom) / 2)
//                }
//            } else {
//                if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//
//                    mRectCanvas.right = mRectMonitor.right
//                    mRectCanvas.offset(0, 0)
//                } else {
//                    val ratio = mCurVideoWidth.toDouble() / mCurVideoHeight
//                    mRectCanvas.bottom = (mRectCanvas.right / ratio).toInt()
//                    mRectCanvas.offset(0, (mRectMonitor.bottom - mRectCanvas.bottom) / 2)
//
//                    //使图片居中
//                    val rmRight = mRectMonitor.right
//                    val rcRight = mRectCanvas.right
//
//                    val offsetX = (rmRight - rcRight) / 2
//                    mRectCanvas.set(offsetX, 0, mRectCanvas.width() + offsetX, mRectCanvas.bottom)
//                }
//            }

            vLeft = mRectCanvas.left
            vTop = mRectCanvas.top
            vRight = mRectCanvas.right
            vBottom = mRectCanvas.bottom

            mCurrentScale = 1f

            Liotc.d("Monitor", "_setFullScreen surfaceChanged[$isFullScreen]")


//            parseMidPoint(
//                mMidPoint,
//                vLeft.toFloat(),
//                vTop.toFloat(),
//                vRight.toFloat(),
//                vBottom.toFloat()
//            )
//            parseMidPoint(
//                mMidPointForCanvas,
//                vLeft.toFloat(),
//                vTop.toFloat(),
//                vRight.toFloat(),
//                vBottom.toFloat()
//            )
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
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
            mLastFrame = bmp

            if (mRenderJob == null || mRenderJob?.isActive != true || !isRunning) {
                Liotc.d("Monitor", "restart render job")
                renderJob()
            }

            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            mBitmapWidth = bmp?.width ?: 0
            mBitmapHeight = bmp?.height ?: 0
            if ((bmp?.width ?: 0) > 0 && (bmp?.height ?: 0) > 0 &&
                (nScreenHeight != mCurVideoHeight || nScreenWidth != mCurVideoWidth)
            ) {
                Liotc.d(
                    "Monitor",
                    "screen[${nScreenWidth},${nScreenHeight}],video[${mCurVideoWidth},${mCurVideoHeight}]"
                )


                mCurVideoWidth = nScreenWidth
                mCurVideoHeight = nScreenHeight

                val cHeight = nScreenWidth * 9 / 16
                if (cHeight > nScreenHeight) {
                    //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                    val cWidth = nScreenHeight * 16 / 9
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

            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP -> {
                val playback = data.parsePlayBack()
                Liotc.d("RecvAudioJob","--------IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP type=${playback?.type}")
                when (playback?.type) {
                    PlaybackStatus.START -> {
                        mVideoTotalTime = playback.time / 1000
                        mVideoPlayTime = 0
                        mPlaybackStatus = PlaybackStatus.START
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )

                        if (playback.channel in 0..31) {
                            mAvChannel = playback.channel
                            startShow()
                            mPlaybackStatus = PlaybackStatus.PLAYING
                            startPlayTime()
                        } else {
                            mPlaybackStatus = PlaybackStatus.ERROR
                            mOnPlaybackCallback?.onPlayBackStatus(mPlaybackStatus, 0, 0)
                            stopPlayTime()
                        }
                    }
                    PlaybackStatus.PAUSE -> {
                        if (mPlaybackStatus == PlaybackStatus.PAUSE) {
                            mPlaybackStatus = PlaybackStatus.PLAYING
                            startPlayTime()
                            renderJob()
                        } else {
                            isRunning = false
                            mPlaybackStatus = PlaybackStatus.PAUSE
                            stopPlayTime()
                            if (isRecording) {
                                stopRecord()
                            }
                        }
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                    }
                    PlaybackStatus.STOP -> {
                        isRunning = false
                        stopPlayTime()
                        stop()
                        mPlaybackStatus = PlaybackStatus.STOP
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                    }
                    PlaybackStatus.END -> {
                        mPlaybackStatus = PlaybackStatus.END
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                        stopPlayTime()
                        stop()
                    }
                }
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
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                fos.flush()
            }
            val contentResolver = context.contentResolver
            val value = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.TITLE, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/")
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
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, os)
                }
                return it
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun interface OnPlayBackCallback {
        /**
         *  @param status
         *  @param totalTime 视频总时长 秒
         *  @param currentTime 当前播放的时长 秒
         */
        fun onPlayBackStatus(status: PlaybackStatus?, totalTime: Int, currentTime: Int)
    }
}
