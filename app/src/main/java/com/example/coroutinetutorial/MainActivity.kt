package com.example.coroutinetutorial

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        * coroutines return a job class, i.e. launch() returns job, that we can save in a variable.
        * we can wait for a job to be finished by using a suspend function join()
        * https://youtu.be/55W60o9uzVc
        * */

        val job = GlobalScope.launch(Dispatchers.Default) {
            repeat(5) {
                Log.d(TAG, "Coroutine working in runBlocking, main thread blocked...")
                delay(1000L)
            }
        }

        runBlocking {
            // runBlocking is a coroutine that blocks the main thread.
            // why would we want that?
            // we can put suspend functions inside it, for example delay(3000L)
            // and it would behave the same as if it was put in the main thread (we can't put suspend
            // functions in main thread, for that use Thread.sleep() )
            // runBlocking is also used for JUnit testing

            job.join() // join() waits for our job to finish, so main thread will be blocked until then
            Log.d(TAG, "Main thread is continuing...")
        }

        /************************************/

        // canceling a job
        // https://youtu.be/55W60o9uzVc

        val job2 = GlobalScope.launch(Dispatchers.Default) {
            repeat(5) {
                Log.d(TAG, "Coroutine working in runBlocking, main thread blocked...")
                delay(1000L)
            }
        }

        runBlocking {
            delay(2000L)
            job2.cancel()
            Log.d(TAG, "Job cancelled.")
            Log.d(TAG, "Main thread is continuing...")
        }

        /*
        * cancelling is cooperative, meaning that a coroutine has to be set up correctly to be cancelled,
        * we have delays in our code that allow it to easily be cancelled, but in real life a coroutine
        * can continue to work after the cancel function, so we need to add manual if check
        * (see next example below)
        * */

        /***************************************************************************************/

        // cancelling automatically due to timeout using withTimeout()
        // (in practice we usually cancel coroutines due to timeout)

        GlobalScope.launch {
            withTimeout(3000L) {
                delay(2500L)
                //withTimeout is useful function to cancel job if it takes too long (e.g. network call)
                if (isActive) { // isActive is useful to additionally check if job is active
                    Log.d(TAG, "Not cancelled due to time out (request took less than 3s)")
                }
            }
        }

        /***************************************************************************************/

        findViewById<Button>(R.id.btnStartActivity).setOnClickListener {
            lifecycleScope.launch {
                while (true) {
                    delay(1000L)
                    Log.d(TAG, "Still running...")
                }
            }
            // global scope means that the coroutine will live as long as the application (if it isn't finished)
            // launch Dispatcher is not needed here, added to show that launch can have dispatchers
            GlobalScope.launch(Dispatchers.Default) {
                delay(5000L)
                Intent(this@MainActivity, SecondActivity::class.java).also {
                    startActivity(it)
                    finish()
                }
            }
        }

        /***************************************************************************************/

        //how to make 2 network calls in coroutines at the same time (asynchronously)

        //note that all examples are inside of this GlobalScope.launch{} block
        GlobalScope.launch(Dispatchers.IO) {
            // example 1 - synchronous (one after another)
            val time = measureTimeMillis {
                val answer1 = networkCall1() // each call has 3000ms delay
                Log.d(TAG, "Answer1 is $answer1")
                val answer2 = networkCall2()
                Log.d(TAG, "Answer2 is $answer2")
            }
            Log.d(TAG, "Sync Requests took $time ms.") // in this case time will be ~6000ms

            /*****************************************/
            // example 2 - asynchronously, but bad practice
            val time2 = measureTimeMillis {
                var answer1: String? = null
                var answer2: String? = null
                val job3 = launch { answer1 = networkCall1() }
                val job4 = launch { answer2 = networkCall2() }
                job3.join()
                Log.d(TAG, "Answer1 is $answer1")
                job4.join()
                Log.d(TAG, "Answer2 is $answer2")
            }
            Log.d(TAG, "Asynchronous bad practice Requests took $time2 ms.") // in this case time will be ~3000ms

            /*****************************************/
            // example 3 - asynchronously using Async (good practice)
            // Async returns a Deferred
            val time3 = measureTimeMillis {
                val answer1 = async { networkCall1() }
                val answer2 = async { networkCall2() }

                // val answer is of type Deferred<String> so we call .await() on it
                Log.d(TAG, "Answer1 is ${answer1.await()}")
                Log.d(TAG, "Answer2 is ${answer2.await()}")

            }
            Log.d(TAG, "Async good practice Requests took $time3 ms.") // in this case time will be ~3000ms
        } // GlobalScope.launch{} block with 3 examples closed
    }

    private suspend fun networkCall1(): String {
        delay(3000L)
        return "Network call result 1"
    }

    private suspend fun networkCall2(): String {
        delay(3000L)
        return "Network call result 2"
    }
}