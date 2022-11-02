/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin.textdetector

import android.content.ComponentCallbacks
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage

import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import vikas.mrzscanner.vision.GraphicOverlay
import vikas.mrzscanner.vision.PreferenceUtils
import vikas.mrzscanner.vision.VisionProcessorBase

/** Processor for the text detector demo. */
class TextRecognitionProcessor(
    private val context: Context,
    textRecognizerOptions: TextRecognizerOptionsInterface, callbacks: onMrzDetected
) : VisionProcessorBase<Text>(context) {


    private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)

    private val TAG = "TextRecognition"

    var frameState: MutableSharedFlow<Text>
    var framesNeedsRejection = 1;

    init {
        frameState = MutableSharedFlow<Text>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        CoroutineScope(Dispatchers.IO).launch {
            //no need to process all frames
            frameState.conflate()
                .collect { value -> // cancel & restart on the latest value

                    Log.d(TAG, "Processing")
                    val text = searchForMrz(value)
                    //mrz detected so cancel corutine
                    if (!text.isEmpty()) {
                        callbacks.onMrzNumberDetected(text)
                        this.cancel()
                    }

                    // delay(1500)
                }
        }

    }

    override fun stop() {
        super.stop()
        textRecognizer.close()
    }

    override fun detectInImage(image: InputImage): Task<Text> {
        return textRecognizer.process(image)
    }


    override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {

        val bl = frameState.tryEmit(text)
        Log.d(TAG, bl.toString())

    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Text detection failed.$e")
    }


    /**
     * https://www.idenfy.com/blog/machine-readable-zone/
     */

    private fun searchForMrz(text: Text): String {
        val listT: MutableList<Text.TextBlock> = text.textBlocks!!.toMutableList()
        //sort the list
        listT.sortedWith(CompareRect)
        if (listT.size > 2) {
            val lastLine: Text.TextBlock = listT.removeLast()
            val lastButOne: Text.TextBlock = listT.removeLast()

            if (isStringMrz(lastLine.text)) {
                //use only lastLine
                if (isLastBlockContainsTwoLines(lastLine.text)) {
                    if (isValidFrame(lastLine.text, 80) && isFirstCharacterIsValid(lastLine.text))
                        Log.d(TAG, lastLine.text)
                    return if (framesNeedsRejection < 0)
                        lastLine.text
                    else {
                        framesNeedsRejection--
                        ""
                    }

                }
                // use lastButOne as well
                else {
                    if (isValidFrame(
                            lastLine.text.plus(lastButOne.text),
                            40
                        ) && isFirstCharacterIsValid(lastButOne.text)
                    )
                        Log.d(TAG, lastLine.text.plus("Wit two lines \n").plus(lastButOne.text))
                    return if (framesNeedsRejection < 0)
                        lastButOne.text.plus(lastLine.text)
                    else {
                        framesNeedsRejection--
                        ""
                    }
                }
            }

        }

        return ""
    }

    private fun isValidFrame(lastLineText: String, count: Int): Boolean {
        if (lastLineText.length >= count) {
            Log.d(TAG, "Valid text count")
            return true
        } else {
            Log.d(TAG, "Not Valid text count")
            return false
        }
    }


    /**
     * MRZ should have "<" string in them
     */

    private fun isStringMrz(lastLineText: String): Boolean {
        return lastLineText.contains("<<", true)
    }


    /**
     * Some time last block may contain two lines
     * and total size of mrz Type 3 is 88 , 44 in each line
     */
    private fun isLastBlockContainsTwoLines(lastLineText: String): Boolean {

        if( lastLineText.length > 80){
            Log.d(TAG,"lastLineText length greater than 80")
                return true
        }else{
            Log.d(TAG,"lastLineText length smaller than 80")
            return false
        }
    }

    /**
     * It should start with "p" to indicate its a passport
     */
    private fun isFirstCharacterIsValid(lastLineText: String): Boolean {
        if( lastLineText.first().equals("P".toCharArray()[0], false)){
            Log.d(TAG,"First latter is valid");
            return true
        }else{
            Log.d(TAG,"First latter is Not Valid");
            return false
        }
    }


    /**
     * Sort the text block according to their yaxis in descending order
     */
    class CompareRect {

        companion object : Comparator<Text.TextBlock> {

            override fun compare(a: Text.TextBlock, b: Text.TextBlock): Int {
                return if (a.boundingBox!!.top < b.boundingBox!!.top) {
                    -1;
                } else if (a.boundingBox!!.top > b.boundingBox!!.top) {
                    1;
                } else {
                    0;
                }
            }
        }
    }


    interface onMrzDetected {
        fun onMrzNumberDetected(detectedNumber: String)
    }
}
