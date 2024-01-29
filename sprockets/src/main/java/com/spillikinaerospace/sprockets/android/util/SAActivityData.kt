// Copyright © 2022 - 2024 Spillikin Aerospace LLC. All rights reserved.
//                    U.S. Patent Pending.
package com.spillikinaerospace.sprockets.android.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity

/**
 * SAActivityData
 *
 * SAActivityData is a convenient way to pass parameters into and
 * out of an Activity.
 *
 * ActivityData sub-types provide specific data for an activity.
 * While SAActivityData itself provides type checking and a
 * convenient API for both simple parameter passing, and
 * activity result handling.
 *
 * Various ActivityUtil functions take SAActivityData and add data to
 * an intent before creating an Activity and going to it.
 *
 * About strict type checking:
 *
 * toIntent() and toResult() save the sub-type of SAActivityData being passed.
 * fromIntent() and fromResult() will check to see if the type matches
 * and return false if it doesn't.
 * The subclasses are also allowed to check activity specfic data
 * when reading data back from the intent.
 */
abstract class SAActivityData {

    private val CLASS_NAME = "saActivityDataClassName"
    private var activityDataClassName: String? = null

    /**
     * Given an intent, pass in all data for the activity
     * we intent to go to. This is called BEFORE we go to
     * the activity.
     * Like this...
     *
     *     val data = FileActivityData()
     *     data.filename = "somefile.txt"
     *     val intent = Intent(context, FileActivity::class.java)
     *     data.toIntent(intent)
     *     context.startActivity(intent)
     *
     * @param intent - intent for new activity.
     */
    fun toIntent(intent: Intent) {
        val bundle = Bundle()
        toBundle(bundle)

        // Save intent specific data in
        // override putToBundle(bundle)
        // bundle.putString(DIRECTORY_KEY, directory)
        // bundle.putString(FINENAME_KEY, filename)
        putToBundle(bundle)

        intent.putExtras(bundle)
    }

    /**
     * toResult passes data FROM an activity TO another activity.
     * Call this BEFORE calling finish() to send data back to
     * the caller.
     *
     * Given an AppCompatActivity, encode the intent for this
     * ActivityData and setResult(Ok, Intent)
     *
     * In SACameraActivity we do this...
     *     override fun finish() {
     *         val data = SACameraActivityData()
     *         data.directory = directory
     *         data.filename = fileName
     *         data.toResult(this)
     *         super.finish()
     *     }
     *
     * @param activity
     */
    fun toResult (activity: AppCompatActivity) {
        toIntent(activity.intent)
        // setResult is NEEDED in order to send anything back.
        activity.setResult(AppCompatActivity.RESULT_OK, activity.intent)
    }

    /**
     * Add the classname.
     * This was we can tell what type of data has been saved.
     *
     * @param bundle
     */
    private fun toBundle(bundle: Bundle) {
        this.activityDataClassName = this.javaClass.canonicalName
        bundle.putString(CLASS_NAME, this.activityDataClassName)
    }

    /**
     * Subclass override this to insert data into the bundle of
     * the intent
     *
     *     override fun putToBundle (bundle: Bundle) {
     *         bundle.putString(DIRECTORY_KEY, directory)
     *         bundle.putString(FINENAME_KEY, filename)
     *     }
     *
     * @param bundle
     */
    abstract fun putToBundle (bundle: Bundle)

    /**
     * Given an intent, attempt to extract expected data.
     * If the SAActivityData sub-class does not match the data
     * in the intent, or there was no bundle, return false.
     *
     * If the data matches, we call the sub-classes getFromBundle()
     * and set the data in the sub-class. The subclass may also
     * return false as it can do its own data validation.
     *
     * Call this in onCreate() or whenever you need the data.
     * Like this...
     *
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *          val data = GetFileActivityData()
     *          if (data.fromIntent(intent) == false ) {
     *              handle error
     *              ...
     *          }
     *          val filename = data.filename
     *          ...
     *     }
     *
     *
     * @param intent - the given intent.
     * @return True if the data matched.
     */
    fun fromIntent (intent: Intent): Boolean {
        val bundle = intent.extras
        if (bundle !is Bundle) {
            return false
        }
        if ( fromBundle(bundle) == false ) {
            return false
        }

        // Get intent specific data in subclass...
        // override getFromBundle(bundle)
        //     directory = bundle.getString(DIRECTORY_KEY, null)
        //     filename = bundle.getString(FINENAME_KEY, null)
        //     check for validity
        //     return isValid
        return getFromBundle(bundle)
    }

    /**
     * fromResult receives data FROM the activity that just finished
     * when it returns to a previous activity.
     * Call this as part of an activities registerForActivityResult() handler.
     *
     * Given an ActivityResult, decode the result for this
     * ActivityData and return True if the expected data was found.
     * We do strict type checking and save the name of the subclass when
     * toResult() or toIntent() is called.
     *
     * In some activity that is waiting for a result from SACameraActivity
     *
     * // This will be called as soon as the SACameraActivity is dismissed.
     *     this.cameraResult = registerForActivityResult(
     *         ActivityResultContracts.StartActivityForResult()) { result ->
     *
     *         // Get our SACameraActivity's data.
     *         val data = SACameraActivityData()
     *         if (data.fromResult((result))) {
     *             val filename = data.filename
     *             val directory = data.directory
     *
     * @param result
     * @return True if data is valid.
     */
    fun fromResult (result: ActivityResult): Boolean {
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent is Intent) {
                return fromIntent(intent)
            }
        }
        return false
    }

    /**
     * Get the classname from the intent. Here we can
     * see if the classname matches the class reading the inte.t
     *
     * @param intent
     * @return True if the bundle data is of the same type as
     *      this ActivityData
     */
    private fun fromBundle (bundle: Bundle): Boolean {
        val className = bundle.getString(CLASS_NAME, null)
        this.activityDataClassName = this.javaClass.canonicalName
        return className == this.activityDataClassName
    }

    /**
     * Subclasses override this to extract Activity specific data from
     * the bundle of the intent.
     * We give the subclass an opportunity to do some data validation
     * of its own.
     *
     *     override fun getFromBundle(bundle: Bundle): Boolean {
     *         directory = bundle.getString(DIRECTORY_KEY, null)
     *         filename = bundle.getString(FINENAME_KEY, null)
     *         //  check your data, set boolean.
     *         return dataIsValid
     *     }
     * @param bundle
     * @return Boolean - true if data found in the bundle is valid.
     */
    abstract fun getFromBundle(bundle: Bundle): Boolean

    /**
     * Use this for debugging.
     *
     * @return the classname contained in the intent.
     */
    fun getClassName(): String? {
        return this.activityDataClassName
    }
}
