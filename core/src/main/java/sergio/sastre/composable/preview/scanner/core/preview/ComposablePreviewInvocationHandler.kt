package sergio.sastre.composable.preview.scanner.core.preview

import sergio.sastre.composable.preview.scanner.core.preview.exception.PreviewParameterIsNotFirstArgumentException
import io.github.classgraph.AnnotationClassRef
import io.github.classgraph.AnnotationInfoList
import sergio.sastre.composable.preview.scanner.core.scanresult.filter.PREVIEW_WRAPPER_ANNOTATION
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.pow
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

/**
 * Used to handle calls to a [composableMethod].
 * If a [parameter] is provided, it will be used as the first parameter of the call.
 */
internal class ComposablePreviewInvocationHandler(
    private val composableMethod: Method,
    private val parameter: Any?,
    private val annotationsInfo: AnnotationInfoList?,
) : InvocationHandler {

    /**
     * Used to indicate that no parameter should be used when calling the [composableMethod].
     * We can't use null here as we might want to pass null as an actual parameter to a function.
     */
    object NoParameter

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        if (method?.name != "invoke") return method?.invoke(this, *(args ?: emptyArray()))

        val safeArgs: Array<out Any?> = fillMissingComposeArgs(args)
        val wrapperData = getPreviewWrapperProvider()

        if (wrapperData != null && args != null && args.size >= 2) {
            return invokeWithWrapper(wrapperData, args, safeArgs)
        }

        return invokeDirectly(
            composer = args?.getOrNull(args.size - 2),
            changed = args?.getOrNull(args.size - 1) as? Int ?: 0,
            safeArgs = safeArgs
        )
    }

    private fun invokeWithWrapper(
        wrapperData: Pair<Any, Method?>,
        args: Array<out Any>,
        safeArgs: Array<out Any?>
    ): Any? {
        val (provider, wrapMethod) = wrapperData
        if (wrapMethod != null) {
            val composer = args[args.size - 2]
            val changed = args[args.size - 1]

            // This lambda mimics a @Composable () -> Unit
            val content = object : (Any?, Int) -> Unit {
                override fun invoke(p1: Any?, p2: Int) {
                    // Call the actual composable method with the wrapper's composer
                    invokeDirectly(p1, p2, safeArgs)
                }
            }
            return wrapMethod.invoke(provider, content, composer, changed)
        }
        return null
    }

    private fun invokeDirectly(composer: Any?, changed: Int, safeArgs: Array<out Any?>): Any? {
        val allParams = composableMethod.kotlinFunction!!.parameters
        val hasDefaultParams = allParams.any { it.isOptional }

        // Update the composer and changed values in the arguments
        val updatedArgs = arrayOf(*safeArgs)
        if (updatedArgs.size >= (if (hasDefaultParams) 3 else 2)) {
            val offset = if (hasDefaultParams) 1 else 0
            updatedArgs[updatedArgs.size - 2 - offset] = composer
            updatedArgs[updatedArgs.size - 1 - offset] = changed
        }

        val safeArgsWithParam =
            when (parameter != NoParameter) {
                true -> arrayOf(parameter, *updatedArgs)
                false -> updatedArgs
            }

        val isInsideClass = !Modifier.isStatic(composableMethod.modifiers)
        val kotlinComposableMethod = composableMethod.kotlinFunction!!.apply { isAccessible = true }
        return when (isInsideClass) {
            false -> kotlinComposableMethod.call(*safeArgsWithParam)
            true -> kotlinComposableMethod.call(
                composableMethod.declaringClass.getDeclaredConstructor().newInstance(),
                *safeArgsWithParam
            )
        }
    }

    private fun getPreviewWrapperProvider(): Pair<Any, Method?>? {
        val annotationParams = annotationsInfo
            ?.filter { it.name == PREVIEW_WRAPPER_ANNOTATION }
            ?.firstOrNull()?.parameterValues
            ?: return null

        val wrapperClassRef = annotationParams.getValue("wrapper") as? AnnotationClassRef ?: return null
        return PreviewWrapperCache.getProviderAndWrapMethod(wrapperClassRef.name)
    }

    private fun fillMissingComposeArgs(passedComposeArgs: Array<out Any>?): Array<out Any?> {
        val safeArgs = passedComposeArgs ?: emptyArray()
        val allParams = composableMethod.kotlinFunction!!.parameters
        val defaultParams = allParams.filter { it.isOptional }
        when (defaultParams.isEmpty()) {
            true -> return safeArgs
            false -> {
                // Very rare case:
                // if @PreviewParameters & default parameters available
                // And @PreviewParameters is not the first of all arguments
                // we cannot handle it:
                // we don't know the value of that argument to pass it at a certain index
                // and it'd throw an UndeclaredThrowableException
                //
                // Update: It seems that from AS Meerkat on, this is enforced :)
                if (allParams.any { !it.isOptional && it.index != 0 }){
                    throw PreviewParameterIsNotFirstArgumentException(
                        className =  composableMethod.declaringClass.name,
                        methodName = composableMethod.name
                    )
                }

                // In kotlin reflect, null params resolve to default kotlin params.
                // These params are added at the beginning of the method by the Compose Compiler
                val defaultParamsAsNull: Array<out Any?> = arrayOfNulls(defaultParams.size)

                // When default params available, the Compose Compiler adds a mask at the end of the method
                // to map the default parameters to their corresponding default values.
                //
                // This mask contains 1 bit for each parameter (0 -> null, 1 -> default value),
                // including default params and those passed via @PreviewParameters
                // so in order to resolve all parameters to their corresponding values,
                // you need the highest possible number in binary e.g.
                // 1 param  -> 1
                // 2 params -> 11 -> 3
                // 3 params -> 111 -> 7
                // 4 params -> 1111 -> 15
                // x params -> 2 pow (x) - 1
                val paramsMask: MutableList<Int> = mutableListOf(2.0.pow(allParams.size).toInt() - 1)
                return (defaultParamsAsNull.toMutableList() + safeArgs.toMutableList() + paramsMask).toTypedArray()
            }
        }
    }
}