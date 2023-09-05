//package com.tutk.IOTC
//
//import android.util.Log
//import androidx.test.platform.app.InstrumentationRegistry
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//import org.junit.Test
//import org.junit.runner.RunWith
//
//import org.junit.Assert.*
//
///**
// * Instrumented test, which will execute on an Android device.
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
//@RunWith(AndroidJUnit4::class)
//class ExampleInstrumentedTest {
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("com.tutk.IOTC", appContext.packageName)
//    }
//
//    @Test
//    fun gloabText(){
//        val sJob = GlobalScope.launch (Dispatchers.IO){
//            while (true){
//                Log.d("ExampleInstrumentedTest", "gloabText: 1111111")
//                print("gloabText: 1111111")
//                delay(100L)
//            }
//        }
//
//        GlobalScope.launch(Dispatchers.IO) {
//            delay(800L)
//            sJob.cancel()
//            print("gloabText: cancel")
//        }
//    }
//
//}