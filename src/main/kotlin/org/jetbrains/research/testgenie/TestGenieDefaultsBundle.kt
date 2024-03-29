package org.jetbrains.research.testgenie

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

const val DEFAULTS_BUNDLE = "defaults.TestGenie"

/**
 * Loads the default values from `defaults/TestGenie.properties` file in the `resources` directory.
 */
object TestGenieDefaultsBundle : DynamicBundle(DEFAULTS_BUNDLE) {

    /**
     * Gets the requested default value.
     */
    @Nls
    fun defaultValue(@PropertyKey(resourceBundle = DEFAULTS_BUNDLE) key: String): String = getMessage(key)
}
